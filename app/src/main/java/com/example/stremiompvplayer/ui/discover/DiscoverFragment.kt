package com.example.stremiompvplayer.ui.discover

import android.content.Intent
import android.os.Bundle
import android.view.ContextThemeWrapper
import android.view.KeyEvent
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
import com.example.stremiompvplayer.ui.ResultsDisplayModule
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

    private lateinit var contentAdapter: PosterAdapter
    private lateinit var displayModule: ResultsDisplayModule
    private var allCatalogs = listOf<UserCatalog>()  // Combined movie + series catalogs
    private var currentCatalogIndex = 0
    private var isShowingGenre = false
    private var isCycling = false  // Prevents double press on DPAD_DOWN
    private var cycleAttemptCount = 0

    private var detailsUpdateJob: Job? = null
    private val focusMemoryManager = FocusMemoryManager.getInstance()
    private val fragmentKey: String = "discover"

    companion object {
        private const val ARG_TYPE = "media_type"
        private const val ARG_SERIES_ID = "series_id"

        fun newInstance(type: String, seriesId: String? = null): DiscoverFragment {
            val fragment = DiscoverFragment()
            val args = Bundle()
            args.putString(ARG_TYPE, type)
            seriesId?.let { args.putString(ARG_SERIES_ID, it) }
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

        // Get the media type from arguments, default to "movie"
        val type = arguments?.getString(ARG_TYPE) ?: "movie"

        // Sync the media type toggle with current content type
        (activity as? MainActivity)?.syncMediaTypeToggle(type)

        setupDisplayModule()
        setupAdapters()
        setupObservers()

        // Check if we should navigate directly to a series
        val seriesId = arguments?.getString(ARG_SERIES_ID)
        if (seriesId != null) {
            // Navigate directly to series drill-down
            displayModule.currentDrillDownLevel = ResultsDisplayModule.DrillDownLevel.SERIES
            displayModule.currentSeriesId = seriesId
            viewModel.loadSeriesMeta(seriesId)
            displayModule.updatePlayButtonVisibility()
        } else {
            loadCatalogs(type)
        }

        setupKeyHandling()
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
            enableDrillDown = true,  // Discover uses drill-down
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

        displayModule.loadGenreList = { genreId, genreName, type ->
            loadGenreList(genreId, genreName, type)
        }
    }

    // Dropdown removed - now browsing both movies and series together

    private fun setupKeyHandling() {
        // Make posterCarousel focusable to receive key events
        binding.posterCarousel.isFocusable = true
        binding.posterCarousel.isFocusableInTouchMode = true

        // Key handling is done in handleKeyDown() method to avoid duplicate event processing
    }

    override fun onResume() {
        super.onResume()
        displayModule.currentSelectedItem?.let { displayModule.updateDetailsPane(it) }

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
                if (allCatalogs.isNotEmpty() && currentCatalogIndex < allCatalogs.size) {
                    val currentCatalog = allCatalogs[currentCatalogIndex]
                    updateCurrentListLabel(currentCatalog.displayName)
                    viewModel.loadContentForCatalog(currentCatalog, isInitialLoad = false)
                }
            }
        }
        return handled
    }
    fun focusSidebar(): Boolean {
        binding.root.post {
            // Focus on Play button or poster carousel
            binding.rvContent.requestFocus()
        }
        return true
    }

    fun performSearch(query: String) {
        if (query.isBlank()) return

        // Reset drill-down state
        displayModule.currentDrillDownLevel = ResultsDisplayModule.DrillDownLevel.CATALOG
        displayModule.currentSeriesId = null
        displayModule.currentSeasonNumber = null

        // Perform dual search (movies + series)
        viewModel.searchTMDB(query)

        // Update label to show search query
        updateCurrentListLabel("Search: $query")
        displayModule.updatePlayButtonVisibility()
    }

    private fun setupAdapters() {
        contentAdapter = PosterAdapter(
            items = emptyList(),
            onClick = { item -> displayModule.onContentClicked(item) },
            onLongClick = { item ->
                val pos = contentAdapter.getItemPosition(item)
                val holder = binding.rvContent.findViewHolderForAdapterPosition(pos)
                if (holder != null) showItemMenu(holder.itemView, item)
            }
        )

        // Horizontal scrolling for Netflix-style poster carousel
        binding.rvContent.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
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

    private fun refreshWatchStatus(item: MetaItem) {
        val userId = SharedPreferencesManager.getInstance(requireContext()).getCurrentUserId()
        if (userId != null) {
            viewModel.checkWatchedStatus(item.id)
        }
    }

    private fun loadCatalogs(type: String = "movie") {
        // Fetch genres for the specified type only
        viewModel.fetchGenres(type)

        // Load catalogs for the specified type only
        viewModel.getDiscoverCatalogs(type).observe(viewLifecycleOwner) { catalogs ->
            // Start with regular catalogs
            val catalogsList = catalogs.toMutableList()

            // Add genre catalog at the end
            if (type == "movie") {
                viewModel.movieGenres.observe(viewLifecycleOwner) { genres ->
                    if (genres.isNotEmpty()) {
                        // Create synthetic catalog for movie genres
                        val genreCatalog = UserCatalog(
                            userId = "",
                            catalogId = "genres_movie",
                            catalogType = "genre",
                            catalogName = "Movie Genres",
                            customName = null,
                            displayOrder = catalogsList.size,
                            pageType = "movie",
                            addonUrl = "",
                            manifestId = "genres_movie"
                        )
                        catalogsList.add(genreCatalog)
                        allCatalogs = catalogsList
                    }
                }
            } else {
                viewModel.tvGenres.observe(viewLifecycleOwner) { genres ->
                    if (genres.isNotEmpty()) {
                        // Create synthetic catalog for series genres
                        val genreCatalog = UserCatalog(
                            userId = "",
                            catalogId = "genres_series",
                            catalogType = "genre",
                            catalogName = "Series Genres",
                            customName = null,
                            displayOrder = catalogsList.size,
                            pageType = "series",
                            addonUrl = "",
                            manifestId = "genres_series"
                        )
                        catalogsList.add(genreCatalog)
                        allCatalogs = catalogsList
                    }
                }
            }

            allCatalogs = catalogsList
            currentCatalogIndex = 0
            if (allCatalogs.isNotEmpty()) {
                updateCurrentListLabel(allCatalogs[0].displayName)
                viewModel.loadContentForCatalog(allCatalogs[0], isInitialLoad = true)
            }
        }
    }

    private fun updateCurrentListLabel(labelText: String) {
        val label = binding.root.findViewById<android.widget.TextView>(R.id.currentListLabel)
        label?.text = labelText
    }

    private fun loadGenreItems(isSeries: Boolean) {
        val genreItems = mutableListOf<MetaItem>()
        val genres = if (isSeries) viewModel.tvGenres.value else viewModel.movieGenres.value
        val mediaType = if (isSeries) "series" else "movie"

        genres?.forEach { genre ->
            genreItems.add(MetaItem(
                id = "genre_${genre.id}_$mediaType",
                type = "genre",
                name = genre.name,
                poster = null,
                background = null,
                description = if (isSeries) "${genre.name} Series" else "${genre.name} Movies",
                genreId = genre.id,
                genreType = mediaType
            ))
        }

        contentAdapter.updateData(genreItems)
        if (genreItems.isNotEmpty()) {
            displayModule.updateDetailsPane(genreItems[0])
        }
    }

    private fun cycleToNextList() {
        if (allCatalogs.isEmpty() || isCycling) return

        // Prevent infinite loops - if we've tried all catalogs twice, stop
        // (twice because the first round might not have loaded yet)
        if (cycleAttemptCount >= allCatalogs.size * 2) {
            cycleAttemptCount = 0
            isCycling = false
            // Clear selected item when no content is available
            displayModule.currentSelectedItem = null
            return
        }

        isCycling = true
        cycleAttemptCount++

        // Reset drill-down state when cycling lists
        displayModule.currentDrillDownLevel = ResultsDisplayModule.DrillDownLevel.CATALOG
        displayModule.currentSeriesId = null
        displayModule.currentSeasonNumber = null

        currentCatalogIndex = (currentCatalogIndex + 1) % allCatalogs.size
        val nextCatalog = allCatalogs[currentCatalogIndex]
        updateCurrentListLabel(nextCatalog.displayName)

        // Check if this is a genre catalog
        if (nextCatalog.catalogType == "genre") {
            // Load genre items instead of regular catalog content
            loadGenreItems(nextCatalog.catalogId.endsWith("series"))
        } else {
            viewModel.loadContentForCatalog(nextCatalog, isInitialLoad = true)
        }

        isShowingGenre = false
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
            } else {
                // Reset isCycling even if empty, so the observer can trigger another cycle
                isCycling = false
            }
            // If empty, the observer will trigger another cycle
        }, 1000)
    }

    private fun loadGenreList(genreId: Int, genreName: String, type: String = "movie") {
        // Reset drill-down state when loading genre
        displayModule.currentDrillDownLevel = ResultsDisplayModule.DrillDownLevel.CATALOG
        displayModule.currentSeriesId = null
        displayModule.currentSeasonNumber = null

        isShowingGenre = true
        updateCurrentListLabel("Genre: $genreName")

        // Fetch popular movies/series for this genre
        lifecycleScope.launch {
            try {
                val content = viewModel.fetchPopularByGenre(type, genreId)
                contentAdapter.updateData(content)
                if (content.isNotEmpty()) {
                    displayModule.updateDetailsPane(content[0])
                }
                displayModule.updatePlayButtonVisibility()

                // [FIX] Focus first item after loading genre
                binding.root.postDelayed({
                    binding.rvContent.scrollToPosition(0)
                    val firstView = binding.rvContent.layoutManager?.findViewByPosition(0)
                    firstView?.requestFocus()
                }, 1000)

            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error loading genre content", Toast.LENGTH_SHORT).show()
            }
        }
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
            popup.menu.add("Clear Progress")
            popup.menu.add("Not Watching")

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
                    "Clear Progress" -> {
                        viewModel.clearWatchedStatus(item)
                        item.isWatched = false
                        item.progress = 0
                        refreshItem(item)
                        true
                    }
                    "Not Watching" -> {
                        viewModel.markAsNotWatching(item)
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


    private fun showGenreSelectionDialog() {
        // Show combined movie and series genres
        val movieGenres = viewModel.movieGenres.value ?: emptyList()
        val tvGenres = viewModel.tvGenres.value ?: emptyList()

        if (movieGenres.isEmpty() && tvGenres.isEmpty()) {
            Toast.makeText(requireContext(), "Loading genres...", Toast.LENGTH_SHORT).show()
            return
        }

        val popupMenu = PopupMenu(requireContext(), binding.rvContent)

        // Add movie genres
        if (movieGenres.isNotEmpty()) {
            popupMenu.menu.add(0, -1, 0, "--- Movies ---").isEnabled = false
            movieGenres.forEach { genre ->
                popupMenu.menu.add(0, genre.id, 0, genre.name)
            }
        }

        // Add series genres
        if (tvGenres.isNotEmpty()) {
            popupMenu.menu.add(1, -1, 0, "--- Series ---").isEnabled = false
            tvGenres.forEach { genre ->
                popupMenu.menu.add(1, genre.id + 10000, 0, genre.name)  // Offset ID to distinguish
            }
        }

        popupMenu.setOnMenuItemClickListener { menuItem ->
            if (menuItem.itemId == -1) return@setOnMenuItemClickListener false

            // Check if it's a movie or series genre
            if (menuItem.itemId >= 10000) {
                // Series genre
                val genreId = menuItem.itemId - 10000
                val selectedGenre = tvGenres.find { it.id == genreId }
                selectedGenre?.let {
                    loadGenreList(it.id, it.name, "series")
                }
            } else {
                // Movie genre
                val selectedGenre = movieGenres.find { it.id == menuItem.itemId }
                selectedGenre?.let {
                    loadGenreList(it.id, it.name, "movie")
                }
            }
            true
        }

        popupMenu.show()
    }

    private fun setupObservers() {
        viewModel.currentCatalogContent.observe(viewLifecycleOwner) { items ->
            // Only update content if we're at catalog level (not drilled down)
            if (displayModule.currentDrillDownLevel == ResultsDisplayModule.DrillDownLevel.CATALOG) {
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
                    // If the list is empty, automatically cycle to the next list (even when not actively cycling)
                    // This effectively hides empty lists from the discover page
                    if (cycleAttemptCount < allCatalogs.size) {
                        binding.root.postDelayed({
                            cycleToNextList()
                        }, 200)  // Shorter delay for smoother experience
                    }
                }
                displayModule.updatePlayButtonVisibility()
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

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        viewModel.isItemWatched.observe(viewLifecycleOwner) { isWatched ->
            displayModule.currentSelectedItem?.let { item ->
                item.isWatched = isWatched
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

        // Observe search results for actor/person content and search
        viewModel.searchResults.observe(viewLifecycleOwner) { results ->
            if (results.isNotEmpty() && displayModule.currentDrillDownLevel == ResultsDisplayModule.DrillDownLevel.CATALOG) {
                contentAdapter.updateData(results)
                displayModule.updateDetailsPane(results[0])

                // Focus first item
                binding.root.postDelayed({
                    binding.rvContent.scrollToPosition(0)
                    binding.rvContent.layoutManager?.findViewByPosition(0)?.requestFocus()
                }, 1000)
            }
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        detailsUpdateJob?.cancel()
    }

}