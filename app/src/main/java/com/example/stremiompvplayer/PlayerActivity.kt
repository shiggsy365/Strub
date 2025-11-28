package com.example.stremiompvplayer

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.MimeTypes
import androidx.media3.common.C
import androidx.media3.common.TrackSelectionParameters
import androidx.media3.common.Tracks
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.CaptionStyleCompat
import com.example.stremiompvplayer.databinding.ActivityPlayerBinding
import com.example.stremiompvplayer.models.MetaItem
import com.example.stremiompvplayer.models.Stream
import com.example.stremiompvplayer.models.Subtitle
import androidx.activity.viewModels
import com.example.stremiompvplayer.viewmodels.MainViewModel
import com.example.stremiompvplayer.viewmodels.MainViewModelFactory
import com.example.stremiompvplayer.data.ServiceLocator
import com.example.stremiompvplayer.utils.SharedPreferencesManager
import com.example.stremiompvplayer.network.AIOStreamsClient
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.util.Log
import kotlinx.coroutines.isActive

class PlayerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPlayerBinding
    private var player: ExoPlayer? = null

    private val viewModel: MainViewModel by viewModels {
        MainViewModelFactory(
            ServiceLocator.getInstance(applicationContext),
            SharedPreferencesManager.getInstance(this)
        )
    }
    private var currentStream: Stream? = null
    private var currentMeta: MetaItem? = null

    // Save position for rotation/backgrounding
    private var playWhenReady = true
    private var currentItem = 0
    private var playbackPosition = 0L

    // Play Next functionality
    private var nextEpisode: MetaItem? = null
    private var playNextShown = false
    private var monitorJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Keep screen on
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        currentStream = intent.getSerializableExtra("stream") as? Stream
        currentMeta = intent.getSerializableExtra("meta") as? MetaItem

        if (currentStream == null) {
            finish()
            return
        }

        setupPlayNextButtons()
        checkForNextEpisode()
    }

    private fun setupPlayNextButtons() {
        binding.btnPlayNext.setOnClickListener {
            playNextEpisode()
        }

        binding.btnPlayNextHide.setOnClickListener {
            binding.playNextCard.visibility = View.GONE
        }
    }

    private fun checkForNextEpisode() {
        val metaId = currentMeta?.id ?: return
        val type = currentMeta?.type ?: return

        // Only check for next episode if current is an episode
        if (type == "episode") {
            lifecycleScope.launch {
                try {
                    nextEpisode = viewModel.getNextEpisode(metaId)
                    if (nextEpisode != null) {
                        Log.d("PlayerActivity", "Found next episode: ${nextEpisode?.name}")
                        startPlaybackMonitoring()
                    } else {
                        Log.d("PlayerActivity", "No next episode available")
                    }
                } catch (e: Exception) {
                    Log.e("PlayerActivity", "Error checking for next episode", e)
                }
            }
        }
    }

    private fun startPlaybackMonitoring() {
        monitorJob?.cancel()
        monitorJob = lifecycleScope.launch {
            // PERFORMANCE: Only check intensively near the end to reduce CPU usage
            while (isActive) {
                player?.let { exoPlayer ->
                    val duration = exoPlayer.duration
                    val position = exoPlayer.currentPosition

                    if (duration > 0 && !playNextShown) {
                        val remainingTime = duration - position

                        // Only check frequently in the last 2 minutes
                        if (remainingTime <= 120000) { // 2 minutes
                            // Check every second when near the end
                            if (remainingTime in 1..30000 && nextEpisode != null) {
                                showPlayNextPopup()
                                playNextShown = true
                                return@launch // Exit monitoring after showing popup
                            }
                            delay(1000)
                        } else {
                            // Check every 30 seconds when not near the end
                            delay(30000)
                        }
                    } else {
                        // No duration yet or popup already shown
                        delay(5000)
                    }
                } ?: return@launch // Exit if no player
            }
        }
    }

    private fun showPlayNextPopup() {
        runOnUiThread {
            nextEpisode?.let { episode ->
                binding.playNextEpisodeName.text = episode.name
                binding.playNextCard.visibility = View.VISIBLE
                Log.d("PlayerActivity", "Showing Play Next popup for: ${episode.name}")
            }
        }
    }

    private fun playNextEpisode() {
        val next = nextEpisode ?: return

        // Update UI to show "Playing Next"
        binding.playNextTitle.text = "Playing Next"
        binding.btnPlayNext.isEnabled = false

        lifecycleScope.launch {
            try {
                // Get the parent ID for loading streams
                val parts = next.id.split(":")
                if (parts.size >= 4) {
                    val parentId = "${parts[0]}:${parts[1]}"
                    val season = parts[2].toIntOrNull() ?: 1
                    val episode = parts[3].toIntOrNull() ?: 1

                    // Load streams for next episode
                    viewModel.loadEpisodeStreams(parentId, season, episode)

                    // Observe streams and play first available (FIXED: remove observer after use to prevent leak)
                    val observer = object : androidx.lifecycle.Observer<List<com.example.stremiompvplayer.models.Stream>> {
                        override fun onChanged(streams: List<com.example.stremiompvplayer.models.Stream>) {
                            if (streams.isNotEmpty()) {
                                val firstStream = streams[0]
                                // Update current meta to next episode
                                currentMeta = next
                                currentStream = firstStream

                                // Release current player and start new one
                                releasePlayer()
                                playbackPosition = 0L
                                initializePlayer()

                                // Hide the popup
                                binding.playNextCard.visibility = View.GONE
                                playNextShown = false

                                // Check for next episode again
                                checkForNextEpisode()

                                // CRITICAL: Remove this observer to prevent memory leak
                                viewModel.streams.removeObserver(this)
                            }
                        }
                    }
                    viewModel.streams.observe(this@PlayerActivity, observer)
                }
            } catch (e: Exception) {
                Log.e("PlayerActivity", "Error playing next episode", e)
            }
        }
    }

    public override fun onStart() {
        super.onStart()
        initializePlayer()
    }

    public override fun onResume() {
        super.onResume()
        hideSystemUi()
        if (player == null) {
            initializePlayer()
        }
    }

    override fun onStop() {
    super.onStop()
    monitorJob?.cancel()
    if (player != null) {
        saveProgress() // Ensure we save locally
        releasePlayer() // Ensure we scrobble 'stop' to Trakt
    }
    }

    public override fun onPause() {
        super.onPause()
        monitorJob?.cancel()
        saveProgress()
        // [FIX] Release player immediately on pause to stop audio when pressing back
        releasePlayer()
    }

    private fun releasePlayer() {
        // Release ExoPlayer
        player?.let { exoPlayer ->
             playbackPosition = exoPlayer.currentPosition
                currentItem = exoPlayer.currentMediaItemIndex
                playWhenReady = exoPlayer.playWhenReady

            // Scrobble STOP
            currentMeta?.let { meta ->
                val duration = exoPlayer.duration
                val position = exoPlayer.currentPosition

                // Only scrobble if meaningful duration
                if (duration > 0) {
                    val progress = (position.toFloat() / duration.toFloat()) * 100f
                    viewModel.scrobble("stop", meta, progress)

                    // OPTIONAL: If >90%, explicitly mark watched here to be safe
                    if (progress > 90f) {
                         viewModel.markAsWatched(meta, syncToTrakt = false) // Trakt scrobble handles remote, we handle local
                    }
                }
            }
            exoPlayer.release()
        }
        player = null
    }

    @OptIn(UnstableApi::class) private fun initializePlayer() {
        if (player != null) return

        // 1. Create the player
        player = ExoPlayer.Builder(this)
            .setSeekForwardIncrementMs(10000)
            .setSeekBackIncrementMs(10000)
            .build()
            .also { exoPlayer ->
                binding.playerView.player = exoPlayer

                // 2. Build the MediaItem with subtitles
                val url = currentStream?.url ?: ""

                // Fetch and add subtitles asynchronously
                lifecycleScope.launch {
                    Log.d("PlayerActivity", "=== SUBTITLE FETCH START ===")
                    Log.d("PlayerActivity", "currentMeta is ${if (currentMeta == null) "NULL" else "not null"}")
                    Log.d("PlayerActivity", "Meta name: ${currentMeta?.name ?: "NULL"}")
                    Log.d("PlayerActivity", "Meta ID: ${currentMeta?.id ?: "NULL"}")
                    Log.d("PlayerActivity", "Meta type: ${currentMeta?.type ?: "NULL"}")

                    val subtitles = currentMeta?.let { meta ->
                        try {
                            Log.d("PlayerActivity", "Calling viewModel.fetchSubtitles()...")
                            val result = viewModel.fetchSubtitles(meta)
                            Log.d("PlayerActivity", "fetchSubtitles() returned ${result.size} subtitles")
                            result
                        } catch (e: Exception) {
                            Log.e("PlayerActivity", "Error fetching subtitles: ${e.message}", e)
                            e.printStackTrace()
                            emptyList()
                        }
                    } ?: run {
                        Log.e("PlayerActivity", "currentMeta is NULL - cannot fetch subtitles")
                        emptyList()
                    }

                    Log.d("PlayerActivity", "Processing ${subtitles.size} subtitles for ExoPlayer")

                    // Build MediaItem with subtitles
                    val subtitleConfigurations = subtitles.map { subtitle ->
                        // Detect MIME type from URL extension
                        val mimeType = when {
                            subtitle.url.endsWith(".vtt", ignoreCase = true) -> MimeTypes.TEXT_VTT
                            subtitle.url.endsWith(".srt", ignoreCase = true) -> MimeTypes.APPLICATION_SUBRIP
                            subtitle.url.endsWith(".ass", ignoreCase = true) || subtitle.url.endsWith(".ssa", ignoreCase = true) -> MimeTypes.TEXT_SSA
                            else -> {
                                Log.w("PlayerActivity", "Unknown subtitle format for URL: ${subtitle.url}, defaulting to VTT")
                                MimeTypes.TEXT_VTT
                            }
                        }

                        Log.d("PlayerActivity", "Creating subtitle config: ${subtitle.formattedTitle} ($mimeType)")

                        MediaItem.SubtitleConfiguration.Builder(
                            android.net.Uri.parse(subtitle.url)
                        )
                            .setMimeType(mimeType)
                            .setLanguage("eng")
                            .setLabel(subtitle.formattedTitle)  // "AIO - [Title]"
                            .setSelectionFlags(0)  // Not forced, user can select
                            .build()
                    }

                    val mediaItem = MediaItem.Builder()
                        .setUri(url)
                        .setSubtitleConfigurations(subtitleConfigurations)
                        .build()

                    if (subtitles.isNotEmpty()) {
                        Log.i("PlayerActivity", "✓ Successfully added ${subtitles.size} English subtitles to ExoPlayer")
                        subtitles.forEachIndexed { index, subtitle ->
                            Log.i("PlayerActivity", "  [$index] ${subtitle.formattedTitle}")
                            Log.d("PlayerActivity", "       URL: ${subtitle.url}")
                        }
                    } else {
                        Log.w("PlayerActivity", "⚠ No subtitles found for ${currentMeta?.name ?: "unknown"}")
                        Log.w("PlayerActivity", "   Check AIOStreams manifest URL configuration and IMDb ID lookup")
                    }

                    Log.d("PlayerActivity", "=== SUBTITLE FETCH END ===")

                    // 3. Set media and prepare (on main thread)
                    withContext(Dispatchers.Main) {
                        exoPlayer.setMediaItem(mediaItem)
                        exoPlayer.playWhenReady = playWhenReady
                        if (playbackPosition > 0) {
                            exoPlayer.seekTo(currentItem, playbackPosition)
                        } else if (currentMeta != null && currentMeta!!.progress > 0 && !currentMeta!!.isWatched) {
                            // Resume from database history if not already watched
                            exoPlayer.seekTo(currentItem, currentMeta!!.progress)
                        }
                        exoPlayer.prepare()
                    }
                }

                // 4. Add Listeners for buffering/errors
                exoPlayer.addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        when (playbackState) {
                            Player.STATE_BUFFERING -> binding.loadingProgress.visibility = View.VISIBLE
                            Player.STATE_READY -> {
                                binding.loadingProgress.visibility = View.GONE
                                // Apply subtitle styling when player is ready
                                applySubtitleStyling()
                                // Auto-select English tracks when player is ready
                                selectEnglishTracks()
                            }
                            Player.STATE_ENDED -> { /* Finish activity or play next */ }
                            Player.STATE_IDLE -> binding.loadingProgress.visibility = View.GONE
                        }
                    }

                    override fun onTracksChanged(tracks: Tracks) {
                        // Called when tracks become available or change
                        // Re-apply English track selection when tracks change
                        selectEnglishTracks()
                    }

                    override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                        Log.e("PlayerActivity", "Playback error: ${error.message}", error)
                        Log.e("PlayerActivity", "Error code: ${error.errorCode}")
                        Log.e("PlayerActivity", "Stream URL: ${currentStream?.url}")

                        // Show error message and return to stream selection
                        runOnUiThread {
                            val errorMessage = when {
                                error.message?.contains("Invalid NAL length") == true ->
                                    "Video codec not supported. Please select a different stream."
                                error.message?.contains("Source error") == true ->
                                    "Failed to load stream. Please try a different stream."
                                error.errorCode == 3001 ->
                                    "Media format error. Please select a different stream."
                                else -> "Playback error: ${error.message}"
                            }

                            android.widget.Toast.makeText(
                                this@PlayerActivity,
                                errorMessage,
                                android.widget.Toast.LENGTH_LONG
                            ).show()

                            // Return to stream selection
                            finish()
                        }
                    }

                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        currentMeta?.let { meta ->
                            val duration = player?.duration ?: 0L
                            val position = player?.currentPosition ?: 0L
                            if (duration > 0) {
                                val progress = (position.toFloat() / duration.toFloat()) * 100f
                                if (isPlaying) {
                                    viewModel.scrobble("start", meta, progress)
                                } else {
                                    viewModel.scrobble("pause", meta, progress)
                                }
                            }
                        }
                    }
                })
            }
    }

    private fun saveProgress() {
        // Save ExoPlayer progress
        player?.let { exoPlayer ->
            val position = exoPlayer.currentPosition
            val duration = exoPlayer.duration

            if (currentMeta != null && duration > 0) {
                viewModel.saveWatchProgress(currentMeta!!, position, duration)
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // Stop playback and return to the previous activity
        // onStop() will be called automatically, which handles:
        // 1. Saving progress
        // 2. Releasing player
        // 3. Scrobbling to Trakt

        // Just finish this activity to return to the calling activity
        finish()
    }

    private fun applySubtitleStyling() {
        try {
            val prefsManager = SharedPreferencesManager.getInstance(this)

            // Get subtitle preferences
            val textSize = prefsManager.getSubtitleTextSize()
            val textColor = prefsManager.getSubtitleTextColor()
            val backgroundColor = prefsManager.getSubtitleBackgroundColor()
            val windowColor = prefsManager.getSubtitleWindowColor()
            val edgeType = prefsManager.getSubtitleEdgeType()
            val edgeColor = prefsManager.getSubtitleEdgeColor()

            // Create CaptionStyleCompat with user preferences
            val captionStyle = CaptionStyleCompat(
                textColor,                    // foregroundColor
                backgroundColor,              // backgroundColor
                windowColor,                  // windowColor
                edgeType,                     // edgeType (0=NONE, 1=OUTLINE, 2=DROP_SHADOW, 3=RAISED, 4=DEPRESSED)
                edgeColor,                    // edgeColor
                null                          // typeface (null = default)
            )

            // Apply to SubtitleView
            val subtitleView = binding.playerView.subtitleView
            subtitleView?.setStyle(captionStyle)

            // Set text size scale
            subtitleView?.setFixedTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 20f * textSize)

            Log.d("PlayerActivity", "Applied subtitle styling - Size: $textSize, Color: $textColor, Edge: $edgeType")
        } catch (e: Exception) {
            Log.e("PlayerActivity", "Error applying subtitle styling", e)
        }
    }

    /**
     * Automatically select first English subtitle and English audio track
     */
    private fun selectEnglishTracks() {
        player?.let { exoPlayer ->
            try {
                val currentTracks = exoPlayer.currentTracks

                Log.d("PlayerActivity", "=== AUTO TRACK SELECTION START ===")
                Log.d("PlayerActivity", "Total track groups: ${currentTracks.groups.size}")

                // Build track selection parameters
                val trackSelectionParameters = exoPlayer.trackSelectionParameters
                    .buildUpon()
                    .setPreferredAudioLanguage("eng")  // Prefer English audio
                    .setPreferredTextLanguage("eng")   // Prefer English subtitles
                    .setSelectUndeterminedTextLanguage(true)  // Also select undetermined language subtitles
                    .build()

                exoPlayer.trackSelectionParameters = trackSelectionParameters

                // Log available tracks for debugging
                currentTracks.groups.forEachIndexed { groupIndex, trackGroup ->
                    val trackType = trackGroup.type
                    val trackTypeName = when (trackType) {
                        C.TRACK_TYPE_AUDIO -> "AUDIO"
                        C.TRACK_TYPE_TEXT -> "SUBTITLE"
                        C.TRACK_TYPE_VIDEO -> "VIDEO"
                        else -> "OTHER($trackType)"
                    }

                    Log.d("PlayerActivity", "Track group $groupIndex: $trackTypeName")

                    for (i in 0 until trackGroup.length) {
                        val format = trackGroup.getTrackFormat(i)
                        val isSelected = trackGroup.isTrackSelected(i)
                        val language = format.language ?: "unknown"
                        val label = format.label ?: "no label"

                        Log.d("PlayerActivity", "  Track $i: Lang=$language, Label=$label, Selected=$isSelected")
                    }
                }

                Log.i("PlayerActivity", "✓ Set track preferences to English audio and subtitles")
                Log.d("PlayerActivity", "=== AUTO TRACK SELECTION END ===")
            } catch (e: Exception) {
                Log.e("PlayerActivity", "Error selecting English tracks", e)
            }
        }
    }

    private fun hideSystemUi() {
        // Immersive mode
        binding.root.systemUiVisibility = (View.SYSTEM_UI_FLAG_LOW_PROFILE
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION)
    }
}
