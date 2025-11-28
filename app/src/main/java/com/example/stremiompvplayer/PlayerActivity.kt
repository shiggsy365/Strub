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
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer

class PlayerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPlayerBinding
    private var player: ExoPlayer? = null

    // LibVLC fallback player
    private var libVLC: LibVLC? = null
    private var vlcPlayer: MediaPlayer? = null
    private var usingVLC = false

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
        // Release VLC player if using VLC
        if (usingVLC) {
            releaseVLCPlayer()
            return
        }

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

                // Apply subtitle styling from preferences
                applySubtitleStyling()

                // 2. Build the MediaItem with subtitles
                val url = currentStream?.url ?: ""

                // Fetch and add subtitles asynchronously
                lifecycleScope.launch {
                    Log.d("PlayerActivity", "=== SUBTITLE FETCH START ===")
                    Log.d("PlayerActivity", "Meta: ${currentMeta?.name} (ID: ${currentMeta?.id}, Type: ${currentMeta?.type})")

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
                    } ?: emptyList()

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
                            Player.STATE_READY -> binding.loadingProgress.visibility = View.GONE
                            Player.STATE_ENDED -> { /* Finish activity or play next */ }
                            Player.STATE_IDLE -> binding.loadingProgress.visibility = View.GONE
                        }
                    }

                    override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                        Log.e("PlayerActivity", "Playback error: ${error.message}", error)
                        Log.e("PlayerActivity", "Error code: ${error.errorCode}")
                        Log.e("PlayerActivity", "Stream URL: ${currentStream?.url}")

                        // Check if this is a codec/format error that VLC might handle
                        val isCodecError = error.message?.contains("Invalid NAL length") == true ||
                                error.message?.contains("Source error") == true ||
                                error.errorCode == 3001

                        if (isCodecError && !usingVLC) {
                            // Offer to switch to VLC player
                            runOnUiThread {
                                android.app.AlertDialog.Builder(this@PlayerActivity)
                                    .setTitle("Playback Error")
                                    .setMessage("This video format may not be supported by the standard player. Would you like to try the VLC player engine instead?")
                                    .setPositiveButton("Try VLC Player") { _, _ ->
                                        switchToVLCPlayer()
                                    }
                                    .setNegativeButton("Select Different Stream") { _, _ ->
                                        finish()
                                    }
                                    .setCancelable(false)
                                    .show()
                            }
                        } else {
                            // Show error toast for other errors
                            val errorMessage = when {
                                usingVLC -> "VLC Player error: ${error.message}"
                                else -> "Playback error: ${error.message}"
                            }

                            android.widget.Toast.makeText(
                                this@PlayerActivity,
                                errorMessage,
                                android.widget.Toast.LENGTH_LONG
                            ).show()
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
        if (usingVLC) {
            // Save VLC progress
            vlcPlayer?.let { vlc ->
                val position = vlc.time
                val duration = vlc.length

                if (currentMeta != null && duration > 0) {
                    viewModel.saveWatchProgress(currentMeta!!, position, duration)
                }
            }
        } else {
            // Save ExoPlayer progress
            player?.let { exoPlayer ->
                val position = exoPlayer.currentPosition
                val duration = exoPlayer.duration

                if (currentMeta != null && duration > 0) {
                    viewModel.saveWatchProgress(currentMeta!!, position, duration)
                }
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

    private fun switchToVLCPlayer() {
        Log.i("PlayerActivity", "Switching to VLC player for better codec support")

        // Release ExoPlayer
        player?.release()
        player = null

        // Mark that we're using VLC
        usingVLC = true

        // Hide ExoPlayer view, show VLC surface
        binding.playerView.visibility = View.GONE
        binding.vlcSurfaceView.visibility = View.VISIBLE

        // Initialize VLC player
        initializeVLCPlayer()
    }

    private fun initializeVLCPlayer() {
        try {
            // Create LibVLC instance with optimized options
            val options = arrayListOf(
                "--no-drop-late-frames",
                "--no-skip-frames",
                "--network-caching=1500",
                "--clock-jitter=0",
                "--live-caching=1500"
            )

            libVLC = LibVLC(this, options)
            vlcPlayer = MediaPlayer(libVLC)

            // Attach to the dedicated VLC surface view
            vlcPlayer?.attachViews(binding.vlcSurfaceView, null, false, false)

            // Set media with HTTP headers to match ExoPlayer behavior
            val streamUrl = currentStream?.url ?: ""
            val media = Media(libVLC, Uri.parse(streamUrl))

            // Add HTTP headers to prevent "forbidden" errors from AIOStreams
            // These headers make VLC requests look identical to ExoPlayer requests
            media.addOption(":http-user-agent=ExoPlayer/2.18.1 (Linux;Android ${android.os.Build.VERSION.RELEASE}) ExoPlayerLib/2.18.1")
            media.addOption(":http-referrer=${streamUrl}")

            // Add additional options for better streaming compatibility
            media.addOption(":network-caching=1500")
            media.addOption(":file-caching=1500")

            Log.d("PlayerActivity", "VLC media created with headers for: $streamUrl")

            vlcPlayer?.media = media
            media.release()

            // Add event listener for VLC errors
            vlcPlayer?.setEventListener { event ->
                when (event.type) {
                    MediaPlayer.Event.Playing -> {
                        binding.loadingProgress.visibility = View.GONE
                        Log.d("PlayerActivity", "VLC: Playing")
                    }
                    MediaPlayer.Event.Buffering -> {
                        binding.loadingProgress.visibility = View.VISIBLE
                        Log.d("PlayerActivity", "VLC: Buffering")
                    }
                    MediaPlayer.Event.EncounteredError -> {
                        Log.e("PlayerActivity", "VLC Player error encountered")
                        runOnUiThread {
                            android.widget.Toast.makeText(
                                this,
                                "VLC Player error. Try selecting a different stream.",
                                android.widget.Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                    MediaPlayer.Event.EndReached -> {
                        Log.d("PlayerActivity", "VLC: End reached")
                        // Handle end of playback
                    }
                }
            }

            // Seek to saved position if any
            if (playbackPosition > 0) {
                vlcPlayer?.time = playbackPosition
            } else if (currentMeta != null && currentMeta!!.progress > 0 && !currentMeta!!.isWatched) {
                vlcPlayer?.time = currentMeta!!.progress
            }

            // Start playback
            vlcPlayer?.play()

            Log.i("PlayerActivity", "VLC player initialized successfully")

        } catch (e: Exception) {
            Log.e("PlayerActivity", "Error initializing VLC player", e)
            android.widget.Toast.makeText(
                this,
                "Failed to initialize VLC player: ${e.message}",
                android.widget.Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun releaseVLCPlayer() {
        if (usingVLC) {
            // Save current position
            vlcPlayer?.let {
                playbackPosition = it.time
            }

            vlcPlayer?.release()
            libVLC?.release()
            vlcPlayer = null
            libVLC = null
        }
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
