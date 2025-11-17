package com.example.stremiompvplayer

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.example.stremiompvplayer.databinding.ActivityPlayerBinding

class PlayerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPlayerBinding
    private var exoPlayer: ExoPlayer? = null
    private val handler = Handler(Looper.getMainLooper())
    private var isControlsVisible = false
    private var streamUrl: String? = null
    private var streamTitle: String? = null

    private val hideControlsRunnable = Runnable {
        hideControls()
    }

    private val updateProgressRunnable = object : Runnable {
        override fun run() {
            updateProgress()
            handler.postDelayed(this, 1000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        streamUrl = intent.getStringExtra("STREAM_URL")
        streamTitle = intent.getStringExtra("STREAM_TITLE")

        binding.titleTextView.text = streamTitle ?: "Playing Stream"

        setupExoPlayer()
        setupControls()
    }

    private fun setupExoPlayer() {
        exoPlayer = ExoPlayer.Builder(this).build().apply {
            // Set the player to the PlayerView
            binding.playerView.player = this

            // Add listener for playback events
            addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    when (playbackState) {
                        Player.STATE_BUFFERING -> {
                            binding.loadingProgress.visibility = View.VISIBLE
                        }
                        Player.STATE_READY -> {
                            binding.loadingProgress.visibility = View.GONE
                            showControls()
                        }
                        Player.STATE_ENDED -> {
                            finish()
                        }
                    }
                }

                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    updatePlayPauseButton(isPlaying)
                }

                override fun onPlayerError(error: PlaybackException) {
                    showError(error.message ?: "Playback error occurred")
                }
            })

            // Load and play the stream
            streamUrl?.let { url ->
                val mediaItem = MediaItem.fromUri(url)
                setMediaItem(mediaItem)
                prepare()
                play()
            }
        }

        // Use ExoPlayer's built-in controls initially
        binding.playerView.useController = true
        binding.playerView.controllerShowTimeoutMs = 5000
    }

    private fun setupControls() {
        // Optional: Add custom controls if needed
        // For now, ExoPlayer's built-in controls work great

        binding.playerView.setOnClickListener {
            toggleControls()
        }
    }

    private fun toggleControls() {
        if (binding.playerView.isControllerFullyVisible) {
            binding.playerView.hideController()
        } else {
            binding.playerView.showController()
        }
    }

    private fun showControls() {
        binding.playerView.showController()
    }

    private fun hideControls() {
        binding.playerView.hideController()
    }

    private fun updateProgress() {
        exoPlayer?.let {
            val position = (it.currentPosition / 1000).toInt()
            val duration = if (it.duration > 0) (it.duration / 1000).toInt() else 0

            binding.currentTimeText.text = formatTime(position)
            binding.durationText.text = formatTime(duration)
        }
    }

    private fun updatePlayPauseButton(playing: Boolean) {
        // ExoPlayer's UI handles this automatically
        // This is here for compatibility if you add custom controls
    }

    private fun formatTime(seconds: Int): String {
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val secs = seconds % 60

        return if (hours > 0) {
            String.format("%d:%02d:%02d", hours, minutes, secs)
        } else {
            String.format("%02d:%02d", minutes, secs)
        }
    }

    private fun showError(message: String) {
        binding.loadingProgress.visibility = View.GONE
        binding.errorText.visibility = View.VISIBLE
        binding.errorText.text = message
    }

    override fun onResume() {
        super.onResume()
        exoPlayer?.play()
        handler.post(updateProgressRunnable)
    }

    override fun onPause() {
        super.onPause()
        exoPlayer?.pause()
        handler.removeCallbacks(updateProgressRunnable)
        handler.removeCallbacks(hideControlsRunnable)
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(updateProgressRunnable)
        handler.removeCallbacks(hideControlsRunnable)
        exoPlayer?.release()
        exoPlayer = null
    }
}