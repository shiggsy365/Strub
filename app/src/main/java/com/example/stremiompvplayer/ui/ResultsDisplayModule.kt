package com.example.stremiompvplayer.ui

import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.stremiompvplayer.PlayerActivity
import com.example.stremiompvplayer.R
import com.example.stremiompvplayer.models.MetaItem
import com.example.stremiompvplayer.models.Stream
import com.example.stremiompvplayer.viewmodels.MainViewModel
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Standardized module for displaying content results across all pages
 * (Home, Discover, Search, Cast Search, Related Search)
 *
 * Handles:
 * - Details pane updates with content-type-specific logic
 * - Content type detection (movie, series header/series/episode levels)
 * - Play button visibility (movies & episodes only)
 * - Drill-down navigation for shows (header → series → episodes)
 * - Button actions (Play, Trailer, Related)
 * - Actor chip display and interaction
 */
class ResultsDisplayModule(
    private val fragment: Fragment,
    private val viewModel: MainViewModel,
    private val config: Configuration
) {

    /**
     * Configuration for page-specific behavior
     */
    data class Configuration(
        val pageBackground: ImageView,
        val detailTitle: TextView,
        val detailLogo: ImageView,
        val detailDescription: TextView,
        val detailDate: TextView,
        val detailRating: TextView,
        val detailEpisode: TextView,
        val actorChips: ChipGroup,
        val btnPlay: View?,
        val btnTrailer: View?,
        val btnRelated: View?,
        val enableDrillDown: Boolean = false,  // Only Discover uses drill-down
        val useGridLayout: Boolean = false,     // Only Search uses grid
        val showEpisodeDescription: Boolean = false  // Show episode description instead of series description
    )

    /**
     * Drill-down navigation state (for Discover page)
     */
    enum class DrillDownLevel {
        CATALOG,  // Viewing catalog of movies/series
        SERIES,   // Viewing seasons of a series
        SEASON    // Viewing episodes of a season
    }

    var currentDrillDownLevel = DrillDownLevel.CATALOG
    var currentSeriesId: String? = null
    var currentSeasonNumber: Int? = null
    var currentSelectedItem: MetaItem? = null

    init {
        setupObservers()
        setupButtons()
    }

    /**
     * Updates the details pane based on content type
     * Handles: Movies, Series (header/series/episode levels), Episodes
     */
    fun updateDetailsPane(item: MetaItem) {
        currentSelectedItem = item

        // Set background image
        Glide.with(fragment)
            .load(item.background ?: item.poster)
            .into(config.pageBackground)

        // Set title (visibility controlled by logo observer)
        config.detailTitle.text = item.name
        config.detailTitle.visibility = View.VISIBLE

        // Format and set date
        val formattedDate = formatDate(item.releaseDate)
        config.detailDate.text = formattedDate ?: ""
        config.detailDate.visibility = if (formattedDate.isNullOrEmpty()) View.GONE else View.VISIBLE

        // Set rating
        config.detailRating.visibility = if (item.rating != null) {
            config.detailRating.text = "★ ${item.rating}"
            View.VISIBLE
        } else {
            View.GONE
        }

        // Handle episode-specific display
        if (item.type == "episode") {
            displayEpisodeInfo(item)
        } else {
            config.detailEpisode.visibility = View.GONE
        }

        // Set description (episode description for episodes if enabled, otherwise series description)
        if (item.type == "episode" && config.showEpisodeDescription) {
            // For episodes, show episode description if available
            config.detailDescription.text = item.description ?: "No description available."
        } else {
            config.detailDescription.text = item.description ?: "No description available."
        }

        // Fetch and display logo
        config.detailLogo.visibility = View.GONE
        viewModel.fetchItemLogo(item)

        // Update actor chips
        updateActorChips(item)

        // Update play button visibility based on content type
        updatePlayButtonVisibility()
    }

    /**
     * Displays episode information (episode number)
     */
    private fun displayEpisodeInfo(item: MetaItem) {
        val parts = item.id.split(":")
        if (parts.size >= 4) {
            val season = parts[2]
            val episode = parts[3]
            config.detailEpisode.text = "S${season.padStart(2, '0')}E${episode.padStart(2, '0')}"
            config.detailEpisode.visibility = View.VISIBLE
        } else {
            config.detailEpisode.visibility = View.GONE
        }
    }

    /**
     * Formats date to year only
     */
    private fun formatDate(dateString: String?): String? {
        return try {
            dateString?.let { dateStr ->
                val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val outputFormat = SimpleDateFormat("dd MMM yy", Locale.getDefault())
                val date = inputFormat.parse(dateStr)
                date?.let { outputFormat.format(it) }
            }
        } catch (e: Exception) {
            dateString
        }
    }

    /**
     * Updates actor chips with top 3 cast members
     */
    private fun updateActorChips(item: MetaItem) {
        config.actorChips.removeAllViews()
        viewModel.fetchCast(item.id, item.type)
    }

    /**
     * Updates play button visibility based on content type and drill-down level
     *
     * Rules:
     * 1. Movie: Show play button
     * 2. Series (header level): No play button, click drills into series
     * 3. Series (series level): No play button, click drills into episodes
     * 4. Episode: Show play button
     */
    fun updatePlayButtonVisibility() {
        val playButton = config.btnPlay ?: return

        val shouldShowPlay = when {
            // Movie: always show play button
            currentSelectedItem?.type == "movie" && currentDrillDownLevel == DrillDownLevel.CATALOG -> true

            // Episode: always show play button (episodes are already playable content)
            currentSelectedItem?.type == "episode" -> true

            // Series: only show at season level (episodes)
            config.enableDrillDown && currentDrillDownLevel == DrillDownLevel.SEASON -> true

            // Default: hide
            else -> false
        }

        playButton.visibility = if (shouldShowPlay) View.VISIBLE else View.GONE
    }

    /**
     * Handles content click based on type and drill-down level
     */
    fun onContentClicked(item: MetaItem) {
        updateDetailsPane(item)

        when (item.type) {
            "genre" -> {
                // Handle genre items (only in Discover)
                item.genreId?.let { genreId ->
                    val genreType = item.genreType ?: "movie"
                    loadGenreList?.invoke(genreId, item.name, genreType)
                }
            }
            "series" -> {
                if (config.enableDrillDown) {
                    // Drill down into series to show seasons
                    currentDrillDownLevel = DrillDownLevel.SERIES
                    currentSeriesId = item.id
                    currentSeasonNumber = null
                    viewModel.loadSeriesMeta(item.id)
                    updatePlayButtonVisibility()
                }
            }
            "season" -> {
                if (config.enableDrillDown) {
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
            }
            "episode" -> {
                // Focus play button for playable episode
                config.btnPlay?.requestFocus()
            }
            "movie" -> {
                // Focus play button for playable movie
                config.btnPlay?.requestFocus()
            }
        }
    }

    /**
     * Shows stream selection dialog
     */
    fun showStreamDialog(item: MetaItem) {
        // Clear any previous streams BEFORE creating the dialog to prevent stale data
        viewModel.clearStreams()

        val dialogView = android.view.LayoutInflater.from(fragment.requireContext())
            .inflate(R.layout.dialog_stream_selection, null)
        val dialog = android.app.AlertDialog.Builder(fragment.requireContext())
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
        rvStreams.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(fragment.requireContext())
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
        val streamObserver = Observer<List<Stream>> { streams ->
            // Only process if this is an update after loadStreams() call
            if (hasReceivedUpdate || streams.isNotEmpty()) {
                progressBar.visibility = View.GONE
                rvStreams.visibility = View.VISIBLE
                if (streams.isEmpty()) {
                    Toast.makeText(fragment.requireContext(), "No streams available", Toast.LENGTH_SHORT).show()
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
        viewModel.streams.observe(fragment.viewLifecycleOwner, streamObserver)

        dialog.setOnDismissListener {
            viewModel.streams.removeObserver(streamObserver)
            viewModel.clearStreams()
        }

        dialog.show()

        // Load streams AFTER setting up observer
        viewModel.loadStreams(item.type, item.id)
    }

    /**
     * Plays a stream
     */
    private fun playStream(stream: Stream) {
        val intent = Intent(fragment.requireContext(), PlayerActivity::class.java).apply {
            putExtra("stream", stream)
            putExtra("meta", currentSelectedItem)  // Pass full MetaItem for subtitles
            putExtra("title", currentSelectedItem?.name ?: "Unknown")
            putExtra("metaId", currentSelectedItem?.id)
        }
        fragment.startActivity(intent)
    }

    /**
     * Plays trailer in external app (YouTube, browser, etc.)
     */
    fun playTrailer(item: MetaItem) {
        fragment.lifecycleScope.launch {
            try {
                val trailerUrl = viewModel.fetchTrailer(item.id, item.type)
                if (trailerUrl != null) {
                    val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(trailerUrl))
                    fragment.startActivity(intent)
                } else {
                    Toast.makeText(fragment.requireContext(), "No trailer available", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(fragment.requireContext(), "Error loading trailer", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Shows related content
     */
    var onRelatedContentLoaded: ((List<MetaItem>) -> Unit)? = null

    fun showRelatedContent(item: MetaItem) {
        // Reset drill-down state
        if (config.enableDrillDown) {
            currentDrillDownLevel = DrillDownLevel.CATALOG
            currentSeriesId = null
            currentSeasonNumber = null
        }

        fragment.lifecycleScope.launch {
            try {
                val similarContent = viewModel.fetchSimilarContent(item.id, item.type)
                if (similarContent.isNotEmpty()) {
                    onRelatedContentLoaded?.invoke(similarContent)
                    updatePlayButtonVisibility()
                } else {
                    Toast.makeText(fragment.requireContext(), "No related content found", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(fragment.requireContext(), "Error loading related content", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Callback for loading genre lists (Discover only)
     */
    var loadGenreList: ((genreId: Int, genreName: String, type: String) -> Unit)? = null

    /**
     * Handles back press for drill-down navigation
     * Returns true if handled, false otherwise
     */
    fun handleBackPress(): Boolean {
        if (!config.enableDrillDown) return false

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

    /**
     * Setup observers for logo and cast
     */
    private fun setupObservers() {
        // Logo observer
        viewModel.currentLogo.observe(fragment.viewLifecycleOwner) { logoUrl ->
            when (logoUrl) {
                "" -> {
                    // Loading
                    config.detailTitle.visibility = View.GONE
                    config.detailLogo.visibility = View.GONE
                }
                null -> {
                    // No Logo - show text title fallback
                    config.detailTitle.visibility = View.VISIBLE
                    config.detailLogo.visibility = View.GONE
                }
                else -> {
                    // Has Logo - hide text title
                    config.detailTitle.visibility = View.GONE
                    config.detailLogo.visibility = View.VISIBLE
                    Glide.with(fragment)
                        .load(logoUrl)
                        .fitCenter()
                        .into(config.detailLogo)
                }
            }
        }

        // Cast loading observer
        viewModel.isCastLoading.observe(fragment.viewLifecycleOwner) { isLoading ->
            if (isLoading) {
                config.actorChips.removeAllViews()
                val placeholderChip = Chip(fragment.requireContext()).apply {
                    text = "No Cast Returned"
                    isClickable = false
                    isFocusable = false
                    setChipBackgroundColorResource(R.color.md_theme_surfaceContainer)
                    setTextColor(fragment.resources.getColor(R.color.text_primary, null))
                }
                config.actorChips.addView(placeholderChip)
            }
        }

        // Cast list observer
        viewModel.castList.observe(fragment.viewLifecycleOwner) { castList ->
            config.actorChips.removeAllViews()

            if (castList.isNotEmpty()) {
                castList.take(3).forEach { actor ->
                    val chip = Chip(fragment.requireContext())
                    chip.text = actor.name
                    chip.isClickable = true
                    chip.isFocusable = true
                    chip.setChipBackgroundColorResource(R.color.md_theme_surfaceContainer)
                    chip.setTextColor(fragment.resources.getColor(R.color.text_primary, null))

                    chip.setOnClickListener {
                        val personId = actor.id.removePrefix("tmdb:").toIntOrNull()
                        if (personId != null) {
                            // Reset drill-down state
                            if (config.enableDrillDown) {
                                currentDrillDownLevel = DrillDownLevel.CATALOG
                                currentSeriesId = null
                                currentSeasonNumber = null
                            }

                            // Load person's credits
                            onActorClicked?.invoke(personId, actor.name)
                        }
                    }

                    config.actorChips.addView(chip)
                }
            }
        }
    }

    /**
     * Callback for actor chip clicks
     */
    var onActorClicked: ((personId: Int, actorName: String) -> Unit)? = null

    /**
     * Setup button click listeners
     */
    private fun setupButtons() {
        config.btnPlay?.setOnClickListener {
            currentSelectedItem?.let { item -> showStreamDialog(item) }
        }

        config.btnTrailer?.setOnClickListener {
            currentSelectedItem?.let { item -> playTrailer(item) }
        }

        config.btnRelated?.setOnClickListener {
            currentSelectedItem?.let { item -> showRelatedContent(item) }
        }
    }
}
