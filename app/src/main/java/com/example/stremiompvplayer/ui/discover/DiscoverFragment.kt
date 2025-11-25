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
    private var currentType = "movie"
    private var currentSelectedItem: MetaItem? = null
    private var currentCatalogs = listOf<UserCatalog>()
    private var currentCatalogIndex = 0
    private var isShowingGenre = false

    private var detailsUpdateJob: Job? = null
    private val focusMemoryManager = FocusMemoryManager.getInstance()
    private val fragmentKey: String
        get() = focusMemoryManager.getFragmentKey("discover", currentType)

    // Drill-down navigation state
    private enum class DrillDownLevel { CATALOG, SERIES, SEASON }
    private var currentDrillDownLevel = DrillDownLevel.CATALOG
    private var currentSeriesId: String? = null
    private var currentSeasonNumber: Int? = null

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
        setupKeyHandling()
    }

    private fun setupKeyHandling() {
        // Make posterCarousel focusable to receive key events
        binding.posterCarousel.isFocusable = true
        binding.posterCarousel.isFocusableInTouchMode = true

        // Set unhandled key event listener on the fragment root to catch down/up before RecyclerView consumes them
        binding.root.setOnKeyListener { _, keyCode, event ->
            if (event.action == KeyEvent.ACTION_DOWN) {
                when (keyCode) {
                    KeyEvent.KEYCODE_DPAD_DOWN -> {
                        // Check if a poster item has focus in the carousel
                        val focusedChild = binding.rvContent.focusedChild
                        if (focusedChild != null) {
                            // Cycle to next list when down is pressed on carousel
                            cycleToNextList()
                            return@setOnKeyListener true
                        }
                        false
                    }
                    KeyEvent.KEYCODE_DPAD_UP -> {
                        // Check if a poster item has focus
                        val focusedChild = binding.rvContent.focusedChild
                        if (focusedChild != null) {
                            // Focus on Play button when up is pressed on carousel
                            binding.root.findViewById<View>(R.id.btnPlay)?.requestFocus()
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

    fun handleBackPress(): Boolean {
        // Handle drill-down navigation back
        when (currentDrillDownLevel) {
            DrillDownLevel.SEASON -> {
                // Go back to seasons view
                currentSeriesId?.let { seriesId ->
                    currentDrillDownLevel = DrillDownLevel.SERIES
                    currentSeasonNumber = null
                    viewModel.loadSeriesMeta(seriesId)
                    updatePlayButtonVisibility()
                    return true
                }
            }
            DrillDownLevel.SERIES -> {
                // Go back to catalog view
                currentDrillDownLevel = DrillDownLevel.CATALOG
                currentSeriesId = null
                currentSeasonNumber = null
                // Reload the current catalog
                if (currentCatalogs.isNotEmpty() && currentCatalogIndex < currentCatalogs.size) {
                    val currentCatalog = currentCatalogs[currentCatalogIndex]
                    updateCurrentListLabel(currentCatalog.displayName)
                    viewModel.loadContentForCatalog(currentCatalog, isInitialLoad = false)
                }
                updatePlayButtonVisibility()
                return true
            }
            DrillDownLevel.CATALOG -> {
                // At top level, don't consume back press
                return false
            }
        }
        return false
    }
    fun focusSidebar(): Boolean {
        binding.root.post {
            // Focus on Play button or poster carousel
            binding.root.findViewById<View>(R.id.btnPlay)?.requestFocus()
                ?: binding.rvContent.requestFocus()
        }
        return true
    }

    fun performSearch(query: String) {
        if (query.isBlank()) return

        // Reset drill-down state
        currentDrillDownLevel = DrillDownLevel.CATALOG
        currentSeriesId = null
        currentSeasonNumber = null

        // Perform search based on current media type
        when (currentType) {
            "movie" -> viewModel.searchMovies(query)
            "series" -> viewModel.searchSeries(query)
            else -> viewModel.searchTMDB(query)
        }

        // Update label to show search query
        updateCurrentListLabel("Search: $query")
        updatePlayButtonVisibility()
    }

    private fun setupAdapters() {
        contentAdapter = PosterAdapter(
            items = emptyList(),
            onClick = { item -> onContentClicked(item) },
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

        // Setup Play button
        binding.root.findViewById<View>(R.id.btnPlay)?.setOnClickListener {
            currentSelectedItem?.let { item ->
                // For episodes, we need to load streams using the episode ID format
                if (item.type == "episode") {
                    showStreamDialog(item)
                } else {
                    showStreamDialog(item)
                }
            }
        }

        // Setup Trailer button
        binding.root.findViewById<View>(R.id.btnTrailer)?.setOnClickListener {
            currentSelectedItem?.let { item ->
                playTrailer(item)
            }
        }
    }

    private fun updateDetailsPane(item: MetaItem) {
        currentSelectedItem = item

        Glide.with(this)
            .load(item.background ?: item.poster)
            .into(binding.pageBackground)

        val formattedDate = try {
            item.releaseDate?.let { dateStr ->
                val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val outputFormat = SimpleDateFormat("yyyy", Locale.getDefault())
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

        // Update actor chips
        updateActorChips(item)

        refreshWatchStatus(item)
    }

    private fun updateActorChips(item: MetaItem) {
        val actorChipGroup = binding.root.findViewById<com.google.android.material.chip.ChipGroup>(R.id.actorChips)
        actorChipGroup?.removeAllViews()

        // Fetch cast information from TMDB
        viewModel.fetchCast(item.id, item.type)
    }

    private fun updatePlayButtonVisibility() {
        val playButton = binding.root.findViewById<View>(R.id.btnPlay)
        val shouldShowPlay = when {
            // Always show for movies at catalog level
            currentDrillDownLevel == DrillDownLevel.CATALOG && currentType == "movie" -> true
            // Show for episodes (when drilled down to season level)
            currentDrillDownLevel == DrillDownLevel.SEASON -> true
            // Hide for series and seasons (not yet at playable level)
            else -> false
        }
        playButton?.visibility = if (shouldShowPlay) View.VISIBLE else View.GONE
    }

    private fun showStreamDialog(item: MetaItem) {
        // Create dialog
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_stream_selection, null)
        val dialog = android.app.AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        // Get views from dialog
        val rvStreams = dialogView.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.rvStreams)
        val progressBar = dialogView.findViewById<android.widget.ProgressBar>(R.id.progressBar)
        val btnCancel = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnCancel)
        val dialogTitle = dialogView.findViewById<android.widget.TextView>(R.id.dialogTitle)

        dialogTitle.text = "Select Stream - ${item.name}"

        // Setup RecyclerView
        val streamAdapter = com.example.stremiompvplayer.adapters.StreamAdapter { stream ->
            dialog.dismiss()
            playStream(stream)
        }
        rvStreams.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(requireContext())
        rvStreams.adapter = streamAdapter

        // Setup cancel button
        btnCancel.setOnClickListener { dialog.dismiss() }

        // Load streams
        progressBar.visibility = View.VISIBLE
        rvStreams.visibility = View.GONE

        viewModel.loadStreams(item.type, item.id)

        // Observe streams
        val streamObserver = androidx.lifecycle.Observer<List<com.example.stremiompvplayer.models.Stream>> { streams ->
            progressBar.visibility = View.GONE
            rvStreams.visibility = View.VISIBLE
            if (streams.isEmpty()) {
                Toast.makeText(requireContext(), "No streams available", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            } else {
                streamAdapter.submitList(streams)
            }
        }
        viewModel.streams.observe(viewLifecycleOwner, streamObserver)

        dialog.setOnDismissListener {
            viewModel.streams.removeObserver(streamObserver)
        }

        dialog.show()
    }

    private fun playStream(stream: com.example.stremiompvplayer.models.Stream) {
        val intent = Intent(requireContext(), com.example.stremiompvplayer.PlayerActivity::class.java).apply {
            putExtra("stream", stream)
            putExtra("title", currentSelectedItem?.name ?: "Unknown")
            putExtra("metaId", currentSelectedItem?.id)
        }
        startActivity(intent)
    }

    private fun playTrailer(item: MetaItem) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val trailerUrl = viewModel.fetchTrailer(item.id, item.type)
                if (trailerUrl != null) {
                    // Open YouTube trailer in browser or YouTube app
                    val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(trailerUrl))
                    startActivity(intent)
                } else {
                    Toast.makeText(requireContext(), "No trailer available", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error loading trailer", Toast.LENGTH_SHORT).show()
            }
        }
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
            currentCatalogs = catalogs
            currentCatalogIndex = 0
            if (catalogs.isNotEmpty()) {
                updateCurrentListLabel(catalogs[0].displayName)
                viewModel.loadContentForCatalog(catalogs[0], isInitialLoad = true)
            }
        }
    }

    private fun updateCurrentListLabel(labelText: String) {
        val topBar = activity?.findViewById<View>(R.id.netflixTopBar)
        val label = topBar?.findViewById<android.widget.TextView>(R.id.currentListLabel)
        label?.text = labelText
    }

    private fun cycleToNextList() {
        if (currentCatalogs.isEmpty()) return

        // Reset drill-down state when cycling lists
        currentDrillDownLevel = DrillDownLevel.CATALOG
        currentSeriesId = null
        currentSeasonNumber = null

        currentCatalogIndex = (currentCatalogIndex + 1) % currentCatalogs.size
        val nextCatalog = currentCatalogs[currentCatalogIndex]
        updateCurrentListLabel(nextCatalog.displayName)
        viewModel.loadContentForCatalog(nextCatalog, isInitialLoad = true)
        isShowingGenre = false
        updatePlayButtonVisibility()
    }

    private fun loadGenreList(genreId: Int, genreName: String) {
        // Reset drill-down state when loading genre
        currentDrillDownLevel = DrillDownLevel.CATALOG
        currentSeriesId = null
        currentSeasonNumber = null

        isShowingGenre = true
        updateCurrentListLabel("Genre: $genreName")

        // Fetch popular movies/series for this genre
        lifecycleScope.launch {
            try {
                val content = viewModel.fetchPopularByGenre(currentType, genreId)
                contentAdapter.updateData(content)
                if (content.isNotEmpty()) {
                    updateDetailsPane(content[0])
                }
                updatePlayButtonVisibility()
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
            popup.menu.add("Clear Watched Status")
            popup.menu.add("View Related")

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
                    "View Related" -> {
                        // Reset drill-down state
                        currentDrillDownLevel = DrillDownLevel.CATALOG
                        currentSeriesId = null
                        currentSeasonNumber = null

                        // Load similar content in Discover layout
                        viewLifecycleOwner.lifecycleScope.launch {
                            try {
                                val similarContent = viewModel.fetchSimilarContent(item.id, item.type)
                                if (similarContent.isNotEmpty()) {
                                    contentAdapter.updateData(similarContent)
                                    updateDetailsPane(similarContent[0])
                                    updateCurrentListLabel("Related to ${item.name}")
                                    updatePlayButtonVisibility()
                                } else {
                                    Toast.makeText(requireContext(), "No related content found", Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {
                                Toast.makeText(requireContext(), "Error loading related content", Toast.LENGTH_SHORT).show()
                            }
                        }
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
        // Check if this is the genre browser item
        if (item.type == "genre_browser") {
            showGenreSelectionDialog()
            return
        }

        updateDetailsPane(item)

        when (item.type) {
            "series" -> {
                // Drill down into series to show seasons
                currentDrillDownLevel = DrillDownLevel.SERIES
                currentSeriesId = item.id
                currentSeasonNumber = null
                viewModel.loadSeriesMeta(item.id)
                updatePlayButtonVisibility()
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
                    viewModel.loadSeasonEpisodes(seriesId, seasonNum)
                    updatePlayButtonVisibility()
                }
            }
            "episode" -> {
                // Show stream selection dialog for episode
                showStreamDialog(item)
            }
            "movie" -> {
                // Show stream selection dialog for movie
                showStreamDialog(item)
            }
            else -> {
                // Fallback - show stream dialog if possible
                showStreamDialog(item)
            }
        }
    }

    private fun showGenreSelectionDialog() {
        val genres = if (currentType == "movie") viewModel.movieGenres.value else viewModel.tvGenres.value
        if (genres == null || genres.isEmpty()) {
            Toast.makeText(requireContext(), "Loading genres...", Toast.LENGTH_SHORT).show()
            return
        }

        val popupMenu = PopupMenu(requireContext(), binding.rvContent)
        genres.forEach { genre ->
            popupMenu.menu.add(genre.name)
        }

        popupMenu.setOnMenuItemClickListener { menuItem ->
            val selectedGenre = genres.find { it.name == menuItem.title.toString() }
            selectedGenre?.let {
                loadGenreList(it.id, it.name)
            }
            true
        }

        popupMenu.show()
    }

    private fun setupObservers() {
        viewModel.currentCatalogContent.observe(viewLifecycleOwner) { items ->
            // Only update content if we're at catalog level (not drilled down)
            if (currentDrillDownLevel == DrillDownLevel.CATALOG) {
                // Add genre selector as the last item in the carousel
                val genres = if (currentType == "movie") viewModel.movieGenres.value else viewModel.tvGenres.value
                val itemsWithGenres = if (genres != null && genres.isNotEmpty() && !isShowingGenre) {
                    // Create a special "Browse Genres" item
                    val genreBrowserItem = MetaItem(
                        id = "genre_browser",
                        type = "genre_browser",
                        name = "Browse by Genre",
                        poster = null,
                        background = null,
                        description = "Select a genre to explore"
                    )
                    items + genreBrowserItem
                } else {
                    items
                }

                contentAdapter.updateData(itemsWithGenres)

                if (itemsWithGenres.isNotEmpty()) {
                    updateDetailsPane(itemsWithGenres[0])
                }

                if (itemsWithGenres.isEmpty()) {
                    currentSelectedItem = null
                }
                updatePlayButtonVisibility()
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

        // Observe cast list and update actor chips
        viewModel.castList.observe(viewLifecycleOwner) { castList ->
            val actorChipGroup = binding.root.findViewById<com.google.android.material.chip.ChipGroup>(R.id.actorChips)
            actorChipGroup?.removeAllViews()

            castList.take(5).forEach { actor ->
                val chip = com.google.android.material.chip.Chip(requireContext())
                chip.text = actor.name
                chip.isClickable = true
                chip.isFocusable = true
                chip.setChipBackgroundColorResource(R.color.md_theme_surfaceContainer)
                chip.setTextColor(resources.getColor(R.color.text_primary, null))

                // Show actor's content in Discover layout
                chip.setOnClickListener {
                    val personId = actor.id.removePrefix("tmdb:").toIntOrNull()
                    if (personId != null) {
                        // Reset drill-down state
                        currentDrillDownLevel = DrillDownLevel.CATALOG
                        currentSeriesId = null
                        currentSeasonNumber = null

                        // Load person's credits (movies/shows they appeared in)
                        viewModel.loadPersonCredits(personId)
                        updateCurrentListLabel("${actor.name} - Filmography")
                        updatePlayButtonVisibility()
                    }
                }

                actorChipGroup?.addView(chip)
            }
        }

        // Observe series meta details for drill-down navigation
        viewModel.metaDetails.observe(viewLifecycleOwner) { meta ->
            if (meta != null && meta.type == "series" && currentDrillDownLevel == DrillDownLevel.SERIES) {
                // Display seasons in the carousel
                val seasons = meta.videos?.mapNotNull { video ->
                    video.season?.let { seasonNum ->
                        MetaItem(
                            id = "${meta.id}:$seasonNum",
                            type = "season",
                            name = video.title,
                            poster = video.thumbnail,
                            background = meta.background,
                            description = "Season $seasonNum"
                        )
                    }
                } ?: emptyList()

                contentAdapter.updateData(seasons)
                if (seasons.isNotEmpty()) {
                    updateDetailsPane(seasons[0])
                }
                updateCurrentListLabel("${meta.name} - Seasons")
            }
        }

        // Observe season episodes for drill-down navigation
        viewModel.seasonEpisodes.observe(viewLifecycleOwner) { episodes ->
            if (currentDrillDownLevel == DrillDownLevel.SEASON && episodes.isNotEmpty()) {
                contentAdapter.updateData(episodes)
                updateDetailsPane(episodes[0])
                currentSeriesId?.let { seriesId ->
                    currentSeasonNumber?.let { seasonNum ->
                        updateCurrentListLabel("Season $seasonNum - Episodes")
                    }
                }
            }
        }

        // Observe search results for actor/person content and search
        viewModel.searchResults.observe(viewLifecycleOwner) { results ->
            if (results.isNotEmpty() && currentDrillDownLevel == DrillDownLevel.CATALOG) {
                contentAdapter.updateData(results)
                updateDetailsPane(results[0])
            }
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        detailsUpdateJob?.cancel()
    }

}