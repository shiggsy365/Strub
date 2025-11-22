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
import com.example.stremiompvplayer.adapters.NavItem
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

    // focusPositionAfterLoad is no longer needed with the post logic for buttons

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
            // Pass isInitialLoad=true to clear cache and start fresh
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

        // Set GridLayoutManager with 10 columns
        binding.rvContent.layoutManager = GridLayoutManager(context, 10)
        binding.rvContent.adapter = contentAdapter

        // Re-adding the focus listener to ensure the Details Pane updates on hover/focus
        binding.rvContent.addOnChildAttachStateChangeListener(object : RecyclerView.OnChildAttachStateChangeListener {
            override fun onChildViewAttachedToWindow(view: View) {
                view.setOnFocusChangeListener { v, hasFocus ->
                    if (hasFocus) {
                        val position = binding.rvContent.getChildAdapterPosition(v)
                        if (position != RecyclerView.NO_POSITION) {
                            val item = contentAdapter.getItem(position)
                            // Only update details for actual content items, not nav buttons
                            if (item != null && item.id != NavItem.NAV_PREV.id && item.id != NavItem.NAV_NEXT.id) {
                                updateDetailsPane(item)
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

        // Update background
        Glide.with(this)
            .load(item.background ?: item.poster)
            .into(binding.pageBackground)

        // Update metadata
        val formattedDate = try {
            item.releaseDate?.let { dateStr ->
                // Assuming item.releaseDate is in standard "yyyy-MM-dd" format
                val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                // Target format: "dd MMMM yyyy" (e.g., "22 November 2025")
                val outputFormat = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())
                val date = inputFormat.parse(dateStr)
                date?.let { outputFormat.format(it) }
            }
        } catch (e: Exception) {
            // Fallback to original string or empty if parsing fails
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
                // Initial load: Pass isInitialLoad=true
                viewModel.loadContentForCatalog(catalogs[0], isInitialLoad = true)
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
                    viewModel.toggleWatchlist(item, force = true) // Use force=true for explicit add
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
        when (item.id) {
            NavItem.NAV_NEXT.id -> {
                // Focus on the first item of the next page (index 0)
                viewModel.loadNextPage()
                binding.rvContent.post {
                    binding.rvContent.scrollToPosition(0)
                    binding.rvContent.layoutManager?.findViewByPosition(0)?.requestFocus()
                }
            }
            NavItem.NAV_PREV.id -> {
                // Focus on the last content item of the previous page (index 18)
                viewModel.loadPreviousPage()
                binding.rvContent.post {
                    // Scroll to item 18 (index 17 in the adapter data, but index 18 in the view including the button)
                    binding.rvContent.scrollToPosition(18)
                    binding.rvContent.layoutManager?.findViewByPosition(18)?.requestFocus()
                }
            }
            else -> {
                // Regular content item click
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
        }
    }

    private fun setupObservers() {
        var currentPage = 1
        var isLastPage = true

        viewModel.currentPage.observe(viewLifecycleOwner) { page ->
            currentPage = page
        }
        viewModel.isLastPage.observe(viewLifecycleOwner) { isLast ->
            isLastPage = isLast
        }

        viewModel.currentCatalogContent.observe(viewLifecycleOwner) { items ->
            // Use DiffUtil or notifyDataSetChanged as appropriate for PosterAdapter
            contentAdapter.updateData(items, currentPage, isLastPage)

            // Focus logic adjustment: Automatically focus on the first content item if it's an initial load
            if (items.isNotEmpty() && currentPage == 1 && items[0].id != NavItem.NAV_PREV.id) {
                updateDetailsPane(items[0])
            }

            // If the content is empty, clear details
            if (items.isEmpty()) {
                currentSelectedItem = null
                // Code to clear details UI would go here if needed
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