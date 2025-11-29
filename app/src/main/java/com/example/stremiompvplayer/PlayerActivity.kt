package com.example.stremiompvplayer

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.Toast
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

    // Auto-play / Scraper State
    private var streamList: List<Stream> = emptyList()
    private var currentStreamIndex = 0
    private var isAutoPlaying = false

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

        if (currentStream == null && currentMeta == null) {
            finish()
            return
        }

        setupPlayNextButtons()

        if (currentStream != null) {
            // Direct play mode (legacy)
            checkForNextEpisode()
        } else if (currentMeta != null) {
            // Auto-scrape mode
            isAutoPlaying = true
            binding.loadingProgress.visibility = View.VISIBLE
            showStatus("Scraping streams...")

            // Observe streams
            viewModel.streams.observe(this) { streams ->
                if (streams.isNotEmpty()) {
                    streamList = streams
                    currentStreamIndex = 0
                    playStreamAtIndex(currentStreamIndex)
                } else {
                    // Only show error if we are not currently loading (avoid initial empty state)
                    if (viewModel.isLoading.value == false) {
                        showErrorAndFinish("No streams found")
                    }
                }
            }

            // Trigger load
            viewModel.loadStreams(currentMeta!!.type, currentMeta!!.id)
        }
    }

    private fun playStreamAtIndex(index: Int) {
        if (index >= streamList.size) {
            showErrorAndFinish("No playable streams found")
            return
        }

        currentStream = streamList[index]
        val displayIndex = index + 1
        val total = streamList.size

        showStatus("Trying stream $displayIndex of $total...")

        // Reset player
        releasePlayer()
        initializePlayer()
    }

    private fun tryNextStream(reason: String) {
        Log.w("PlayerActivity", "Stream failed: $reason. Moving to next.")
        currentStreamIndex++
        playStreamAtIndex(currentStreamIndex)
    }

    private fun showStatus(message: String) {
        // You might want a dedicated TextView for status updates overlaying the player
        // For now, we can use a Toast or repurpose Play Next text temporarily if visible
        // Ideally, add a status TextView to layout. I'll use Toast for simplicity or logs.
        runOnUiThread {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun showErrorAndFinish(message: String) {
        runOnUiThread {
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
            finish()
        }
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
                                // In auto-play mode, we update the list and reset index
                                streamList = streams
                                currentStreamIndex = 0

                                // Update current meta to next episode
                                currentMeta = next
                                currentStream = streams[0]

                                // Release current player and start new one
                                releasePlayer()
                                playbackPosition = 0L

                                // If we are in auto-play mode, playStreamAtIndex handles init
                                if (isAutoPlaying) {
                                    playStreamAtIndex(0)
                                } else {
                                    initializePlayer()
                                }

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
        if (currentStream != null) {
            initializePlayer()
        }
    }

    public override fun onResume() {
        super.onResume()
        hideSystemUi()
        if (player == null && currentStream != null) {
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

        binding.loadingProgress.visibility = View.VISIBLE

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

                    // Skip subtitle fetch if not needed or already failed to simplify loop
                    val subtitles = currentMeta?.let { meta ->
                        try {
                            viewModel.fetchSubtitles(meta)
                        } catch (e: Exception) {
                            emptyList()
                        }
                    } ?: emptyList()

                    // Build MediaItem with subtitles
                    val subtitleConfigurations = subtitles.mapIndexed { index, subtitle ->
                        val mimeType = when {
                            subtitle.url.endsWith(".vtt", ignoreCase = true) -> MimeTypes.TEXT_VTT
                            subtitle.url.endsWith(".srt", ignoreCase = true) -> MimeTypes.APPLICATION_SUBRIP
                            subtitle.url.endsWith(".ass", ignoreCase = true) || subtitle.url.endsWith(".ssa", ignoreCase = true) -> MimeTypes.TEXT_SSA
                            subtitle.url.contains("stremio", ignoreCase = true) || subtitle.url.contains("opensubtitles", ignoreCase = true) -> MimeTypes.APPLICATION_SUBRIP
                            else -> MimeTypes.APPLICATION_SUBRIP
                        }

                        MediaItem.SubtitleConfiguration.Builder(
                            android.net.Uri.parse(subtitle.url)
                        )
                            .setMimeType(mimeType)
                            .setLanguage("eng")
                            .setLabel(subtitle.formattedTitle)
                            .setSelectionFlags(if (index == 0) androidx.media3.common.C.SELECTION_FLAG_DEFAULT else 0)
                            .build()
                    }

                    val mediaItem = MediaItem.Builder()
                        .setUri(url)
                        .setSubtitleConfigurations(subtitleConfigurations)
                        .build()

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

                                // DURATION CHECK FOR AUTO-PLAY
                                if (isAutoPlaying) {
                                    val durationMs = exoPlayer.duration
                                    if (durationMs > 0 && durationMs < 120000) { // Less than 2 minutes
                                        tryNextStream("Duration too short (${durationMs}ms)")
                                        return // Stop processing readiness
                                    } else {
                                        // Valid stream found!
                                        // Show success? Or just let it play.
                                        checkForNextEpisode() // Start checking for next episode
                                    }
                                }

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
                        selectEnglishTracks()
                    }

                    override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                        Log.e("PlayerActivity", "Playback error: ${error.message}")

                        if (isAutoPlaying) {
                            // Try next stream
                            tryNextStream("Playback error: ${error.message}")
                        } else {
                            // Normal error handling
                            runOnUiThread {
                                val errorMessage = "Playback error: ${error.message}"
                                android.widget.Toast.makeText(this@PlayerActivity, errorMessage, android.widget.Toast.LENGTH_LONG).show()
                                finish()
                            }
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
        finish()
    }

    private fun applySubtitleStyling() {
        try {
            val prefsManager = SharedPreferencesManager.getInstance(this)

            val textSize = prefsManager.getSubtitleTextSize()
            val textColor = prefsManager.getSubtitleTextColor()
            val backgroundColor = prefsManager.getSubtitleBackgroundColor()
            val windowColor = prefsManager.getSubtitleWindowColor()
            val edgeType = prefsManager.getSubtitleEdgeType()
            val edgeColor = prefsManager.getSubtitleEdgeColor()

            val captionStyle = CaptionStyleCompat(
                textColor, backgroundColor, windowColor, edgeType, edgeColor, null
            )

            val subtitleView = binding.playerView.subtitleView
            subtitleView?.setStyle(captionStyle)
            subtitleView?.setFixedTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 20f * textSize)
        } catch (e: Exception) {
            Log.e("PlayerActivity", "Error applying subtitle styling", e)
        }
    }

    private fun selectEnglishTracks() {
        player?.let { exoPlayer ->
            try {
                val trackSelectionParameters = exoPlayer.trackSelectionParameters
                    .buildUpon()
                    .setPreferredAudioLanguage("eng")
                    .setPreferredTextLanguage("eng")
                    .setSelectUndeterminedTextLanguage(true)
                    .setIgnoredTextSelectionFlags(0)
                    .build()

                exoPlayer.trackSelectionParameters = trackSelectionParameters
            } catch (e: Exception) {
                Log.e("PlayerActivity", "Error selecting English tracks", e)
            }
        }
    }

    private fun hideSystemUi() {
        binding.root.systemUiVisibility = (View.SYSTEM_UI_FLAG_LOW_PROFILE
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION)
    }
}