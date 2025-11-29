package com.example.stremiompvplayer.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
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
import com.example.stremiompvplayer.viewmodels.HomeViewModel
import com.example.stremiompvplayer.viewmodels.HomeViewModelFactory
import com.example.stremiompvplayer.viewmodels.MainViewModel
import com.example.stremiompvplayer.viewmodels.MainViewModelFactory
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by viewModels {
        HomeViewModelFactory(
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

    // Helper module for dialogs (lazy initialized)
    private lateinit var displayModule: ResultsDisplayModule

    // Drill-down navigation state
    enum class DrillDownLevel { ROWS, SERIES, SEASON, CAST_RESULTS }
    private var currentDrillDownLevel = DrillDownLevel.ROWS
    private var currentSeriesId: String? = null
    private var currentSeasonNumber: Int? = null
    private var currentSeriesName: String? = null

    // Adapter for drill-down content (seasons/episodes/cast results)
    private var drillDownAdapter: PosterAdapter? = null

    // Store current home rows for restoration
    private var cachedHomeRows: List<HomeRow> = emptyList()

    // Cast search results
    private var castMovieResults: List<MetaItem> = emptyList()
    private var castShowResults: List<MetaItem> = emptyList()
    private var currentCastPersonName: String? = null
    private var isCastMovieSection: Boolean = true // Track which cast section is showing

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

        // Initialize display module for helper methods
        // Note: We pass null/dummies for views because we are handling the Hero banner manually in this fragment
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
        viewModel.loadHomeContent()
    }

    private fun setupRecyclerView() {
        rowAdapter = HomeRowAdapter(
            onContentClick = { item ->
                handleSinglePress(item)
            },
            onContentFocused = { item ->
                updateHeroBanner(item)
            }
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
                // PlayerActivity will handle scraping and auto-play.
                val intent = android.content.Intent(requireContext(), PlayerActivity::class.java).apply {
                    putExtra("meta", item)
                }
                startActivity(intent)
            }
            "series" -> {
                // Drill down into series IN-PLACE (not navigating to another fragment)
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
                // Fallback for other types (e.g. Person?)
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

        // Set the title
        val titleView = view.findViewById<TextView>(R.id.menuTitle)
        titleView.text = item.name

        // --- Actions ---

        // 1. Scrape Streams (Explicit)
        view.findViewById<View>(R.id.actionScrape).setOnClickListener {
            dialog.dismiss()
            displayModule.showStreamDialog(item)
        }

        // 2. Watch Trailer - Uses external YouTube player
        view.findViewById<View>(R.id.actionTrailer).setOnClickListener {
            dialog.dismiss()
            displayModule.playTrailer(item)
        }

        // 3. Watchlist Toggle - Check Trakt watchlist status and toggle accordingly
        val actionWatchlist = view.findViewById<TextView>(R.id.actionWatchlist)
        lifecycleScope.launch {
            // Check if Trakt is enabled to determine initial state
            val isTraktEnabled = mainViewModel.isTraktEnabled.value ?: false
            // For now, default to "Add to Watchlist" - could be enhanced with actual status check
            actionWatchlist.text = "Add to Watchlist"
        }
        actionWatchlist.setOnClickListener {
            dialog.dismiss()
            // Toggle watchlist - adds if not present, removes if present
            mainViewModel.addToWatchlist(item)
        }

        // 4. Library Toggle - Shows proper toggle state
        val actionLibrary = view.findViewById<TextView>(R.id.actionLibrary)
        lifecycleScope.launch {
            val inLib = mainViewModel.isItemInLibrarySync(item.id)
            actionLibrary.text = if (inLib) "Remove from Library" else "Add to Library"
        }
        actionLibrary.setOnClickListener {
            dialog.dismiss()
            mainViewModel.toggleLibrary(item)
        }

        // 5. Watched Toggle - Shows proper toggle state
        val actionWatched = view.findViewById<TextView>(R.id.actionWatched)
        actionWatched.text = if (item.isWatched) "Mark Unwatched" else "Mark Watched"
        actionWatched.setOnClickListener {
            dialog.dismiss()
            if (item.isWatched) mainViewModel.clearWatchedStatus(item) else mainViewModel.markAsWatched(item)
        }

        // 6. Not Watching - Complete removal from all lists
        view.findViewById<View>(R.id.actionNotWatching).setOnClickListener {
            dialog.dismiss()
            mainViewModel.markAsNotWatching(item)
        }

        // --- Cast Section ---
        // Fetch cast and populate chips
        val castGroup = view.findViewById<ChipGroup>(R.id.castChipGroup)

        // Create a temporary observer for this dialog
        val castObserver = androidx.lifecycle.Observer<List<MetaItem>> { castList ->
            if (dialog.isShowing) {
                castGroup.removeAllViews()
                castList.take(10).forEach { actor ->
                    val chip = Chip(requireContext())
                    chip.text = actor.name
                    chip.setOnClickListener {
                        dialog.dismiss()
                        // Trigger actor search IN-PLACE
                        val personId = actor.id.removePrefix("tmdb:").toIntOrNull()
                        if (personId != null) {
                            currentCastPersonName = actor.name
                            mainViewModel.loadPersonCredits(personId)
                            // The results will be handled by the personCredits observer
                            // which splits them into movies and shows
                        }
                    }
                    castGroup.addView(chip)
                }
            }
        }

        mainViewModel.castList.observe(viewLifecycleOwner, castObserver)
        // Trigger fetch
        mainViewModel.fetchCast(item.id, item.type)

        dialog.setOnDismissListener {
            mainViewModel.castList.removeObserver(castObserver)
        }

        dialog.show()
    }

    private fun updateHeroBanner(item: MetaItem) {
        heroUpdateJob?.cancel()
        heroUpdateJob = viewLifecycleOwner.lifecycleScope.launch {
            delay(200)
            if (!isAdded) return@launch

            Glide.with(this@HomeFragment)
                .load(item.background ?: item.poster)
                .into(binding.pageBackground)

            // --- Logo Handling ---
            // We check if we have a cached logo URL in MainViewModel, or trigger a fetch.
            // Since MainViewModel handles one logo at a time, fast scrolling might get tricky.
            // For now, we'll use text title and try to fetch logo.

            binding.detailTitle.text = item.name
            binding.detailTitle.visibility = View.VISIBLE
            binding.detailLogo.visibility = View.GONE

            // Trigger logo fetch (updates mainViewModel.currentLogo)
            mainViewModel.fetchItemLogo(item)

            binding.detailDescription.text = item.description ?: "No description available."

            binding.detailRating.visibility = if (item.rating != null) View.VISIBLE else View.GONE
            binding.detailRating.text = "★ ${item.rating}"

            // Format date
            val dateText = try {
                item.releaseDate?.take(4) ?: ""
            } catch (e: Exception) { "" }
            binding.detailDate.text = dateText

            // Episode Info
            if (item.type == "episode") {
                binding.detailEpisode.visibility = View.VISIBLE
                // Try to parse SxxExx from ID or Name if not explicit
                if (item.id.split(":").size >= 4) {
                    val parts = item.id.split(":")
                    binding.detailEpisode.text = "S${parts[2]}E${parts[3]}"
                } else {
                    binding.detailEpisode.text = "Episode"
                }
            } else {
                binding.detailEpisode.visibility = View.GONE
            }

            // --- Cast Chips in Hero ---
            // Clear existing
            binding.actorChips.removeAllViews()
            // We won't trigger a network call for cast on every scroll to avoid rate limiting.
            // Only update if we already have data or maybe for the very first item.
        }
    }

    private fun setupObservers() {
        viewModel.homeRows.observe(viewLifecycleOwner) { rows ->
            cachedHomeRows = rows
            // Only update rows view if we're at ROWS level
            if (currentDrillDownLevel == DrillDownLevel.ROWS) {
                rowAdapter.updateData(rows, this::showLongPressMenu)

                if (rows.isNotEmpty() && rows[0].items.isNotEmpty()) {
                    updateHeroBanner(rows[0].items[0])
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
                // Keep title visible if no logo found
                binding.detailTitle.visibility = View.VISIBLE
                binding.detailLogo.visibility = View.GONE
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

        // Observer for person credits (cast search results)
        mainViewModel.searchResults.observe(viewLifecycleOwner) { results ->
            if (currentCastPersonName != null && results.isNotEmpty()) {
                // Split results into movies and shows
                castMovieResults = results.filter { it.type == "movie" }
                castShowResults = results.filter { it.type == "series" || it.type == "tv" }.map { item ->
                    // Normalize "tv" type to "series"
                    if (item.type == "tv") item.copy(type = "series") else item
                }

                // Show cast results in-place, starting with movies
                currentDrillDownLevel = DrillDownLevel.CAST_RESULTS
                isCastMovieSection = true
                
                val displayItems = if (castMovieResults.isNotEmpty()) {
                    castMovieResults
                } else {
                    isCastMovieSection = false
                    castShowResults
                }
                
                val label = if (isCastMovieSection) {
                    "$currentCastPersonName - Movies (${castMovieResults.size})"
                } else {
                    "$currentCastPersonName - Shows (${castShowResults.size})"
                }
                
                showDrillDownContent(displayItems, label)
                
                // Note: We keep currentCastPersonName set because we need it for navigation
                // and switching between movie/show sections. It will be cleared when
                // the user navigates back to the home rows view.
            }
        }
    }

    fun focusSidebar() {
        binding.rvHomeRows.post {
            binding.rvHomeRows.requestFocus()
        }
    }

    /**
     * Shows drill-down content (seasons, episodes, or cast results) in place of home rows
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
        // We re-use the same RecyclerView but change the layout manager and adapter
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
     * Restores the home rows view from drill-down state
     */
    private fun restoreHomeRowsView() {
        // Restore original layout manager and adapter
        binding.rvHomeRows.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        binding.rvHomeRows.adapter = rowAdapter

        // Restore home rows data
        if (cachedHomeRows.isNotEmpty()) {
            rowAdapter.updateData(cachedHomeRows, this::showLongPressMenu)
            if (cachedHomeRows[0].items.isNotEmpty()) {
                updateHeroBanner(cachedHomeRows[0].items[0])
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
                // Go back to home rows view
                currentDrillDownLevel = DrillDownLevel.ROWS
                currentSeriesId = null
                currentSeriesName = null
                currentSeasonNumber = null
                restoreHomeRowsView()
                return true
            }
            DrillDownLevel.CAST_RESULTS -> {
                if (!isCastMovieSection && castMovieResults.isNotEmpty()) {
                    // If showing shows, go back to movies
                    isCastMovieSection = true
                    showDrillDownContent(
                        castMovieResults,
                        "$currentCastPersonName - Movies (${castMovieResults.size})"
                    )
                    return true
                } else {
                    // Go back to home rows view
                    currentDrillDownLevel = DrillDownLevel.ROWS
                    currentCastPersonName = null
                    castMovieResults = emptyList()
                    castShowResults = emptyList()
                    restoreHomeRowsView()
                    return true
                }
            }
            DrillDownLevel.ROWS -> {
                // At top level, don't consume back press
                return false
            }
        }
        return false
    }

    /**
     * Switches between cast movie and show sections using DPAD_DOWN
     */
    fun handleKeyDown(keyCode: Int, event: android.view.KeyEvent?): Boolean {
        if (event?.action == android.view.KeyEvent.ACTION_DOWN) {
            if (keyCode == android.view.KeyEvent.KEYCODE_DPAD_DOWN) {
                // In cast results mode, cycle between movies and shows sections
                if (currentDrillDownLevel == DrillDownLevel.CAST_RESULTS) {
                    if (isCastMovieSection && castShowResults.isNotEmpty()) {
                        // Switch from movies to shows
                        isCastMovieSection = false
                        showDrillDownContent(
                            castShowResults,
                            "$currentCastPersonName - Shows (${castShowResults.size})"
                        )
                        return true
                    } else if (!isCastMovieSection && castMovieResults.isNotEmpty()) {
                        // Switch from shows to movies
                        isCastMovieSection = true
                        showDrillDownContent(
                            castMovieResults,
                            "$currentCastPersonName - Movies (${castMovieResults.size})"
                        )
                        return true
                    }
                }
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