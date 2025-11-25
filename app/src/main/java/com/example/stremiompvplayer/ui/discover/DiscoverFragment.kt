package com.example.stremiompvplayer.ui.discover

import android.content.Intent
import android.os.Bundle
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.stremiompvplayer.DetailsActivity2
import com.example.stremiompvplayer.R
import com.example.stremiompvplayer.data.ServiceLocator
import com.example.stremiompvplayer.databinding.FragmentDiscoverBinding
import com.example.stremiompvplayer.models.MetaItem
import com.example.stremiompvplayer.models.UserCatalog
import com.example.stremiompvplayer.utils.SharedPreferencesManager
import com.example.stremiompvplayer.utils.FocusMemoryManager
import com.example.stremiompvplayer.viewmodels.MainViewModel
import com.example.stremiompvplayer.viewmodels.MainViewModelFactory
import com.example.stremiompvplayer.adapters.PosterAdapter
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

class DiscoverFragment : Fragment() {

    private var _binding: FragmentDiscoverBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MainViewModel by activityViewModels {
        MainViewModelFactory(
            ServiceLocator.getInstance(requireContext()),
            SharedPreferencesManager.getInstance(requireContext())
        )
    }

    private lateinit var sidebarAdapter: SidebarAdapter
    private lateinit var contentAdapter: PosterAdapter
    private var currentType = "movie"
    private var currentSelectedItem: MetaItem? = null

    private var detailsUpdateJob: Job? = null
    private val focusMemoryManager = FocusMemoryManager.getInstance()
    private val fragmentKey: String
        get() = focusMemoryManager.getFragmentKey("discover", currentType)

    companion object {
        private const val ARG_TYPE = "media_type"
        fun newInstance(type: String): DiscoverFragment {
            val fragment = DiscoverFragment()
            val args = Bundle()
            args.putString(ARG_TYPE, type)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentDiscoverBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        currentType = arguments?.getString(ARG_TYPE) ?: "movie"
        setupAdapters()
        setupObservers()
        loadCatalogs()
    }

    override fun onResume() {
        super.onResume()
        currentSelectedItem?.let { updateDetailsPane(it) }

        // Restore previously focused position for seamless navigation
        val savedPosition = focusMemoryManager.getSavedPosition(fragmentKey)
        if (savedPosition >= 0 && savedPosition < contentAdapter.itemCount) {
            binding.root.postDelayed({
                binding.rvContent.scrollToPosition(savedPosition)
                binding.rvContent.postDelayed({
                    binding.rvContent.layoutManager?.findViewByPosition(savedPosition)?.requestFocus()
                }, 100)
            }, 150)
        }
    }

    override fun onPause() {
        super.onPause()
        // Save current focus before leaving fragment
        val currentView = view?.findFocus()
        if (currentView != null) {
            val position = binding.rvContent.getChildAdapterPosition(currentView)
            if (position != RecyclerView.NO_POSITION) {
                focusMemoryManager.saveFocus(fragmentKey, currentView, position)
            }
        }
    }

    fun handleBackPress(): Boolean { return false }
    fun focusSidebar(): Boolean {
        binding.root.post {
            val firstView = binding.rvSidebar.layoutManager?.findViewByPosition(0)
            if (firstView != null && firstView.isFocusable) {
                firstView.requestFocus()
            } else if (binding.rvSidebar.isFocusable) {
                binding.rvSidebar.requestFocus()
            } else if (binding.genreSelector.isFocusable) {
                binding.genreSelector.requestFocus()
            } else {
                // Try again after a delay if views aren't ready
                binding.root.postDelayed({
                    binding.rvSidebar.layoutManager?.findViewByPosition(0)?.requestFocus()
                        ?: binding.rvContent.requestFocus()
                }, 100)
            }
        }
        return true  // Always return true as we've initiated focus attempt
    }

    private fun setupAdapters() {
        // Setup genre selector
        binding.genreSelector.setOnClickListener {
            showGenreSelector()
        }

        sidebarAdapter = SidebarAdapter { catalog ->
            viewModel.loadContentForCatalog(catalog, isInitialLoad = true)
        }
        binding.rvSidebar.layoutManager = LinearLayoutManager(context)
        binding.rvSidebar.adapter = sidebarAdapter

        contentAdapter = PosterAdapter(
            items = emptyList(),
            onClick = { item -> onContentClicked(item) },
            onLongClick = { item ->
                val pos = contentAdapter.getItemPosition(item)
                val holder = binding.rvContent.findViewHolderForAdapterPosition(pos)
                if (holder != null) showItemMenu(holder.itemView, item)
            }
        )

        binding.rvContent.layoutManager = GridLayoutManager(context, 10)
        binding.rvContent.adapter = contentAdapter

        binding.rvContent.addOnChildAttachStateChangeListener(object : RecyclerView.OnChildAttachStateChangeListener {
            override fun onChildViewAttachedToWindow(view: View) {
                view.setOnFocusChangeListener { v, hasFocus ->
                    if (hasFocus) {
                        val position = binding.rvContent.getChildAdapterPosition(v)
                        if (position != RecyclerView.NO_POSITION) {
                            // Save focus position for seamless navigation
                            focusMemoryManager.saveFocus(fragmentKey, v, position)

                            val item = contentAdapter.getItem(position)
                            if (item != null) {
                                detailsUpdateJob?.cancel()
                                detailsUpdateJob = viewLifecycleOwner.lifecycleScope.launch {
                                    delay(300) // PERFORMANCE: Reduced from 1000ms for snappier UX
                                    if (isAdded) {
                                        updateDetailsPane(item)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            override fun onChildViewDetachedFromWindow(view: View) {
                view.setOnFocusChangeListener(null)
            }
        })
    }

    private fun updateDetailsPane(item: MetaItem) {
        currentSelectedItem = item

        Glide.with(this)
            .load(item.background ?: item.poster)
            .into(binding.pageBackground)

        val formattedDate = try {
            item.releaseDate?.let { dateStr ->
                val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val outputFormat = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())
                val date = inputFormat.parse(dateStr)
                date?.let { outputFormat.format(it) }
            }
        } catch (e: Exception) {
            item.releaseDate
        }

        binding.detailDate.text = formattedDate ?: ""
        binding.detailRating.visibility = if (item.rating != null) {
            binding.detailRating.text = "★ ${item.rating}"
            View.VISIBLE
        } else {
            View.GONE
        }

        binding.detailDescription.text = item.description ?: "No description available."

        binding.detailTitle.text = item.name
        // [CHANGE] Initial state is hidden for both to prevent flash
        binding.detailTitle.visibility = View.GONE
        binding.detailLogo.visibility = View.GONE

        viewModel.fetchItemLogo(item)

        refreshWatchStatus(item)
    }

    private fun refreshWatchStatus(item: MetaItem) {
        val userId = SharedPreferencesManager.getInstance(requireContext()).getCurrentUserId()
        if (userId != null) {
            viewModel.checkWatchedStatus(item.id)
        }
    }

    private fun loadCatalogs() {
        // Fetch genres for the current type
        viewModel.fetchGenres(currentType)

        viewModel.getDiscoverCatalogs(currentType).observe(viewLifecycleOwner) { catalogs ->
            sidebarAdapter.submitList(catalogs)
            if (catalogs.isNotEmpty()) {
                viewModel.loadContentForCatalog(catalogs[0], isInitialLoad = true)
            }
        }
    }

    private fun showGenreSelector() {
        val genres = if (currentType == "movie") {
            viewModel.movieGenres.value
        } else {
            viewModel.tvGenres.value
        }

        if (genres == null || genres.isEmpty()) {
            Toast.makeText(requireContext(), "Loading genres...", Toast.LENGTH_SHORT).show()
            return
        }

        val popupMenu = PopupMenu(requireContext(), binding.genreSelector)

        // Add "All" option at the top
        popupMenu.menu.add(0, -1, 0, "All")

        // Add all genres
        genres.forEachIndexed { index, genre ->
            popupMenu.menu.add(0, genre.id, index + 1, genre.name)
        }

        popupMenu.setOnMenuItemClickListener { item ->
            if (item.itemId == -1) {
                // "All" selected - clear genre filter
                viewModel.clearGenreSelection()
            } else {
                // Specific genre selected
                val selectedGenre = genres.find { it.id == item.itemId }
                selectedGenre?.let {
                    viewModel.selectGenre(it)
                }
            }
            true
        }

        popupMenu.show()
    }

    private fun showItemMenu(view: View, item: MetaItem) {
        val wrapper = ContextThemeWrapper(requireContext(),
            android.R.style.Theme_DeviceDefault_Light_NoActionBar)
        val popup = PopupMenu(wrapper, view)

        // [FIX] Use lifecycleScope to check library status properly before showing menu
        viewLifecycleOwner.lifecycleScope.launch {
            val isInLibrary = viewModel.isItemInLibrarySync(item.id)

            if (isInLibrary) {
                popup.menu.add("Remove from Library")
            } else {
                popup.menu.add("Add to Library")
            }

            popup.menu.add("Mark as Watched")
            popup.menu.add("Clear Watched Status")

            popup.setOnMenuItemClickListener { menuItem ->
                when (menuItem.title) {
                    "Add to Library" -> {
                        viewModel.addToLibrary(item)
                        true
                    }
                    "Remove from Library" -> {
                        viewModel.removeFromLibrary(item.id)
                        true
                    }
                    "Mark as Watched" -> {
                        viewModel.markAsWatched(item)
                        item.isWatched = true
                        refreshItem(item)
                        true
                    }
                    "Clear Watched Status" -> {
                        viewModel.clearWatchedStatus(item)
                        item.isWatched = false
                        refreshItem(item)
                        true
                    }
                    else -> false
                }
            }
            popup.show()
        }
    }



    private fun refreshItem(item: MetaItem) {
        val position = contentAdapter.getItemPosition(item)
        if (position != -1) {
            contentAdapter.notifyItemChanged(position)
        }
    }

    private fun onContentClicked(item: MetaItem) {
        updateDetailsPane(item)
        val intent = Intent(requireContext(), DetailsActivity2::class.java).apply {
            putExtra("metaId", item.id)
            putExtra("title", item.name)
            putExtra("poster", item.poster)
            putExtra("background", item.background)
            putExtra("description", item.description)
            putExtra("type", item.type)
        }
        startActivity(intent)
    }

    private fun setupObservers() {
        // Observe genre selection changes
        viewModel.selectedGenre.observe(viewLifecycleOwner) { genre ->
            if (genre != null) {
                binding.genreSelector.text = "Genre: ${genre.name}"
            } else {
                binding.genreSelector.text = "Genre: All"
            }

            // Reload content when genre changes
            val currentCatalogs = if (currentType == "movie") {
                viewModel.movieCatalogs.value
            } else {
                viewModel.seriesCatalogs.value
            }
            currentCatalogs?.firstOrNull()?.let { catalog ->
                viewModel.loadContentForCatalog(catalog, isInitialLoad = true)
            }
        }

        viewModel.currentCatalogContent.observe(viewLifecycleOwner) { items ->
            contentAdapter.updateData(items)

            if (items.isNotEmpty()) {
                updateDetailsPane(items[0])
            }

            if (items.isEmpty()) {
                currentSelectedItem = null
            }
        }

        viewModel.actionResult.observe(viewLifecycleOwner) { result ->
            when (result) {
                is MainViewModel.ActionResult.Success -> {
                    Toast.makeText(requireContext(), result.message,
                        Toast.LENGTH_SHORT).show()
                }
                is MainViewModel.ActionResult.Error -> {
                    Toast.makeText(requireContext(), result.message,
                        Toast.LENGTH_LONG).show()
                }
            }
        }

        viewModel.error.observe(viewLifecycleOwner) { errorMessage ->
            if (errorMessage.isNotEmpty()) {
                Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_LONG).show()
            }
        }
        // [CHANGE] Updated observer logic
        viewModel.currentLogo.observe(viewLifecycleOwner) { logoUrl ->
            when (logoUrl) {
                "" -> { // Loading
                    binding.detailTitle.visibility = View.GONE
                    binding.detailLogo.visibility = View.GONE
                }
                null -> { // No Logo
                    binding.detailTitle.visibility = View.VISIBLE
                    binding.detailLogo.visibility = View.GONE
                }
                else -> { // Has Logo
                    binding.detailTitle.visibility = View.GONE
                    binding.detailLogo.visibility = View.VISIBLE
                    Glide.with(this)
                        .load(logoUrl)
                        .fitCenter()
                        .into(binding.detailLogo)
                }
            }
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        viewModel.isItemWatched.observe(viewLifecycleOwner) { isWatched ->
            currentSelectedItem?.let { item ->
                item.isWatched = isWatched
            }
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        detailsUpdateJob?.cancel()
    }

    inner class SidebarAdapter(
        private val onClick: (UserCatalog) -> Unit
    ) : androidx.recyclerview.widget.ListAdapter<UserCatalog, SidebarAdapter.ViewHolder>(
        com.example.stremiompvplayer.adapters.CatalogConfigAdapter.DiffCallback()
    ) {

        private var selectedPosition = 0

        inner class ViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
            val name: android.widget.TextView = view.findViewById(R.id.catalogName)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_discover_sidebar, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = getItem(position)
            holder.name.text = item.displayName
            holder.view.isSelected = position == selectedPosition
            holder.view.setOnClickListener {
                val oldPosition = selectedPosition
                selectedPosition = position
                notifyItemChanged(oldPosition)
                notifyItemChanged(selectedPosition)
                onClick(item)
            }
        }
    }
}