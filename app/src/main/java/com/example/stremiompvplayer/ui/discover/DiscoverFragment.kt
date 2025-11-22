package com.example.stremiompvplayer.ui.discover

import android.content.Intent
import android.os.Bundle
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
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
import com.example.stremiompvplayer.viewmodels.MainViewModel
import com.example.stremiompvplayer.viewmodels.MainViewModelFactory
import com.example.stremiompvplayer.adapters.PosterAdapter

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
        // Update details for currently selected item to refresh watch status
        currentSelectedItem?.let { updateDetailsPane(it) }
    }

    fun handleBackPress(): Boolean { return false }
    fun focusSidebar() { binding.rvSidebar.requestFocus() }

    private fun setupAdapters() {
        sidebarAdapter = SidebarAdapter { catalog ->
            viewModel.loadContentForCatalog(catalog)
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

        // Set GridLayoutManager with 10 columns
        binding.rvContent.layoutManager = GridLayoutManager(context, 10)
        binding.rvContent.adapter = contentAdapter

        // Add scroll listener to auto-update details on focus change
        binding.rvContent.addOnChildAttachStateChangeListener(object : RecyclerView.OnChildAttachStateChangeListener {
            override fun onChildViewAttachedToWindow(view: View) {
                view.setOnFocusChangeListener { v, hasFocus ->
                    if (hasFocus) {
                        val position = binding.rvContent.getChildAdapterPosition(v)
                        if (position != RecyclerView.NO_POSITION) {
                            val item = contentAdapter.getItem(position)
                            item?.let { updateDetailsPane(it) }
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

        // Update background
        Glide.with(this)
            .load(item.background ?: item.poster)
            .into(binding.pageBackground)

        // Update metadata
        binding.detailDate.text = item.releaseDate ?: ""
        binding.detailRating.visibility = if (item.rating != null) {
            binding.detailRating.text = "★ ${item.rating}"
            View.VISIBLE
        } else {
            View.GONE
        }

        binding.detailDescription.text = item.description ?: "No description available."

        // Update title/logo
        binding.detailTitle.text = item.name
        binding.detailTitle.visibility = View.VISIBLE
        binding.detailLogo.visibility = View.GONE

        // Fetch logo if possible
        viewModel.fetchItemLogo(item)

        // Update watch status indicators
        refreshWatchStatus(item)
    }

    private fun refreshWatchStatus(item: MetaItem) {
        // Force refresh from database
        val userId = SharedPreferencesManager.getInstance(requireContext()).getCurrentUserId()
        if (userId != null) {
            viewModel.checkWatchedStatus(item.id)
        }
    }

    private fun loadCatalogs() {
        viewModel.getDiscoverCatalogs(currentType).observe(viewLifecycleOwner) { catalogs ->
            sidebarAdapter.submitList(catalogs)
            if (catalogs.isNotEmpty()) {
                viewModel.loadContentForCatalog(catalogs[0])
            }
        }
    }

    private fun showItemMenu(view: View, item: MetaItem) {
        val wrapper = ContextThemeWrapper(requireContext(), android.R.style.Theme_DeviceDefault_Light_NoActionBar)
        val popup = PopupMenu(wrapper, view)

        popup.menu.add("Add to Library")
        popup.menu.add("Add to TMDB Watchlist")
        popup.menu.add("Mark as Watched")
        popup.menu.add("Clear Watched Status")

        popup.setOnMenuItemClickListener { menuItem ->
            when (menuItem.title) {
                "Add to Library" -> {
                    viewModel.addToLibrary(item)
                    true
                }
                "Add to TMDB Watchlist" -> {
                    viewModel.toggleWatchlist(item, true)
                    true
                }
                "Mark as Watched" -> {
                    viewModel.markAsWatched(item)
                    // Optimistic UI update
                    item.isWatched = true
                    item.progress = item.duration
                    val position = contentAdapter.getItemPosition(item)
                    if (position != -1) {
                        contentAdapter.notifyItemChanged(position)
                    }
                    true
                }
                "Clear Watched Status" -> {
                    viewModel.clearWatchedStatus(item)
                    // Optimistic UI update
                    item.isWatched = false
                    item.progress = 0
                    val position = contentAdapter.getItemPosition(item)
                    if (position != -1) {
                        contentAdapter.notifyItemChanged(position)
                    }
                    true
                }
                else -> false
            }
        }
        popup.show()
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
        viewModel.currentCatalogContent.observe(viewLifecycleOwner) { items ->
            contentAdapter.updateData(items)
            if (items.isNotEmpty()) {
                updateDetailsPane(items[0])
            }
        }

        viewModel.currentLogo.observe(viewLifecycleOwner) { logoUrl ->
            if (logoUrl != null) {
                binding.detailTitle.visibility = View.GONE
                binding.detailLogo.visibility = View.VISIBLE
                Glide.with(this)
                    .load(logoUrl)
                    .fitCenter()
                    .into(binding.detailLogo)
            } else {
                binding.detailTitle.visibility = View.VISIBLE
                binding.detailLogo.visibility = View.GONE
            }
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        // Watch the watched status to update UI
        viewModel.isItemWatched.observe(viewLifecycleOwner) { isWatched ->
            currentSelectedItem?.let { item ->
                item.isWatched = isWatched
                val position = contentAdapter.getItemPosition(item)
                if (position != -1) {
                    contentAdapter.notifyItemChanged(position)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
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

            // Visual feedback for selected catalog
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