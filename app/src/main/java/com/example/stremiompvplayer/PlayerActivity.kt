package com.example.stremiompvplayer

import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.example.stremiompvplayer.databinding.ActivityPlayerBinding
import com.example.stremiompvplayer.models.MetaItem
import com.example.stremiompvplayer.models.Stream
import androidx.activity.viewModels
import com.example.stremiompvplayer.viewmodels.MainViewModel
import com.example.stremiompvplayer.viewmodels.MainViewModelFactory
import com.example.stremiompvplayer.data.ServiceLocator
import com.example.stremiompvplayer.utils.SharedPreferencesManager
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import android.util.Log

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
            while (true) {
                delay(1000) // Check every second
                player?.let { exoPlayer ->
                    val duration = exoPlayer.duration
                    val position = exoPlayer.currentPosition

                    if (duration > 0 && !playNextShown) {
                        val remainingTime = duration - position
                        // Show popup in last 30 seconds
                        if (remainingTime in 1..30000 && nextEpisode != null) {
                            showPlayNextPopup()
                            playNextShown = true
                        }
                    }
                }
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

                // 2. Build the MediaItem
                val url = currentStream?.url ?: ""
                val mediaItem = MediaItem.fromUri(url)

                // 3. Set media and prepare
                exoPlayer.setMediaItem(mediaItem)
                exoPlayer.playWhenReady = playWhenReady
                if (playbackPosition > 0) {
                    exoPlayer.seekTo(currentItem, playbackPosition)
                } else if (currentMeta != null && currentMeta!!.progress > 0 && !currentMeta!!.isWatched) {
                    // Resume from database history if not already watched
                    exoPlayer.seekTo(currentItem, currentMeta!!.progress)
                }
                exoPlayer.prepare()

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
        player?.let { exoPlayer ->
            val position = exoPlayer.currentPosition
            val duration = exoPlayer.duration

            if (currentMeta != null && duration > 0) {
                viewModel.saveWatchProgress(currentMeta!!, position, duration)
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
