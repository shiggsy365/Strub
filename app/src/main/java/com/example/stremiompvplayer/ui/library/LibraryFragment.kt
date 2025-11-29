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
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.stremiompvplayer.R
import com.example.stremiompvplayer.UserSelectionActivity
import com.example.stremiompvplayer.data.ServiceLocator
import com.example.stremiompvplayer.databinding.FragmentLibraryNewBinding
import com.example.stremiompvplayer.models.MetaItem
import com.example.stremiompvplayer.utils.SharedPreferencesManager
import com.example.stremiompvplayer.viewmodels.MainViewModel
import com.example.stremiompvplayer.viewmodels.MainViewModelFactory
import com.example.stremiompvplayer.adapters.PosterAdapter
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

class LibraryFragment : Fragment() {

    private var _binding: FragmentLibraryNewBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MainViewModel by activityViewModels {
        MainViewModelFactory(
            ServiceLocator.getInstance(requireContext()),
            SharedPreferencesManager.getInstance(requireContext())
        )
    }

    private lateinit var contentAdapter: PosterAdapter
    private var currentSelectedItem: MetaItem? = null
    private var allLibraryItems = listOf<MetaItem>()  // Combined movie + series library
    private var currentCatalogIndex = 0
    private var currentCatalogs = listOf<Pair<String, List<MetaItem>>>()  // (label, items) pairs

    private var detailsUpdateJob: Job? = null

    // Sorting and filtering state
    private var currentSortBy = "dateAdded"
    private var currentSortAscending = false
    private var currentGenreFilter: String? = null

    // Drill-down navigation state (like DiscoverFragment)
    private enum class DrillDownLevel { CATALOG, SERIES, SEASON }
    private var currentDrillDownLevel = DrillDownLevel.CATALOG
    private var currentSeriesId: String? = null
    private var currentSeasonNumber: Int? = null

    companion object {
        private const val ARG_TYPE = "media_type"
        fun newInstance(type: String): LibraryFragment {
            val fragment = LibraryFragment()
            val args = Bundle()
            args.putString(ARG_TYPE, type)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentLibraryNewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Get the media type from arguments, default to "movie"
        val type = arguments?.getString(ARG_TYPE) ?: "movie"


        setupAdapters()
        setupObservers()
        setupKeyHandling()

        loadLibrary(type)
    }

    override fun onResume() {
        super.onResume()
        currentSelectedItem?.let { updateDetailsPane(it) }
    }

    // Dropdown removed - now browsing both movies and series together

    private fun setupKeyHandling() {
        // Make posterCarousel focusable to receive key events
        binding.posterCarousel.isFocusable = true
        binding.posterCarousel.isFocusableInTouchMode = true

        // Set key listener for up/down navigation
        binding.root.setOnKeyListener { _, keyCode, event ->
            if (event.action == android.view.KeyEvent.ACTION_DOWN) {
                when (keyCode) {
                    android.view.KeyEvent.KEYCODE_DPAD_DOWN -> {
                        val focusedChild = binding.rvContent.focusedChild
                        if (focusedChild != null) {
                            cycleToNextList()
                            return@setOnKeyListener true
                        }
                        false
                    }
                    android.view.KeyEvent.KEYCODE_DPAD_UP -> {
                        val focusedChild = binding.rvContent.focusedChild
                        if (focusedChild != null) {
                            // Focus on Play button if available, otherwise Related button
                            val btnPlay = binding.root.findViewById<View>(R.id.btnPlay)
                            val btnRelated = binding.root.findViewById<View>(R.id.btnRelated)
                            when {
                                btnPlay?.visibility == View.VISIBLE -> btnPlay.requestFocus()
                                btnRelated?.visibility == View.VISIBLE -> btnRelated.requestFocus()
                                else -> btnPlay?.requestFocus() // Fallback to play even if hidden
                            }
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
                            val item = contentAdapter.getItem(position)
                            if (item != null) {
                                detailsUpdateJob?.cancel()
                                detailsUpdateJob = viewLifecycleOwner.lifecycleScope.launch {
                                    delay(300) // PERFORMANCE: Reduced delay for snappier UX
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
                showStreamDialog(item)
            }
        }

        // Setup Trailer button
        binding.root.findViewById<View>(R.id.btnTrailer)?.setOnClickListener {
            currentSelectedItem?.let { item ->
                playTrailer(item)
            }
        }

        // Setup Related button
        binding.root.findViewById<View>(R.id.btnRelated)?.setOnClickListener {
            currentSelectedItem?.let { item ->
                showRelatedContent(item)
            }
        }

        // Setup Sort/Filter button
        binding.root.findViewById<View>(R.id.sortFilterButton)?.setOnClickListener { view ->
            showSortFilterMenu(view)
        }
    }

    private fun loadLibrary(type: String = "movie") {
        // Load library for the specified type only
        viewModel.filterAndSortLibrary(type)
    }

    private fun cycleToNextList() {
        if (currentCatalogs.isEmpty()) return
        currentCatalogIndex = (currentCatalogIndex + 1) % currentCatalogs.size
        val (label, items) = currentCatalogs[currentCatalogIndex]
        updateCurrentListLabel(label)
        contentAdapter.updateData(items)
        if (items.isNotEmpty()) {
            updateDetailsPane(items[0])
        }
        // [FIX] Force focus to the first item when cycling lists
        binding.root.postDelayed({
            binding.rvContent.scrollToPosition(0)
            binding.rvContent.post {
                val firstView = binding.rvContent.layoutManager?.findViewByPosition(0)
                firstView?.requestFocus()
            }
        }, 200)
    }

    private fun updateCurrentListLabel(labelText: String) {
        val label = binding.root.findViewById<android.widget.TextView>(R.id.currentListLabel)
        label?.text = labelText
    }

    private fun updateItemUI(item: MetaItem, isWatched: Boolean) {
        item.isWatched = isWatched
        item.progress = if (isWatched) item.duration else 0

        val position = contentAdapter.getItemPosition(item)
        if (position != -1) {
            contentAdapter.notifyItemChanged(position)
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
                val outputFormat = SimpleDateFormat("dd MMM yy", Locale.getDefault())
                val date = inputFormat.parse(dateStr)
                date?.let { outputFormat.format(it) }
            }
        } catch (e: Exception) {
            item.releaseDate
        }

        binding.detailDate.text = formattedDate ?: ""
        binding.detailDate.visibility = if (formattedDate.isNullOrEmpty()) View.GONE else View.VISIBLE
        binding.detailRating.visibility = if (item.rating != null) {
            binding.detailRating.text = "★ ${item.rating}"
            View.VISIBLE
        } else {
            View.GONE
        }

        binding.detailDescription.text = item.description ?: "No description available."

        // Episode Info (Specific to episodes)
        if (item.type == "episode") {
            val parts = item.id.split(":")
            if (parts.size >= 4) {
                val season = parts[2]
                val episode = parts[3]
                binding.detailEpisode.text = "S${season.padStart(2, '0')}E${episode.padStart(2, '0')}"
                binding.detailEpisode.visibility = View.VISIBLE
            } else {
                binding.detailEpisode.visibility = View.GONE
            }
        } else {
            binding.detailEpisode.visibility = View.GONE
        }

        binding.detailTitle.text = item.name
        binding.detailTitle.visibility = View.GONE
        binding.detailLogo.visibility = View.GONE

        viewModel.fetchItemLogo(item)

        // Update actor chips
        updateActorChips(item)
    }

    private fun showItemMenu(view: View, item: MetaItem) {
        val wrapper = ContextThemeWrapper(requireContext(),
            android.R.style.Theme_DeviceDefault_Light_NoActionBar)
        val popup = PopupMenu(wrapper, view)

        // Use lifecycleScope to get library status synchronously before showing menu
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

    private fun refreshItem(item: MetaItem) {
        val position = contentAdapter.getItemPosition(item)
        if (position != -1) {
            contentAdapter.notifyItemChanged(position)
        }
    }

    private fun onContentClicked(item: MetaItem) {
        updateDetailsPane(item)

        when (item.type) {
            "series" -> {
                // Drill down into series to show seasons (like DiscoverFragment)
                currentDrillDownLevel = DrillDownLevel.SERIES
                currentSeriesId = item.id
                currentSeasonNumber = null
                viewModel.loadSeriesMeta(item.id)
                updatePlayButtonVisibility()

                // Focus first item of seasons list
                binding.root.postDelayed({
                    binding.rvContent.scrollToPosition(0)
                    binding.rvContent.layoutManager?.findViewByPosition(0)?.requestFocus()
                }, 1000)
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

                    // Focus first item of episodes list
                    binding.root.postDelayed({
                        binding.rvContent.scrollToPosition(0)
                        binding.rvContent.layoutManager?.findViewByPosition(0)?.requestFocus()
                    }, 1000)
                }
            }
            "episode" -> {
                // Focus play button for playable episode
                binding.root.findViewById<View>(R.id.btnPlay)?.requestFocus()
            }
            "movie" -> {
                // Focus play button for playable movie
                binding.root.findViewById<View>(R.id.btnPlay)?.requestFocus()
            }
            else -> {
                // Unknown type - do nothing
            }
        }
    }

    private fun updatePlayButtonVisibility() {
        val btnPlay = binding.root.findViewById<View>(R.id.btnPlay)
        when (currentDrillDownLevel) {
            DrillDownLevel.CATALOG -> {
                // At catalog level, show play for movies, hide for series
                val type = arguments?.getString(ARG_TYPE) ?: "movie"
                btnPlay?.visibility = if (type == "movie") View.VISIBLE else View.GONE
            }
            DrillDownLevel.SERIES -> {
                // At series level (showing seasons), hide play button
                btnPlay?.visibility = View.GONE
            }
            DrillDownLevel.SEASON -> {
                // At season level (showing episodes), show play button
                btnPlay?.visibility = View.VISIBLE
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
                // Go back to library view
                currentDrillDownLevel = DrillDownLevel.CATALOG
                currentSeriesId = null
                currentSeasonNumber = null
                // Reload the library
                val type = arguments?.getString(ARG_TYPE) ?: "movie"
                loadLibrary(type)
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

    private fun showStreamDialog(item: MetaItem) {
        // Clear any previous streams BEFORE creating the dialog to prevent stale data
        viewModel.clearStreams()

        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_stream_selection, null)
        val dialog = android.app.AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        val rvStreams = dialogView.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.rvStreams)
        val progressBar = dialogView.findViewById<android.widget.ProgressBar>(R.id.progressBar)
        val btnCancel = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnCancel)
        val dialogTitle = dialogView.findViewById<android.widget.TextView>(R.id.dialogTitle)

        dialogTitle.text = "Select Stream - ${item.name}"

        val streamAdapter = com.example.stremiompvplayer.adapters.StreamAdapter { stream ->
            dialog.dismiss()
            viewModel.clearStreams()
            playStream(stream)
        }
        rvStreams.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(requireContext())
        rvStreams.adapter = streamAdapter

        btnCancel.setOnClickListener {
            dialog.dismiss()
            viewModel.clearStreams()
        }

        // Show loading state
        progressBar.visibility = View.VISIBLE
        rvStreams.visibility = View.GONE

        // Observe streams BEFORE loading to ensure we catch the update
        var hasReceivedUpdate = false
        val streamObserver = androidx.lifecycle.Observer<List<com.example.stremiompvplayer.models.Stream>> { streams ->
            // Only process if this is an update after loadStreams() call
            if (hasReceivedUpdate || streams.isNotEmpty()) {
                progressBar.visibility = View.GONE
                rvStreams.visibility = View.VISIBLE
                if (streams.isEmpty()) {
                    Toast.makeText(requireContext(), "No streams available", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                } else {
                    streamAdapter.submitList(streams)
                    // Focus first item after list is populated
                    rvStreams.post {
                        rvStreams.layoutManager?.findViewByPosition(0)?.requestFocus()
                    }
                }
            }
            hasReceivedUpdate = true
        }
        viewModel.streams.observe(viewLifecycleOwner, streamObserver)

        dialog.setOnDismissListener {
            viewModel.streams.removeObserver(streamObserver)
            viewModel.clearStreams()
        }

        dialog.show()

        // Load streams AFTER setting up observer
        viewModel.loadStreams(item.type, item.id)
    }

    private fun playStream(stream: com.example.stremiompvplayer.models.Stream) {
        val intent = Intent(requireContext(), com.example.stremiompvplayer.PlayerActivity::class.java).apply {
            putExtra("stream", stream)
            putExtra("meta", currentSelectedItem)  // Pass full MetaItem for subtitles
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

    private fun showRelatedContent(item: MetaItem) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                viewModel.fetchSimilarContent(item.id, item.type)
                Toast.makeText(requireContext(), "Loading related content...", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error loading related content", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateActorChips(item: MetaItem) {
        val actorChipGroup = binding.root.findViewById<com.google.android.material.chip.ChipGroup>(R.id.actorChips)
        actorChipGroup?.removeAllViews()

        // Fetch cast information from TMDB
        viewModel.fetchCast(item.id, item.type)
    }

    private fun setupObservers() {
        viewModel.libraryMovies.observe(viewLifecycleOwner, androidx.lifecycle.Observer {})
        viewModel.librarySeries.observe(viewLifecycleOwner, androidx.lifecycle.Observer {})

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

        // Get the media type from arguments
        val selectedType = arguments?.getString(ARG_TYPE) ?: "movie"

        // Observe the appropriate library based on selected type
        if (selectedType == "movie") {
            viewModel.filteredLibraryMovies.observe(viewLifecycleOwner) { movieItems ->
                // Create catalog for movies only
                currentCatalogs = listOf(Pair("My Movies", movieItems))

                // Show the movie catalog
                currentCatalogIndex = 0
                val (label, items) = currentCatalogs[0]
                updateCurrentListLabel(label)
                contentAdapter.updateData(items)

                if (items.isNotEmpty()) {
                    binding.emptyText.visibility = View.GONE
                    binding.rvContent.visibility = View.VISIBLE
                    updateDetailsPane(items[0])
                } else {
                    binding.emptyText.visibility = View.VISIBLE
                    binding.rvContent.visibility = View.GONE
                }
            }
        } else {
            viewModel.filteredLibrarySeries.observe(viewLifecycleOwner) { seriesItems ->
                // Create catalog for series only
                currentCatalogs = listOf(Pair("My Series", seriesItems))

                // Show the series catalog
                currentCatalogIndex = 0
                val (label, items) = currentCatalogs[0]
                updateCurrentListLabel(label)
                contentAdapter.updateData(items)

                if (items.isNotEmpty()) {
                    binding.emptyText.visibility = View.GONE
                    binding.rvContent.visibility = View.VISIBLE
                    updateDetailsPane(items[0])
                } else {
                    binding.emptyText.visibility = View.VISIBLE
                    binding.rvContent.visibility = View.GONE
                }
            }
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        // Observe cast loading state
        viewModel.isCastLoading.observe(viewLifecycleOwner) { isLoading ->
            if (isLoading) {
                val actorChipGroup = binding.root.findViewById<com.google.android.material.chip.ChipGroup>(R.id.actorChips)
                actorChipGroup?.removeAllViews()
                val placeholderChip = com.google.android.material.chip.Chip(requireContext()).apply {
                    text = "No Cast Returned"
                    isClickable = false
                    isFocusable = false
                    setChipBackgroundColorResource(R.color.md_theme_surfaceContainer)
                    setTextColor(resources.getColor(R.color.text_primary, null))
                }
                actorChipGroup?.addView(placeholderChip)
            }
        }

        // Observe cast list and update actor chips
        viewModel.castList.observe(viewLifecycleOwner) { castList ->
            val actorChipGroup = binding.root.findViewById<com.google.android.material.chip.ChipGroup>(R.id.actorChips)
            actorChipGroup?.removeAllViews()

            if (castList.isNotEmpty()) {
                castList.take(3).forEach { actor ->
                    val chip = com.google.android.material.chip.Chip(requireContext())
                    chip.text = actor.name
                    chip.isClickable = true
                    chip.isFocusable = true
                    chip.setChipBackgroundColorResource(R.color.md_theme_surfaceContainer)
                    chip.setTextColor(resources.getColor(R.color.text_primary, null))

                    chip.setOnClickListener {
                        val personId = actor.id.removePrefix("tmdb:").toIntOrNull()
                        if (personId != null) {
                            // Navigate to search with person
                            val intent = Intent(requireContext(), com.example.stremiompvplayer.MainActivity::class.java).apply {
                                putExtra("SEARCH_PERSON_ID", personId)
                                putExtra("SEARCH_QUERY", actor.name)
                            }
                            startActivity(intent)
                        }
                    }

                    actorChipGroup?.addView(chip)
                }
            }
        }

        // [ADD] Observer for logo loading - same as DiscoverFragment
        viewModel.currentLogo.observe(viewLifecycleOwner) { logoUrl ->
            when (logoUrl) {
                "" -> { // Loading
                    binding.detailLogo.visibility = View.GONE
                }
                null -> { // No Logo
                    binding.detailLogo.visibility = View.GONE
                }
                else -> { // Has Logo
                    binding.detailLogo.visibility = View.VISIBLE
                    Glide.with(this)
                        .load(logoUrl)
                        .fitCenter()
                        .into(binding.detailLogo)
                }
            }
        }

        // Observer for series metadata (for drill-down navigation)
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

        // Observer for season episodes (for drill-down navigation)
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
    }

    private fun showSortFilterMenu(view: View) {
        val wrapper = ContextThemeWrapper(requireContext(),
            android.R.style.Theme_DeviceDefault_Light_NoActionBar)
        val popup = PopupMenu(wrapper, view)

        // Add sorting options
        popup.menu.add("Sort by Release Date (Newest First)")
        popup.menu.add("Sort by Release Date (Oldest First)")
        popup.menu.add("Sort by Name (A-Z)")
        popup.menu.add("Sort by Name (Z-A)")
        popup.menu.add("Sort by Date Added (Newest First)")
        popup.menu.add("Sort by Date Added (Oldest First)")

        // Add genre filter option
        popup.menu.add("Filter by Genre...")

        popup.setOnMenuItemClickListener { menuItem ->
            val type = arguments?.getString(ARG_TYPE) ?: "movie"
            when (menuItem.title) {
                "Sort by Release Date (Newest First)" -> {
                    currentSortBy = "releaseDate"
                    currentSortAscending = false
                    viewModel.filterAndSortLibrary(type, currentGenreFilter, currentSortBy, currentSortAscending)
                    true
                }
                "Sort by Release Date (Oldest First)" -> {
                    currentSortBy = "releaseDate"
                    currentSortAscending = true
                    viewModel.filterAndSortLibrary(type, currentGenreFilter, currentSortBy, currentSortAscending)
                    true
                }
                "Sort by Name (A-Z)" -> {
                    currentSortBy = "title"
                    currentSortAscending = true
                    viewModel.filterAndSortLibrary(type, currentGenreFilter, currentSortBy, currentSortAscending)
                    true
                }
                "Sort by Name (Z-A)" -> {
                    currentSortBy = "title"
                    currentSortAscending = false
                    viewModel.filterAndSortLibrary(type, currentGenreFilter, currentSortBy, currentSortAscending)
                    true
                }
                "Sort by Date Added (Newest First)" -> {
                    currentSortBy = "dateAdded"
                    currentSortAscending = false
                    viewModel.filterAndSortLibrary(type, currentGenreFilter, currentSortBy, currentSortAscending)
                    true
                }
                "Sort by Date Added (Oldest First)" -> {
                    currentSortBy = "dateAdded"
                    currentSortAscending = true
                    viewModel.filterAndSortLibrary(type, currentGenreFilter, currentSortBy, currentSortAscending)
                    true
                }
                "Filter by Genre..." -> {
                    showGenreFilterDialog()
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

    private fun showGenreFilterDialog() {
        // Fetch genres for the current type
        val type = arguments?.getString(ARG_TYPE) ?: "movie"

        // Use the appropriate genre list based on type
        val genreSource = if (type == "movie") viewModel.movieGenres else viewModel.tvGenres

        // Check if genres are already loaded
        if (genreSource.value != null && genreSource.value!!.isNotEmpty()) {
            showGenreSelectionDialog(genreSource.value!!, type)
        } else {
            // Fetch genres if not loaded
            viewModel.fetchGenres(type)

            // Show loading dialog while genres are being fetched
            val builder = android.app.AlertDialog.Builder(requireContext())
            builder.setTitle("Select Genre")
            builder.setMessage("Loading genres...")
            val loadingDialog = builder.create()
            loadingDialog.show()

            // Observe once and show selection dialog
            genreSource.observe(viewLifecycleOwner, object : androidx.lifecycle.Observer<List<com.example.stremiompvplayer.models.TMDBGenre>> {
                override fun onChanged(genres: List<com.example.stremiompvplayer.models.TMDBGenre>) {
                    loadingDialog.dismiss()
                    genreSource.removeObserver(this)

                    if (genres.isEmpty()) {
                        Toast.makeText(requireContext(), "No genres available", Toast.LENGTH_SHORT).show()
                        return
                    }

                    showGenreSelectionDialog(genres, type)
                }
            })
        }
    }

    private fun showGenreSelectionDialog(genres: List<com.example.stremiompvplayer.models.TMDBGenre>, type: String) {
        val genreNames = listOf("All Genres") + genres.map { it.name }
        val genreIds = listOf(null) + genres.map { it.id.toString() }

        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Select Genre")
            .setItems(genreNames.toTypedArray()) { _, which ->
                currentGenreFilter = genreIds[which]
                val selectedGenreName = genreNames[which]

                // Update label to show filter
                val label = if (currentGenreFilter == null) {
                    if (type == "movie") "My Movies" else "My Series"
                } else {
                    if (type == "movie") "My Movies - $selectedGenreName" else "My Series - $selectedGenreName"
                }
                updateCurrentListLabel(label)

                // Apply filter
                viewModel.filterAndSortLibrary(type, currentGenreFilter, currentSortBy, currentSortAscending)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    fun focusSidebar(): Boolean {
        binding.root.post {
            // Focus on Play button or poster carousel
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