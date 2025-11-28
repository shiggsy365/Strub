package com.example.stremiompvplayer.ui.home

import android.content.Intent
import android.os.Bundle
import android.view.ContextThemeWrapper
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.stremiompvplayer.R
import com.example.stremiompvplayer.adapters.PosterAdapter
import com.example.stremiompvplayer.data.ServiceLocator
import com.example.stremiompvplayer.databinding.FragmentHomeBinding
import com.example.stremiompvplayer.models.MetaItem
import com.example.stremiompvplayer.models.UserCatalog
import com.example.stremiompvplayer.utils.SharedPreferencesManager
import com.example.stremiompvplayer.utils.FocusMemoryManager
import com.example.stremiompvplayer.viewmodels.MainViewModel
import com.example.stremiompvplayer.viewmodels.MainViewModelFactory
import com.example.stremiompvplayer.ui.ResultsDisplayModule
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MainViewModel by activityViewModels {
        MainViewModelFactory(
            ServiceLocator.getInstance(requireContext()),
            SharedPreferencesManager.getInstance(requireContext())
        )
    }

    private lateinit var contentAdapter: PosterAdapter
    private lateinit var displayModule: ResultsDisplayModule
    private var detailsUpdateJob: Job? = null

    // Track current catalogs and index for cycling
    private var currentCatalogs = listOf<UserCatalog>()
    private var currentCatalogIndex = 0
    private var isCycling = false
    private var cycleAttemptCount = 0

    private val focusMemoryManager = FocusMemoryManager.getInstance()
    private val fragmentKey = "home"

    companion object {
        fun newInstance(): HomeFragment {
            return HomeFragment()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupDisplayModule()
        setupUI()
        setupAdapters()
        setupObservers()
        loadHomeCatalogs()
    }

    private fun setupDisplayModule() {
        val config = ResultsDisplayModule.Configuration(
            pageBackground = binding.pageBackground,
            detailTitle = binding.detailTitle,
            detailLogo = binding.detailLogo,
            detailDescription = binding.detailDescription,
            detailDate = binding.detailDate,
            detailRating = binding.detailRating,
            detailEpisode = binding.detailEpisode,
            actorChips = binding.root.findViewById(R.id.actorChips),
            btnPlay = binding.root.findViewById(R.id.btnPlay),
            btnTrailer = binding.root.findViewById(R.id.btnTrailer),
            btnRelated = binding.root.findViewById(R.id.btnRelated),
            enableDrillDown = true,  // Enable drill-down for series navigation
            useGridLayout = false,
            showEpisodeDescription = false
        )
        displayModule = ResultsDisplayModule(this, viewModel, config)

        // Setup callbacks
        displayModule.onActorClicked = { personId, actorName ->
            viewModel.loadPersonCredits(personId)
            updateCurrentListLabel("$actorName - Filmography")
        }

        displayModule.onRelatedContentLoaded = { similarContent ->
            contentAdapter.updateData(similarContent)
            displayModule.updateDetailsPane(similarContent[0])
            updateCurrentListLabel("Related to ${displayModule.currentSelectedItem?.name}")
        }
    }

    override fun onResume() {
        super.onResume()
        displayModule.currentSelectedItem?.let { displayModule.updateDetailsPane(it) }
        if (currentCatalogs.isNotEmpty() && currentCatalogIndex < currentCatalogs.size) {
            viewModel.loadContentForCatalog(currentCatalogs[currentCatalogIndex], isInitialLoad = true)
        }
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
        val currentView = view?.findFocus()
        if (currentView != null) {
            // Find the RecyclerView item view (walk up the view hierarchy)
            var itemView = currentView
            while (itemView != null && itemView.parent != binding.rvContent) {
                itemView = itemView.parent as? android.view.View
            }

            // Only try to get position if we found a valid RecyclerView child
            if (itemView != null && itemView.parent == binding.rvContent) {
                val position = binding.rvContent.getChildAdapterPosition(itemView)
                if (position != RecyclerView.NO_POSITION) {
                    focusMemoryManager.saveFocus(fragmentKey, currentView, position)
                }
            }
        }
    }

    fun handleKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (event?.action == KeyEvent.ACTION_DOWN) {
            when (keyCode) {
                KeyEvent.KEYCODE_DPAD_DOWN -> {
                    if (binding.rvContent.hasFocus() || binding.rvContent.focusedChild != null) {
                        cycleToNextList()
                        return true
                    }
                }
                KeyEvent.KEYCODE_DPAD_UP -> {
                    if (binding.rvContent.hasFocus() || binding.rvContent.focusedChild != null) {
                        // Focus on Play button if available, otherwise Related button
                        val btnPlay = binding.root.findViewById<View>(R.id.btnPlay)
                        val btnRelated = binding.root.findViewById<View>(R.id.btnRelated)
                        when {
                            btnPlay?.visibility == View.VISIBLE -> btnPlay.requestFocus()
                            btnRelated?.visibility == View.VISIBLE -> btnRelated.requestFocus()
                            else -> btnPlay?.requestFocus() // Fallback to play even if hidden
                        }
                        return true
                    }
                }
            }
        }
        return false
    }

    fun handleBackPress(): Boolean {
        val handled = displayModule.handleBackPress()
        if (handled) {
            // If going back to catalog, reload the current catalog
            if (displayModule.currentDrillDownLevel == ResultsDisplayModule.DrillDownLevel.CATALOG) {
                if (currentCatalogs.isNotEmpty() && currentCatalogIndex < currentCatalogs.size) {
                    val currentCatalog = currentCatalogs[currentCatalogIndex]
                    updateCurrentListLabel(currentCatalog.displayName)
                    viewModel.loadContentForCatalog(currentCatalog, isInitialLoad = false)
                }
            }
        }
        return handled
    }

    private fun setupUI() {
        // No dropdown setup needed
    }

    private fun setupAdapters() {
        contentAdapter = PosterAdapter(
            items = emptyList(),
            onClick = { item -> displayModule.onContentClicked(item) },  // Use standard navigation
            onLongClick = { item ->
                val pos = contentAdapter.getItemPosition(item)
                val holder = binding.rvContent.findViewHolderForAdapterPosition(pos)
                if (holder != null) showItemMenu(holder.itemView, item)
            }
        )

        binding.rvContent.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        binding.rvContent.adapter = contentAdapter

        binding.rvContent.addOnChildAttachStateChangeListener(object : RecyclerView.OnChildAttachStateChangeListener {
            override fun onChildViewAttachedToWindow(view: View) {
                view.setOnFocusChangeListener { v, hasFocus ->
                    if (hasFocus) {
                        val position = binding.rvContent.getChildAdapterPosition(v)
                        if (position != RecyclerView.NO_POSITION) {
                            focusMemoryManager.saveFocus(fragmentKey, v, position)
                            val item = contentAdapter.getItem(position)
                            if (item != null) {
                                detailsUpdateJob?.cancel()
                                detailsUpdateJob = viewLifecycleOwner.lifecycleScope.launch {
                                    delay(300)
                                    if (isAdded) displayModule.updateDetailsPane(item)
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

    private fun showItemMenu(view: View, item: MetaItem) {
        val wrapper = ContextThemeWrapper(requireContext(), android.R.style.Theme_DeviceDefault_Light_NoActionBar)
        val popup = PopupMenu(wrapper, view)

        viewLifecycleOwner.lifecycleScope.launch {
            val isInLibrary = viewModel.isItemInLibrarySync(item.id)

            if (isInLibrary) {
                popup.menu.add("Remove from Library")
            } else {
                popup.menu.add("Add to Library")
            }

            popup.menu.add("Mark as Watched")
            popup.menu.add("Clear Progress")
            popup.menu.add("Not Watching")
            popup.menu.add("Add to Trakt Watchlist")
            popup.menu.add("Remove from Trakt Watchlist")
            popup.menu.add("Rate on Trakt")

            // Add "Browse Show" for episodes
            if (item.type == "episode") {
                popup.menu.add("Browse Show")
            }

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
                        true
                    }
                    "Clear Progress" -> {
                        viewModel.clearWatchedStatus(item)
                        true
                    }
                    "Not Watching" -> {
                        viewModel.markAsNotWatching(item)
                        true
                    }
                    "Add to Trakt Watchlist" -> {
                        viewModel.addToWatchlist(item)
                        true
                    }
                    "Remove from Trakt Watchlist" -> {
                        viewModel.removeFromWatchlist(item)
                        true
                    }
                    "Rate on Trakt" -> {
                        showRatingDialog(item)
                        true
                    }
                    "Browse Show" -> {
                        browseShow(item)
                        true
                    }
                    else -> false
                }
            }
            popup.show()
        }
    }

    private fun showRatingDialog(item: MetaItem) {
        val ratings = arrayOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "10")
        com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
            .setTitle("Rate ${item.name} on Trakt")
            .setItems(ratings) { _, which ->
                val rating = which + 1
                viewModel.rateOnTrakt(item, rating)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun browseShow(item: MetaItem) {
        // Extract series ID from episode ID (format: "tmdb:12345:1:1" -> "tmdb:12345")
        val parts = item.id.split(":")
        if (parts.size >= 2) {
            val seriesId = "${parts[0]}:${parts[1]}"
            // Navigate to discover fragment with series ID
            val discoverFragment = com.example.stremiompvplayer.ui.discover.DiscoverFragment.newInstance("series", seriesId)
            (activity as? com.example.stremiompvplayer.MainActivity)?.loadFragment(discoverFragment)
        }
    }

    private fun loadHomeCatalogs() {
        currentCatalogs = viewModel.getHomeCatalogs()
        currentCatalogIndex = 0
        if (currentCatalogs.isNotEmpty()) {
            updateCurrentListLabel(currentCatalogs[0].displayName)
            viewModel.loadContentForCatalog(currentCatalogs[0], isInitialLoad = true)
        }
    }

    fun updateCurrentListLabel(labelText: String) {
        val label = binding.root.findViewById<android.widget.TextView>(R.id.currentListLabel)
        label?.text = labelText
    }

    private fun cycleToNextList() {
        if (currentCatalogs.isEmpty()) return

        // Prevent infinite loops - if we've tried all catalogs, stop
        if (cycleAttemptCount >= currentCatalogs.size) {
            cycleAttemptCount = 0
            isCycling = false
            return
        }

        isCycling = true
        cycleAttemptCount++

        // Reset drill-down state when cycling lists
        displayModule.currentDrillDownLevel = ResultsDisplayModule.DrillDownLevel.CATALOG
        displayModule.currentSeriesId = null
        displayModule.currentSeasonNumber = null

        currentCatalogIndex = (currentCatalogIndex + 1) % currentCatalogs.size
        val nextCatalog = currentCatalogs[currentCatalogIndex]
        updateCurrentListLabel(nextCatalog.displayName)
        viewModel.loadContentForCatalog(nextCatalog, isInitialLoad = true)
        displayModule.updatePlayButtonVisibility()

        // Focus will be handled in the observer after content loads
        binding.root.postDelayed({
            if (contentAdapter.itemCount > 0) {
                binding.rvContent.scrollToPosition(0)
                binding.rvContent.post {
                    val firstView = binding.rvContent.layoutManager?.findViewByPosition(0)
                    firstView?.requestFocus()
                    isCycling = false
                    cycleAttemptCount = 0
                }
            }
            // If empty, the observer will trigger another cycle
        }, 1000)
    }

    private fun setupObservers() {
        viewModel.currentCatalogContent.observe(viewLifecycleOwner) { items ->
            contentAdapter.updateData(items)
            if (items.isNotEmpty()) {
                displayModule.updateDetailsPane(items[0])
                // Reset cycle attempt count when we find a non-empty list
                if (isCycling) {
                    cycleAttemptCount = 0
                }

                // Auto-focus first item when content loads
                binding.root.postDelayed({
                    if (binding.rvContent.childCount > 0) {
                        val firstView = binding.rvContent.layoutManager?.findViewByPosition(0)
                        firstView?.requestFocus()
                    }
                }, 100)
            } else {
                displayModule.currentSelectedItem = null
                // If we're cycling and the list is empty, automatically cycle to the next list
                if (isCycling && cycleAttemptCount < currentCatalogs.size) {
                    binding.root.postDelayed({
                        cycleToNextList()
                    }, 500)
                }
            }
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading -> binding.loadingCard.visibility = if (isLoading) View.VISIBLE else View.GONE }

        // Observe search results for actor/person content
        viewModel.searchResults.observe(viewLifecycleOwner) { results ->
            if (results.isNotEmpty()) {
                contentAdapter.updateData(results)
                displayModule.updateDetailsPane(results[0])

                // Focus first item
                binding.root.postDelayed({
                    binding.rvContent.scrollToPosition(0)
                    binding.rvContent.layoutManager?.findViewByPosition(0)?.requestFocus()
                }, 100)
            }
        }

        // Observe series meta details for drill-down navigation
        viewModel.metaDetails.observe(viewLifecycleOwner) { meta ->
            if (meta != null && meta.type == "series" && displayModule.currentDrillDownLevel == ResultsDisplayModule.DrillDownLevel.SERIES) {
                // Display seasons in the carousel
                val seasons = meta.videos?.mapNotNull { video ->
                    video.season?.let { seasonNum ->
                        MetaItem(
                            id = "${meta.id}:$seasonNum",
                            type = "season",
                            name = video.title,
                            poster = video.thumbnail,
                            background = meta.background,
                            description = "Season $seasonNum",
                            releaseDate = video.released,
                            rating = video.rating
                        )
                    }
                } ?: emptyList()

                contentAdapter.updateData(seasons)
                if (seasons.isNotEmpty()) {
                    displayModule.updateDetailsPane(seasons[0])

                    // Auto-focus first season
                    binding.root.postDelayed({
                        if (binding.rvContent.childCount > 0) {
                            val firstView = binding.rvContent.layoutManager?.findViewByPosition(0)
                            firstView?.requestFocus()
                        }
                    }, 100)
                }
                updateCurrentListLabel("${meta.name} - Seasons")
            }
        }

        // Observe season episodes for drill-down navigation
        viewModel.seasonEpisodes.observe(viewLifecycleOwner) { episodes ->
            if (displayModule.currentDrillDownLevel == ResultsDisplayModule.DrillDownLevel.SEASON && episodes.isNotEmpty()) {
                contentAdapter.updateData(episodes)
                displayModule.updateDetailsPane(episodes[0])

                // Auto-focus first episode
                binding.root.postDelayed({
                    if (binding.rvContent.childCount > 0) {
                        val firstView = binding.rvContent.layoutManager?.findViewByPosition(0)
                        firstView?.requestFocus()
                    }
                }, 100)

                displayModule.currentSeriesId?.let { seriesId ->
                    displayModule.currentSeasonNumber?.let { seasonNum ->
                        updateCurrentListLabel("Season $seasonNum - Episodes")
                    }
                }
            }
        }
    }

    fun focusSidebar(): Boolean {
        binding.root.post {
            binding.root.findViewById<View>(R.id.btnPlay)?.requestFocus()
                ?: binding.rvContent.requestFocus()
        }
        return true
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        detailsUpdateJob?.cancel()
    }
}