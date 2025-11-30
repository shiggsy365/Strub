package com.example.stremiompvplayer.ui.search

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.stremiompvplayer.PlayerActivity
import com.example.stremiompvplayer.R
import com.example.stremiompvplayer.adapters.HomeRowAdapter
import com.example.stremiompvplayer.adapters.PosterAdapter
import com.example.stremiompvplayer.data.ServiceLocator
import com.example.stremiompvplayer.databinding.FragmentSearchNewBinding
import com.example.stremiompvplayer.models.MetaItem
import com.example.stremiompvplayer.utils.SharedPreferencesManager
import com.example.stremiompvplayer.viewmodels.HomeRow
import com.example.stremiompvplayer.viewmodels.MainViewModel
import com.example.stremiompvplayer.viewmodels.MainViewModelFactory
import com.example.stremiompvplayer.ui.ResultsDisplayModule
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.text.format

class SearchFragment : Fragment() {

    private var _binding: FragmentSearchNewBinding? = null
    private val binding get() = _binding!!

    companion object {
        private const val ARG_TYPE = "type"

        fun newInstance(type: String = "movie"): SearchFragment {
            return SearchFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_TYPE, type)
                }
            }
        }
    }

    private val viewModel: MainViewModel by activityViewModels {
        MainViewModelFactory(
            ServiceLocator.getInstance(requireContext()),
            SharedPreferencesManager.getInstance(requireContext())
        )
    }

    // Row-based adapter for Home-style layout
    private lateinit var rowAdapter: HomeRowAdapter
    
    // Drill-down adapter for seasons/episodes (when navigating into a series)
    private var drillDownAdapter: PosterAdapter? = null
    
    private lateinit var displayModule: ResultsDisplayModule
    private var currentSearchQuery: String? = null

    private var heroUpdateJob: Job? = null

    // Drill-down navigation state
    enum class DrillDownLevel { SEARCH_RESULTS, SERIES, SEASON }
    private var currentDrillDownLevel = DrillDownLevel.SEARCH_RESULTS
    private var currentSeriesId: String? = null
    private var currentSeasonNumber: Int? = null
    private var currentSeriesName: String? = null

    // Cache search results for restoration after drill-down
    private var cachedMovieResults: List<MetaItem> = emptyList()
    private var cachedSeriesResults: List<MetaItem> = emptyList()

    /**
     * Helper data class for parsed season ID components
     */
    private data class SeasonIdComponents(val seriesId: String, val seasonNumber: Int)

    /**
     * Parses a season ID in format "tmdb:12345:2" to extract series ID and season number.
     * Returns null if the format is invalid.
     */
    private fun parseSeasonId(seasonId: String): SeasonIdComponents? {
        val parts = seasonId.split(":")
        if (parts.size < 2) return null
        
        val seriesId = parts.dropLast(1).joinToString(":")
        val seasonNum = parts.lastOrNull()?.toIntOrNull() ?: return null
        
        return SeasonIdComponents(seriesId, seasonNum)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSearchNewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupDisplayModule()
        setupRecyclerView()
        setupObservers()
        setupSearchListeners()

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
            actorChips = binding.actorChips,
            btnPlay = null, btnTrailer = null, btnRelated = null
        )
        displayModule = ResultsDisplayModule(this, viewModel, config)

        // Setup callbacks
        displayModule.onActorClicked = { personId, actorName ->
            viewModel.loadPersonCredits(personId)
        }

        displayModule.onRelatedContentLoaded = { similarContent ->
            // Update search results with related content
            val movies = similarContent.filter { it.type == "movie" }
            val series = similarContent.filter { it.type == "series" || it.type == "tv" }.map { item ->
                if (item.type == "tv") item.copy(type = "series") else item
            }
            cachedMovieResults = movies
            cachedSeriesResults = series
            updateSearchRows(movies, series)
            if (similarContent.isNotEmpty()) {
                updateHeroBanner(similarContent[0])
            }
        }
    }

    private fun setupSearchListeners() {

    }

    private fun setupRecyclerView() {
        rowAdapter = HomeRowAdapter(
            onContentClick = { item ->
                handleContentClick(item)
            },
            onContentFocused = { item ->
                updateHeroBanner(item)
            }
        )

        binding.rvSearchRows.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
            adapter = rowAdapter
            setItemViewCacheSize(10)
        }
    }

    private fun handleContentClick(item: MetaItem) {
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
                viewModel.loadSeriesMeta(item.id)
                updateHeroBanner(item)
            }
            "season" -> {
                // Drill down into season to show episodes
                val seasonComponents = parseSeasonId(item.id)
                if (seasonComponents != null) {
                    currentDrillDownLevel = DrillDownLevel.SEASON
                    currentSeriesId = seasonComponents.seriesId
                    currentSeasonNumber = seasonComponents.seasonNumber
                    viewModel.loadSeasonEpisodes(seasonComponents.seriesId, seasonComponents.seasonNumber)
                    updateHeroBanner(item)
                }
            }
            else -> {
                // Fallback for other types
                displayModule.showStreamDialog(item)
            }
        }
    }

    private fun updateHeroBanner(item: MetaItem) {
        heroUpdateJob?.cancel()
        heroUpdateJob = viewLifecycleOwner.lifecycleScope.launch {
            delay(200)
            if (!isAdded) return@launch

            Glide.with(this@SearchFragment)
                .load(item.background ?: item.poster)
                .into(binding.pageBackground)

            binding.detailTitle.text = item.name
            binding.detailTitle.visibility = View.VISIBLE
            binding.detailLogo.visibility = View.GONE

            // Trigger logo fetch
            viewModel.fetchItemLogo(item)

            binding.detailDescription.text = item.description ?: "No description available."

            binding.detailRating.visibility = if (item.rating != null) View.VISIBLE else View.GONE
            binding.detailRating.text = "★ ${item.rating}"

            // Format date
            val dateText = try {
                item.releaseDate?.take(4) ?: ""
            } catch (e: Exception) { "" }
            binding.detailDate.text = formatReleaseDate(dateText)

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

            // Fetch and update cast chips
            binding.actorChips.removeAllViews()
            viewModel.fetchCast(item.id, item.type)

            // Show hero card when we have content
            binding.pageBackground.visibility = View.VISIBLE
        }
    }

    fun formatReleaseDate(dateStr: String?): String {
        if (dateStr.isNullOrBlank()) return ""
        val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val outputFormat = SimpleDateFormat("d MMM yyyy", Locale.US)
        return try {
            val date = inputFormat.parse(dateStr)
            val day = SimpleDateFormat("d", Locale.US).format(date).toInt()
            val suffix = when {
                day in 11..13 -> "th"
                day % 10 == 1 -> "st"
                day % 10 == 2 -> "nd"
                day % 10 == 3 -> "rd"
                else -> "th"
            }
            val formatted = outputFormat.format(date)
            formatted.replaceFirst(Regex("\\d+"), "$day$suffix")
        } catch (e: Exception) {
            ""
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

        // 3. Watchlist Toggle - Uses toggleWatchlist for proper add/remove behavior
        val actionWatchlist = view.findViewById<TextView>(R.id.actionWatchlist)
        // Default text - actual toggle behavior will add if not present, remove if present
        actionWatchlist.text = "Toggle Watchlist"
        actionWatchlist.setOnClickListener {
            dialog.dismiss()
            viewModel.toggleWatchlist(item)
        }

        // 4. Library Toggle - Shows proper toggle state
        val actionLibrary = view.findViewById<TextView>(R.id.actionLibrary)
        lifecycleScope.launch {
            val inLib = viewModel.isItemInLibrarySync(item.id)
            actionLibrary.text = if (inLib) "Remove from Library" else "Add to Library"
        }
        actionLibrary.setOnClickListener {
            dialog.dismiss()
            viewModel.toggleLibrary(item)
        }

        // 5. Watched Toggle - Shows proper toggle state
        val actionWatched = view.findViewById<TextView>(R.id.actionWatched)
        actionWatched.text = if (item.isWatched) "Mark Unwatched" else "Mark Watched"
        actionWatched.setOnClickListener {
            dialog.dismiss()
            if (item.isWatched) viewModel.clearWatchedStatus(item) else viewModel.markAsWatched(item)
        }

        // 6. Not Watching - Complete removal from all lists
        view.findViewById<View>(R.id.actionNotWatching).setOnClickListener {
            dialog.dismiss()
            viewModel.markAsNotWatching(item)
        }

        // --- Cast Section ---
        val castGroup = view.findViewById<ChipGroup>(R.id.castChipGroup)

        val castObserver = androidx.lifecycle.Observer<List<MetaItem>> { castList ->
            if (dialog.isShowing) {
                castGroup.removeAllViews()
                castList.take(3).forEach { actor ->
                    val chip = Chip(requireContext())
                    chip.text = actor.name
                    chip.setOnClickListener {
                        dialog.dismiss()
                        val personId = actor.id.removePrefix("tmdb:").toIntOrNull()
                        if (personId != null) {
                            viewModel.loadPersonCredits(personId)
                        }
                    }
                    castGroup.addView(chip)
                }
            }
        }

        viewModel.castList.observe(viewLifecycleOwner, castObserver)
        viewModel.fetchCast(item.id, item.type)

        dialog.setOnDismissListener {
            viewModel.castList.removeObserver(castObserver)
        }

        dialog.show()
        dialog.window?.apply {
            val width = (resources.displayMetrics.widthPixels * 0.8).toInt()
            setLayout(width, android.view.WindowManager.LayoutParams.WRAP_CONTENT)
            setGravity(android.view.Gravity.CENTER)
        }
    }

    fun performSearch(query: String) {
        if (query.isBlank()) return
        currentSearchQuery = query

        // Reset drill-down state when performing new search
        currentDrillDownLevel = DrillDownLevel.SEARCH_RESULTS
        currentSeriesId = null
        currentSeasonNumber = null
        currentSeriesName = null

        // Restore row adapter if we were in drill-down mode
        if (binding.rvSearchRows.adapter !is HomeRowAdapter) {
            binding.rvSearchRows.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
            binding.rvSearchRows.adapter = rowAdapter
        }

        // Trigger search - results will be handled by the observer
        viewModel.searchTMDB(query)
    }

    private fun updateSearchRows(movies: List<MetaItem>, series: List<MetaItem>) {
        val rows = mutableListOf<HomeRow>()

        if (movies.isNotEmpty()) {
            rows.add(HomeRow("search_movies", "Movies", movies))
        }

        if (series.isNotEmpty()) {
            rows.add(HomeRow("search_series", "Series", series))
        }

        rowAdapter.updateData(rows, this::showLongPressMenu)

        // Update visibility
        if (rows.isEmpty()) {
            binding.rvSearchRows.visibility = View.GONE
            binding.pageBackground.visibility = View.GONE
        } else {
            binding.rvSearchRows.visibility = View.VISIBLE
            binding.pageBackground.visibility = View.VISIBLE


            // Update hero banner with first item
            val firstItem = movies.firstOrNull() ?: series.firstOrNull()
            if (firstItem != null) {
                updateHeroBanner(firstItem)
            }

            // Auto-focus first item
            binding.root.postDelayed({
                binding.rvSearchRows.requestFocus()
            }, 100)
        }
    }

    /**
     * Shows drill-down content (seasons or episodes) in place of search result rows
     */
    private fun showDrillDownContent(items: List<MetaItem>, label: String) {
        if (items.isEmpty()) return

        // Create or update the drill-down adapter
        if (drillDownAdapter == null) {
            drillDownAdapter = PosterAdapter(
                items = items,
                onClick = { item -> handleContentClick(item) },
                onLongClick = { item -> showLongPressMenu(item) }
            )
        } else {
            drillDownAdapter?.updateData(items)
        }

        // Replace rows with drill-down content
        binding.rvSearchRows.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        binding.rvSearchRows.adapter = drillDownAdapter

        // Update hero banner with first item
        if (items.isNotEmpty()) {
            updateHeroBanner(items[0])
        }

        // Setup focus change listener for drill-down items
        binding.rvSearchRows.setOnHierarchyChangeListener(object : ViewGroup.OnHierarchyChangeListener {
            override fun onChildViewAdded(parent: View?, child: View?) {
                child?.setOnFocusChangeListener { v, hasFocus ->
                    if (hasFocus) {
                        val pos = binding.rvSearchRows.getChildAdapterPosition(v)
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
            binding.rvSearchRows.scrollToPosition(0)
            binding.rvSearchRows.layoutManager?.findViewByPosition(0)?.requestFocus()
        }, 100)
    }

    /**
     * Restores the search results rows view from drill-down state
     */
    private fun restoreSearchRowsView() {
        // Clear hierarchy listener to prevent memory leaks
        binding.rvSearchRows.setOnHierarchyChangeListener(null)
        
        // Restore original layout manager and adapter
        binding.rvSearchRows.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        binding.rvSearchRows.adapter = rowAdapter

        // Restore search results
        updateSearchRows(cachedMovieResults, cachedSeriesResults)

        // Focus first item
        binding.root.postDelayed({
            binding.rvSearchRows.scrollToPosition(0)
            binding.rvSearchRows.requestFocus()
        }, 100)
    }

    private fun setupObservers() {
        // Observe search results
        viewModel.searchResults.observe(viewLifecycleOwner) { results ->
            if (currentDrillDownLevel == DrillDownLevel.SEARCH_RESULTS) {
                // Normalize "tv" type to "series" for consistency
                val normalizedResults = results.map { item ->
                    if (item.type == "tv") {
                        item.copy(type = "series")
                    } else {
                        item
                    }
                }

                // Separate movies and series
                val movies = normalizedResults.filter { it.type == "movie" }
                val series = normalizedResults.filter { it.type == "series" }

                // Cache for restoration after drill-down
                cachedMovieResults = movies
                cachedSeriesResults = series

                // Update search rows
                updateSearchRows(movies, series)
            }
        }

        viewModel.actionResult.observe(viewLifecycleOwner) { result ->
            when (result) {
                is MainViewModel.ActionResult.Success -> {
                    Toast.makeText(requireContext(), result.message, Toast.LENGTH_SHORT).show()
                }
                is MainViewModel.ActionResult.Error -> {
                    Toast.makeText(requireContext(), result.message, Toast.LENGTH_LONG).show()
                }
            }
        }

        // Observe series meta details for drill-down navigation
        viewModel.metaDetails.observe(viewLifecycleOwner) { meta ->
            if (meta != null && meta.type == "series" && currentDrillDownLevel == DrillDownLevel.SERIES) {
                // Display seasons in horizontal list
                val seasons = meta.videos?.mapNotNull { video ->
                    video.season?.let { seasonNum ->
                        MetaItem(
                            id = "${meta.id}:$seasonNum",
                            type = "season",
                            name = video.title ?: "Season $seasonNum",
                            poster = video.thumbnail ?: meta.poster,
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

        // Observe season episodes for drill-down navigation
        viewModel.seasonEpisodes.observe(viewLifecycleOwner) { episodes ->
            if (currentDrillDownLevel == DrillDownLevel.SEASON && episodes.isNotEmpty()) {
                val label = currentSeriesName?.let { "$it - Season $currentSeasonNumber" }
                    ?: "Season $currentSeasonNumber - Episodes"
                showDrillDownContent(episodes, label)
            }
        }

        // Observe Logo updates for the Hero Banner
        viewModel.currentLogo.observe(viewLifecycleOwner) { logoUrl ->
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
        viewModel.castList.observe(viewLifecycleOwner) { castList ->
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
                        // Handle both "tmdb:" prefixed IDs and plain numeric IDs
                        val idStr = if (actor.id.startsWith("tmdb:")) actor.id.removePrefix("tmdb:") else actor.id
                        val personId = idStr.toIntOrNull()
                        if (personId != null) {
                            viewModel.loadPersonCredits(personId)
                        } else {
                            Log.w("SearchFragment", "Invalid cast ID format: ${actor.id}")
                        }
                    }
                    binding.actorChips.addView(chip)
                }
            }
        }
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
                    viewModel.loadSeriesMeta(seriesId)
                    return true
                }
            }
            DrillDownLevel.SERIES -> {
                // Go back to search results view
                currentDrillDownLevel = DrillDownLevel.SEARCH_RESULTS
                currentSeriesId = null
                currentSeriesName = null
                currentSeasonNumber = null
                restoreSearchRowsView()
                return true
            }
            DrillDownLevel.SEARCH_RESULTS -> {
                // At top level, don't consume back press
                return false
            }
        }
        return false
    }


    /**
     * Focuses the first item in the search results RecyclerView.
     * Called when navigating to search from the main menu.
     */
    fun focusFirstItem() {
        binding.root.post {
            binding.rvSearchRows.scrollToPosition(0)
            binding.rvSearchRows.requestFocus()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Clean up hierarchy listener to prevent memory leaks
        _binding?.rvSearchRows?.setOnHierarchyChangeListener(null)
        _binding = null
        viewModel.clearSearchResults()
        heroUpdateJob?.cancel()
    }


}