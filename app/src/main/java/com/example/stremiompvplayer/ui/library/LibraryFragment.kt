package com.example.stremiompvplayer.ui.library

import android.content.Intent
import android.os.Bundle
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.stremiompvplayer.PlayerActivity
import com.example.stremiompvplayer.R
import com.example.stremiompvplayer.adapters.HomeRowAdapter
import com.example.stremiompvplayer.adapters.PosterAdapter
import com.example.stremiompvplayer.data.ServiceLocator
import com.example.stremiompvplayer.databinding.FragmentHomeBinding
import com.example.stremiompvplayer.models.MetaItem
import com.example.stremiompvplayer.ui.ResultsDisplayModule
import com.example.stremiompvplayer.utils.SharedPreferencesManager
import com.example.stremiompvplayer.viewmodels.HomeRow
import com.example.stremiompvplayer.viewmodels.LibraryFilterConfig
import com.example.stremiompvplayer.viewmodels.LibraryViewModel
import com.example.stremiompvplayer.viewmodels.LibraryViewModelFactory
import com.example.stremiompvplayer.viewmodels.MainViewModel
import com.example.stremiompvplayer.viewmodels.MainViewModelFactory
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Library Fragment that displays user's library content using the same layout as Home.
 * Uses HomeRowAdapter with vertical rows for Movies and Series.
 * Action buttons are removed - use context menu (long press) for actions.
 */
class LibraryFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: LibraryViewModel by viewModels {
        LibraryViewModelFactory(
            ServiceLocator.getInstance(requireContext()),
            SharedPreferencesManager.getInstance(requireContext())
        )
    }

    // Access MainViewModel for shared logic (player, library actions)
    private val mainViewModel: MainViewModel by activityViewModels {
        MainViewModelFactory(
            ServiceLocator.getInstance(requireContext()),
            SharedPreferencesManager.getInstance(requireContext())
        )
    }

    private lateinit var rowAdapter: HomeRowAdapter
    private var heroUpdateJob: Job? = null

    // Helper module for dialogs
    private lateinit var displayModule: ResultsDisplayModule

    // Drill-down navigation state
    enum class DrillDownLevel { ROWS, SERIES, SEASON }
    private var currentDrillDownLevel = DrillDownLevel.ROWS
    private var currentSeriesId: String? = null
    private var currentSeasonNumber: Int? = null
    private var currentSeriesName: String? = null

    // Adapter for drill-down content (seasons/episodes)
    private var drillDownAdapter: PosterAdapter? = null

    // Store current library rows for restoration
    private var cachedLibraryRows: List<HomeRow> = emptyList()

    // Track the focused item position for refresh restoration
    private var lastFocusedRowIndex: Int = 0
    private var lastFocusedItemIndex: Int = 0

    companion object {
        fun newInstance(): LibraryFragment {
            return LibraryFragment()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize display module for helper methods
        val config = ResultsDisplayModule.Configuration(
            pageBackground = binding.pageBackground,
            detailTitle = binding.detailTitle,
            detailLogo = binding.detailLogo,
            detailDescription = binding.detailDescription,
            detailDate = binding.detailDate,
            detailRating = binding.detailRating,
            detailEpisode = binding.detailEpisode,
            actorChips = binding.actorChips,
            btnPlay = null, btnTrailer = null, btnRelated = null
        )
        displayModule = ResultsDisplayModule(this, mainViewModel, config)

        setupRecyclerView()
        setupObservers()

        // Initial load
        viewModel.loadLibraryContent()
    }

    private fun setupRecyclerView() {
        rowAdapter = HomeRowAdapter(
            onContentClick = { item ->
                handleSinglePress(item)
            },
            onContentFocused = { item ->
                updateHeroBanner(item)
            },
            showRatings = true  // Show ratings on library posters
        )

        binding.rvHomeRows.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
            adapter = rowAdapter
            setItemViewCacheSize(10)
        }
    }

    private fun handleSinglePress(item: MetaItem) {
        when (item.type) {
            "episode", "movie" -> {
                // DIRECT PLAY: Launch PlayerActivity with MetaItem only.
                val intent = Intent(requireContext(), PlayerActivity::class.java).apply {
                    putExtra("meta", item)
                }
                startActivity(intent)
            }
            "series" -> {
                // Drill down into series IN-PLACE
                currentDrillDownLevel = DrillDownLevel.SERIES
                currentSeriesId = item.id
                currentSeriesName = item.name
                currentSeasonNumber = null
                mainViewModel.loadSeriesMeta(item.id)
                updateHeroBanner(item)
            }
            "season" -> {
                // Drill down into season to show episodes
                val parts = item.id.split(":")
                if (parts.size >= 2) {
                    val seriesId = parts.dropLast(1).joinToString(":")
                    val seasonNum = parts.last().toIntOrNull() ?: 1
                    currentDrillDownLevel = DrillDownLevel.SEASON
                    currentSeriesId = seriesId
                    currentSeasonNumber = seasonNum
                    mainViewModel.loadSeasonEpisodes(seriesId, seasonNum)
                    updateHeroBanner(item)
                }
            }
            else -> {
                // Fallback for other types
                displayModule.showStreamDialog(item)
            }
        }
    }

    private fun showLongPressMenu(item: MetaItem) {
        val view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_content_options, null)
        val dialog = android.app.AlertDialog.Builder(requireContext(), android.R.style.Theme_DeviceDefault_Dialog_NoActionBar)
            .setView(view)
            .create()
        
        // Make dialog background transparent to show the card background
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val titleView = view.findViewById<TextView>(R.id.menuTitle)
        titleView.text = item.name

        // --- Library-specific: Show Sort and Filter option at the top ---
        val actionSortFilter = view.findViewById<View>(R.id.actionSortFilter)
        val dividerSortFilter = view.findViewById<View>(R.id.dividerSortFilter)
        actionSortFilter.visibility = View.VISIBLE
        dividerSortFilter.visibility = View.VISIBLE
        actionSortFilter.setOnClickListener {
            dialog.dismiss()
            showFilterDialog()
        }

        // --- Actions ---

        // 1. Scrape Streams (Explicit)
        view.findViewById<View>(R.id.actionScrape).setOnClickListener {
            dialog.dismiss()
            displayModule.showStreamDialog(item)
        }

        // 2. Watch Trailer
        view.findViewById<View>(R.id.actionTrailer).setOnClickListener {
            dialog.dismiss()
            displayModule.playTrailer(item)
        }

        // 3. Watchlist Toggle
        val actionWatchlist = view.findViewById<TextView>(R.id.actionWatchlist)
        actionWatchlist.text = "Toggle Watchlist"
        actionWatchlist.setOnClickListener {
            dialog.dismiss()
            mainViewModel.toggleWatchlist(item)
            // Refresh library after action
            refreshLibraryAndRestoreFocus()
        }

        // 4. Library Toggle
        val actionLibrary = view.findViewById<TextView>(R.id.actionLibrary)
        lifecycleScope.launch {
            val inLib = mainViewModel.isItemInLibrarySync(item.id)
            actionLibrary.text = if (inLib) "Remove from Library" else "Add to Library"
        }
        actionLibrary.setOnClickListener {
            dialog.dismiss()
            mainViewModel.toggleLibrary(item)
            // Refresh library after action
            refreshLibraryAndRestoreFocus()
        }

        // 5. Watched Toggle
        val actionWatched = view.findViewById<TextView>(R.id.actionWatched)
        actionWatched.text = if (item.isWatched) "Mark Unwatched" else "Mark Watched"
        actionWatched.setOnClickListener {
            dialog.dismiss()
            if (item.isWatched) mainViewModel.clearWatchedStatus(item) else mainViewModel.markAsWatched(item)
            // Refresh library after action
            refreshLibraryAndRestoreFocus()
        }

        // 6. Not Watching - Complete removal from all lists
        view.findViewById<View>(R.id.actionNotWatching).setOnClickListener {
            dialog.dismiss()
            mainViewModel.markAsNotWatching(item)
            // Refresh library after action
            refreshLibraryAndRestoreFocus()
        }

        // --- Cast Section ---
        val castGroup = view.findViewById<ChipGroup>(R.id.castChipGroup)

        val castObserver = androidx.lifecycle.Observer<List<MetaItem>> { castList ->
            if (dialog.isShowing) {
                castGroup.removeAllViews()
                castList.take(10).forEach { actor ->
                    val chip = Chip(requireContext())
                    chip.text = actor.name
                    chip.setOnClickListener {
                        dialog.dismiss()
                        val personId = actor.id.removePrefix("tmdb:").toIntOrNull()
                        if (personId != null) {
                            mainViewModel.loadPersonCredits(personId)
                        }
                    }
                    castGroup.addView(chip)
                }
            }
        }

        mainViewModel.castList.observe(viewLifecycleOwner, castObserver)
        mainViewModel.fetchCast(item.id, item.type)

        dialog.setOnDismissListener {
            mainViewModel.castList.removeObserver(castObserver)
        }

        dialog.show()
        dialog.window?.apply {
            val width = (resources.displayMetrics.widthPixels * 0.8).toInt()
            setLayout(width, android.view.WindowManager.LayoutParams.WRAP_CONTENT)
            setGravity(android.view.Gravity.CENTER)
        }
    }

    /**
     * Refreshes the library content and restores focus to the previous item.
     */
    private fun refreshLibraryAndRestoreFocus() {
        // Store current focus position
        saveFocusPosition()
        
        // Refresh library
        viewModel.loadLibraryContent()
        
        // Focus will be restored in the observer after data loads
    }

    /**
     * Shows the library filter/sort dialog.
     */
    private fun showFilterDialog() {
        val currentConfig = viewModel.filterConfig.value ?: LibraryFilterConfig()
        val movieGenres = viewModel.movieGenres.value ?: emptyList()
        val tvGenres = viewModel.tvGenres.value ?: emptyList()
        
        LibraryFilterDialog(
            context = requireContext(),
            currentConfig = currentConfig,
            movieGenres = movieGenres,
            tvGenres = tvGenres,
            onApply = { newConfig ->
                viewModel.updateFilterConfig(newConfig)
            },
            onClear = {
                viewModel.clearFilters()
            }
        ).show()
    }

    /**
     * Saves the current focus position for restoration after refresh.
     */
    private fun saveFocusPosition() {
        // Try to find the currently focused position
        val focusedView = binding.rvHomeRows.findFocus()
        if (focusedView != null) {
            // Walk up to find the row viewholder
            var parent = focusedView.parent
            while (parent != null && parent != binding.rvHomeRows) {
                if (parent is View) {
                    val rowPosition = binding.rvHomeRows.getChildAdapterPosition(parent)
                    if (rowPosition != androidx.recyclerview.widget.RecyclerView.NO_POSITION) {
                        lastFocusedRowIndex = rowPosition
                        break
                    }
                }
                parent = parent.parent
            }
        }
    }

    private fun updateHeroBanner(item: MetaItem) {
        heroUpdateJob?.cancel()
        heroUpdateJob = viewLifecycleOwner.lifecycleScope.launch {
            delay(200)
            if (!isAdded) return@launch

            Glide.with(this@LibraryFragment)
                .load(item.background ?: item.poster)
                .into(binding.pageBackground)

            binding.detailTitle.text = item.name
            binding.detailTitle.visibility = View.VISIBLE
            binding.detailLogo.visibility = View.GONE

            mainViewModel.fetchItemLogo(item)

            binding.detailDescription.text = item.description ?: "No description available."

            binding.detailRating.visibility = if (item.rating != null) View.VISIBLE else View.GONE
            binding.detailRating.text = "★ ${item.rating}"

            val dateText = try {
                item.releaseDate?.take(4) ?: ""
            } catch (e: Exception) { "" }
            binding.detailDate.text = dateText

            // Episode Info
            if (item.type == "episode") {
                binding.detailEpisode.visibility = View.VISIBLE
                if (item.id.split(":").size >= 4) {
                    val parts = item.id.split(":")
                    val season = parts[2].toIntOrNull() ?: 0
                    val episode = parts[3].toIntOrNull() ?: 0
                    binding.detailEpisode.text = "S%02d - E%02d".format(season, episode)
                } else {
                    binding.detailEpisode.text = "Episode"
                }
            } else {
                binding.detailEpisode.visibility = View.GONE
            }

            // Fetch cast for hero banner
            binding.actorChips.removeAllViews()
            mainViewModel.fetchCast(item.id, item.type)
        }
    }

    private fun setupObservers() {
        viewModel.libraryRows.observe(viewLifecycleOwner) { rows ->
            cachedLibraryRows = rows
            // Only update rows view if we're at ROWS level
            if (currentDrillDownLevel == DrillDownLevel.ROWS) {
                rowAdapter.updateData(rows, this::showLongPressMenu)

                if (rows.isNotEmpty() && rows[0].items.isNotEmpty()) {
                    updateHeroBanner(rows[0].items[0])
                    
                    // Restore focus to previous position after refresh
                    binding.root.postDelayed({
                        if (lastFocusedRowIndex < rows.size) {
                            binding.rvHomeRows.scrollToPosition(lastFocusedRowIndex)
                            binding.rvHomeRows.requestFocus()
                        }
                    }, 100)
                }
            }
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        // Observe Logo updates for the Hero Banner
        mainViewModel.currentLogo.observe(viewLifecycleOwner) { logoUrl ->
            if (!logoUrl.isNullOrEmpty()) {
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

        // Observe cast list and update actor chips in hero banner
        mainViewModel.castList.observe(viewLifecycleOwner) { castList ->
            binding.actorChips.removeAllViews()
            if (castList.isNotEmpty()) {
                castList.take(3).forEach { actor ->
                    val chip = Chip(requireContext())
                    chip.text = actor.name
                    chip.isClickable = true
                    chip.isFocusable = true
                    chip.setChipBackgroundColorResource(R.color.md_theme_surfaceContainer)
                    chip.setTextColor(resources.getColor(R.color.text_primary, null))
                    chip.setOnClickListener {
                        val personId = actor.id.removePrefix("tmdb:").toIntOrNull()
                        if (personId != null) {
                            mainViewModel.loadPersonCredits(personId)
                        }
                    }
                    binding.actorChips.addView(chip)
                }
            }
        }

        // Observer for series metadata (for drill-down navigation)
        mainViewModel.metaDetails.observe(viewLifecycleOwner) { meta ->
            if (meta != null && meta.type == "series" && currentDrillDownLevel == DrillDownLevel.SERIES) {
                // Display seasons in the carousel (replace rows view)
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
                }?.distinctBy { it.id } ?: emptyList()

                showDrillDownContent(seasons, "${meta.name} - Seasons")
            }
        }

        // Observer for season episodes (for drill-down navigation)
        mainViewModel.seasonEpisodes.observe(viewLifecycleOwner) { episodes ->
            if (currentDrillDownLevel == DrillDownLevel.SEASON && episodes.isNotEmpty()) {
                val label = currentSeriesName?.let { "$it - Season $currentSeasonNumber" }
                    ?: "Season $currentSeasonNumber - Episodes"
                showDrillDownContent(episodes, label)
            }
        }
    }

    fun focusSidebar() {
        binding.rvHomeRows.post {
            binding.rvHomeRows.requestFocus()
        }
    }

    /**
     * Shows drill-down content (seasons or episodes) in place of library rows
     */
    private fun showDrillDownContent(items: List<MetaItem>, label: String) {
        if (items.isEmpty()) return

        // Create or update the drill-down adapter
        if (drillDownAdapter == null) {
            drillDownAdapter = PosterAdapter(
                items = items,
                onClick = { item -> handleSinglePress(item) },
                onLongClick = { item -> showLongPressMenu(item) }
            )
        } else {
            drillDownAdapter?.updateData(items)
        }

        // Replace rows with drill-down content
        binding.rvHomeRows.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        binding.rvHomeRows.adapter = drillDownAdapter

        // Update hero banner with first item
        if (items.isNotEmpty()) {
            updateHeroBanner(items[0])
        }

        // Setup focus change listener for drill-down items
        binding.rvHomeRows.setOnHierarchyChangeListener(object : ViewGroup.OnHierarchyChangeListener {
            override fun onChildViewAdded(parent: View?, child: View?) {
                child?.setOnFocusChangeListener { v, hasFocus ->
                    if (hasFocus) {
                        val pos = binding.rvHomeRows.getChildAdapterPosition(v)
                        if (pos != androidx.recyclerview.widget.RecyclerView.NO_POSITION) {
                            val item = drillDownAdapter?.getItem(pos)
                            if (item != null) {
                                heroUpdateJob?.cancel()
                                heroUpdateJob = viewLifecycleOwner.lifecycleScope.launch {
                                    delay(200)
                                    if (isAdded) {
                                        updateHeroBanner(item)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            override fun onChildViewRemoved(parent: View?, child: View?) {
                child?.onFocusChangeListener = null
            }
        })

        // Focus first item after a short delay
        binding.root.postDelayed({
            binding.rvHomeRows.scrollToPosition(0)
            binding.rvHomeRows.layoutManager?.findViewByPosition(0)?.requestFocus()
        }, 100)
    }

    /**
     * Restores the library rows view from drill-down state
     */
    private fun restoreLibraryRowsView() {
        // Restore original layout manager and adapter
        binding.rvHomeRows.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        binding.rvHomeRows.adapter = rowAdapter

        // Restore library rows data
        if (cachedLibraryRows.isNotEmpty()) {
            rowAdapter.updateData(cachedLibraryRows, this::showLongPressMenu)
            if (cachedLibraryRows[0].items.isNotEmpty()) {
                updateHeroBanner(cachedLibraryRows[0].items[0])
            }
        }

        // Focus first item
        binding.root.postDelayed({
            binding.rvHomeRows.scrollToPosition(0)
            binding.rvHomeRows.requestFocus()
        }, 100)
    }

    /**
     * Handles back button press for drill-down navigation
     * Returns true if handled, false otherwise
     */
    fun handleBackPress(): Boolean {
        when (currentDrillDownLevel) {
            DrillDownLevel.SEASON -> {
                // Go back to seasons view
                currentSeriesId?.let { seriesId ->
                    currentDrillDownLevel = DrillDownLevel.SERIES
                    currentSeasonNumber = null
                    mainViewModel.loadSeriesMeta(seriesId)
                    return true
                }
            }
            DrillDownLevel.SERIES -> {
                // Go back to library rows view
                currentDrillDownLevel = DrillDownLevel.ROWS
                currentSeriesId = null
                currentSeriesName = null
                currentSeasonNumber = null
                restoreLibraryRowsView()
                return true
            }
            DrillDownLevel.ROWS -> {
                // At top level, don't consume back press
                return false
            }
        }
        return false
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        heroUpdateJob?.cancel()
    }
}
