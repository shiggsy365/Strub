package com.example.stremiompvplayer.ui.search

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.ContextThemeWrapper

import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.PopupMenu
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.bumptech.glide.Glide
import com.example.stremiompvplayer.DetailsActivity2
import com.example.stremiompvplayer.R
import com.example.stremiompvplayer.UserSelectionActivity
import com.example.stremiompvplayer.adapters.PosterAdapter
import com.example.stremiompvplayer.data.ServiceLocator
import com.example.stremiompvplayer.databinding.FragmentSearchNewBinding
import com.example.stremiompvplayer.models.MetaItem
import com.example.stremiompvplayer.utils.SharedPreferencesManager
import com.example.stremiompvplayer.viewmodels.MainViewModel
import com.example.stremiompvplayer.viewmodels.MainViewModelFactory
import com.example.stremiompvplayer.ui.ResultsDisplayModule
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

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

    private fun updateItemUI(item: MetaItem, isWatched: Boolean) {
        item.isWatched = isWatched
        item.progress = if (isWatched) item.duration else 0

        val position = contentAdapter.getItemPosition(item)
        if (position != -1) {
            contentAdapter.notifyItemChanged(position)
        }
    }

    private lateinit var contentAdapter: PosterAdapter
    private lateinit var searchAdapter: PosterAdapter
    private lateinit var displayModule: ResultsDisplayModule
    private var movieResults = listOf<MetaItem>()
    private var seriesResults = listOf<MetaItem>()
    private var currentResultIndex = 0  // 0 for movies, 1 for series
    private var currentSearchQuery: String? = null

    private var detailsUpdateJob: Job? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSearchNewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupDisplayModule()
        setupRecyclerView()
        setupObservers()
        setupKeyHandling()
        setupSearchListeners()
        binding.searchEditText.requestFocus()
    }

    private fun setupDisplayModule() {
        val config = ResultsDisplayModule.Configuration(
            pageBackground = binding.pageBackground,
            detailTitle = binding.detailTitle,
            detailLogo = binding.detailLogo,
            detailDescription = binding.detailDescription,
            detailDate = binding.detailDate,
            detailRating = binding.detailRating,
            detailEpisode = binding.detailEpisodeNumber,
            actorChips = binding.root.findViewById(R.id.actorChips) ?: com.google.android.material.chip.ChipGroup(requireContext()),
            btnPlay = binding.btnPlay,
            btnTrailer = null,  // Search doesn't have trailer button
            btnRelated = binding.btnRelated,
            enableDrillDown = true,  // Enable drill-down for series navigation
            useGridLayout = true,
            showEpisodeDescription = false
        )
        displayModule = ResultsDisplayModule(this, viewModel, config)

        // Setup callbacks
        displayModule.onActorClicked = { personId, actorName ->
            viewModel.loadPersonCredits(personId)
            // Label update could be added if needed
        }

        displayModule.onRelatedContentLoaded = { similarContent ->
            // Update search results with related content
            movieResults = similarContent
            seriesResults = emptyList()
            currentResultIndex = 0
            updateDisplayedResults()
        }
    }

    private fun setupSearchListeners() {
        binding.searchEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val query = binding.searchEditText.text.toString()
                if (query.isNotBlank()) {
                    performSearch(query)
                    hideKeyboard()
                }
                true
            } else {
                false
            }
        }

        binding.searchButton.setOnClickListener {
            val query = binding.searchEditText.text.toString()
            if (query.isNotBlank()) {
                performSearch(query)
                hideKeyboard()
            }
        }
    }

    private fun setupKeyHandling() {
        // Set key listener for cycling through movie/series results
        binding.root.setOnKeyListener { _, keyCode, event ->
            if (event.action == KeyEvent.ACTION_DOWN) {
                when (keyCode) {
                    KeyEvent.KEYCODE_DPAD_DOWN -> {
                        val focusedChild = binding.resultsRecycler.focusedChild
                        if (focusedChild != null && (movieResults.isNotEmpty() || seriesResults.isNotEmpty())) {
                            cycleToNextResultType()
                            return@setOnKeyListener true
                        }
                        false
                    }
                    else -> false
                }
            } else {
                false
            }
        }
    }

    private fun cycleToNextResultType() {
        val hasMovies = movieResults.isNotEmpty()
        val hasSeries = seriesResults.isNotEmpty()

        if (!hasMovies && !hasSeries) return

        // Cycle between movies (0) and series (1)
        if (hasMovies && hasSeries) {
            currentResultIndex = (currentResultIndex + 1) % 2
        } else if (hasMovies) {
            currentResultIndex = 0
        } else {
            currentResultIndex = 1
        }

        updateDisplayedResults()
    }

    private fun updateDisplayedResults() {
        // Don't update if we're in drill-down mode
        if (displayModule.currentDrillDownLevel != ResultsDisplayModule.DrillDownLevel.CATALOG) {
            return
        }

        val results = if (currentResultIndex == 0 && movieResults.isNotEmpty()) {
            movieResults
        } else if (currentResultIndex == 1 && seriesResults.isNotEmpty()) {
            seriesResults
        } else if (movieResults.isNotEmpty()) {
            movieResults
        } else {
            seriesResults
        }

        searchAdapter.updateData(results)
        if (results.isNotEmpty()) {
            displayModule.updateDetailsPane(results[0])

            // Update label to show current type
            binding.root.post {
                val label = if (currentResultIndex == 0) "Movies" else "Series"
                // You might want to add a label TextView to show this
            }
        }

        // [FIX] Force focus to the first item when switching types
        binding.root.postDelayed({
            binding.resultsRecycler.scrollToPosition(0)
            binding.resultsRecycler.post {
                val firstView = binding.resultsRecycler.layoutManager?.findViewByPosition(0)
                firstView?.requestFocus()
            }
        }, 1000)
    }

    private fun setupRecyclerView() {
        searchAdapter = PosterAdapter(
            items = emptyList(),
            onClick = { item -> displayModule.onContentClicked(item) },  // Use standard navigation
            onLongClick = { item ->
                val pos = searchAdapter.getItemPosition(item)
                val holder = binding.resultsRecycler.findViewHolderForAdapterPosition(pos)
                if (holder != null) showItemMenu(holder.itemView, item)
            }
        )
        binding.resultsRecycler.apply {
            layoutManager = com.example.stremiompvplayer.utils.AutoFitGridLayoutManager(requireContext(), 140)
            adapter = searchAdapter
        }

        binding.resultsRecycler.addOnChildAttachStateChangeListener(object : androidx.recyclerview.widget.RecyclerView.OnChildAttachStateChangeListener {
            override fun onChildViewAttachedToWindow(view: View) {
                view.setOnFocusChangeListener { v, hasFocus ->
                    if (hasFocus) {
                        val position = binding.resultsRecycler.getChildAdapterPosition(v)
                        if (position != androidx.recyclerview.widget.RecyclerView.NO_POSITION) {
                            val item = searchAdapter.getItem(position)
                            if (item != null) {
                                detailsUpdateJob?.cancel()
                                detailsUpdateJob = viewLifecycleOwner.lifecycleScope.launch {
                                    delay(1000)
                                    if (isAdded) {
                                        displayModule.updateDetailsPane(item)
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


    private fun showItemMenu(view: View, item: MetaItem) {
        val wrapper = ContextThemeWrapper(requireContext(),
            android.R.style.Theme_DeviceDefault_Light_NoActionBar)
        val popup = PopupMenu(wrapper, view)

        // Use lifecycleScope for synchronous library check
        viewLifecycleOwner.lifecycleScope.launch {
            val isInLibrary = viewModel.isItemInLibrarySync(item.id)

            if (isInLibrary) {
                popup.menu.add("Remove from Library")
            } else {
                popup.menu.add("Add to Library")
            }

            popup.menu.add("Mark as Watched")
            popup.menu.add("Clear Progress")

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
                        item.progress = item.duration
                        refreshItem(item)
                        true
                    }
                    "Clear Progress" -> {
                        viewModel.clearWatchedStatus(item)
                        item.isWatched = false
                        item.progress = 0
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

    fun performSearch(query: String) {
        if (query.isBlank()) return
        currentSearchQuery = query

        // Reset drill-down state when performing new search
        displayModule.currentDrillDownLevel = ResultsDisplayModule.DrillDownLevel.CATALOG
        displayModule.currentSeriesId = null
        displayModule.currentSeasonNumber = null

        // Trigger search - results will be handled by the observer
        viewModel.searchTMDB(query)
    }

    private fun setupObservers() {
        viewModel.searchResults.observe(viewLifecycleOwner) { results ->
            // Don't update if we're in drill-down mode
            if (displayModule.currentDrillDownLevel != ResultsDisplayModule.DrillDownLevel.CATALOG) {
                return@observe
            }

            // Combined results sorted by popularity
            if (currentSearchQuery != null) {
                // Normalize "tv" type to "series" for consistency
                val normalizedResults = results.map { item ->
                    if (item.type == "tv") {
                        item.copy(type = "series")
                    } else {
                        item
                    }
                }

                // Separate and sort by rating
                val movies = normalizedResults.filter { it.type == "movie" }
                    .sortedByDescending { it.rating?.toDoubleOrNull() ?: 0.0 }

                val series = normalizedResults.filter { it.type == "series" }
                    .sortedByDescending { it.rating?.toDoubleOrNull() ?: 0.0 }

                // Interleave results: movie 0, series 0, movie 1, series 1, etc.
                val interleaved = mutableListOf<MetaItem>()
                val maxSize = maxOf(movies.size, series.size)
                for (i in 0 until maxSize) {
                    if (i < movies.size) interleaved.add(movies[i])
                    if (i < series.size) interleaved.add(series[i])
                }

                movieResults = interleaved
                seriesResults = emptyList()
                val combinedResults = interleaved

                if (combinedResults.isEmpty()) {
                    binding.emptyState.visibility = View.GONE
                    binding.noResultsState.visibility = View.VISIBLE
                    binding.contentGrid.visibility = View.GONE
                    binding.heroCard.visibility = View.GONE
                } else {
                    binding.emptyState.visibility = View.GONE
                    binding.noResultsState.visibility = View.GONE
                    binding.contentGrid.visibility = View.VISIBLE
                    binding.heroCard.visibility = View.VISIBLE
                    currentResultIndex = 0
                    updateDisplayedResults()

                    // Auto-focus first item when results load
                    binding.root.postDelayed({
                        if (binding.resultsRecycler.childCount > 0) {
                            val firstView = binding.resultsRecycler.layoutManager?.findViewByPosition(0)
                            firstView?.requestFocus()
                        }
                    }, 100)
                }
            } else {
                // Initial empty state
                // Normalize "tv" type to "series" for consistency
                val normalizedResults = results.map { item ->
                    if (item.type == "tv") {
                        item.copy(type = "series")
                    } else {
                        item
                    }
                }
                searchAdapter.updateData(normalizedResults)
                if (normalizedResults.isEmpty()) {
                    binding.emptyState.visibility = View.GONE
                    binding.noResultsState.visibility = View.VISIBLE
                    binding.contentGrid.visibility = View.GONE
                    binding.heroCard.visibility = View.GONE
                } else {
                    binding.emptyState.visibility = View.GONE
                    binding.noResultsState.visibility = View.GONE
                    binding.contentGrid.visibility = View.VISIBLE
                    binding.heroCard.visibility = View.VISIBLE
                    if (normalizedResults.isNotEmpty()) displayModule.updateDetailsPane(normalizedResults[0])
                }
                binding.resultsRecycler.requestFocus()
            }
        }
        viewModel.isSearching.observe(viewLifecycleOwner) { isSearching ->
            binding.progressBar.visibility = if (isSearching) View.VISIBLE else View.GONE
            binding.loadingCard.visibility = if (isSearching) View.VISIBLE else View.GONE
            if (isSearching) binding.noResultsState.visibility = View.GONE
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

        // Observe series meta details for drill-down navigation
        viewModel.metaDetails.observe(viewLifecycleOwner) { meta ->
            if (meta != null && meta.type == "series" && displayModule.currentDrillDownLevel == ResultsDisplayModule.DrillDownLevel.SERIES) {
                // Display seasons in the grid
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

                searchAdapter.updateData(seasons)
                if (seasons.isNotEmpty()) {
                    displayModule.updateDetailsPane(seasons[0])

                    // Auto-focus first season
                    binding.root.postDelayed({
                        if (binding.resultsRecycler.childCount > 0) {
                            val firstView = binding.resultsRecycler.layoutManager?.findViewByPosition(0)
                            firstView?.requestFocus()
                        }
                    }, 100)
                }
            }
        }

        // Observe season episodes for drill-down navigation
        viewModel.seasonEpisodes.observe(viewLifecycleOwner) { episodes ->
            if (displayModule.currentDrillDownLevel == ResultsDisplayModule.DrillDownLevel.SEASON && episodes.isNotEmpty()) {
                searchAdapter.updateData(episodes)
                displayModule.updateDetailsPane(episodes[0])

                // Auto-focus first episode
                binding.root.postDelayed({
                    if (binding.resultsRecycler.childCount > 0) {
                        val firstView = binding.resultsRecycler.layoutManager?.findViewByPosition(0)
                        firstView?.requestFocus()
                    }
                }, 100)
            }
        }
    }

    fun handleBackPress(): Boolean {
        val handled = displayModule.handleBackPress()
        if (handled) {
            // If going back to catalog, reload search results
            if (displayModule.currentDrillDownLevel == ResultsDisplayModule.DrillDownLevel.CATALOG) {
                currentSearchQuery?.let { query ->
                    performSearch(query)
                }
            }
        }
        return handled
    }

    fun setSearchText(text: String) { binding.searchEditText.setText(text) }
    fun getSearchQuery(): String? = currentSearchQuery
    fun searchByPersonId(id: Int) { viewModel.loadPersonCredits(id) }
    private fun hideKeyboard() { (requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).hideSoftInputFromWindow(binding.searchEditText.windowToken, 0) }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        viewModel.clearSearchResults()
        detailsUpdateJob?.cancel()
    }

    fun focusSearch() { binding.searchEditText.requestFocus() }
}