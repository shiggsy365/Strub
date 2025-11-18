package com.example.stremiompvplayer

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
// FIX: Add all Media3 and OptIn imports
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
// FIX: Add databinding import
import com.example.stremiompvplayer.databinding.ActivityPlayerBinding
import com.example.stremiompvplayer.models.Meta
import com.example.stremiompvplayer.models.Stream

class PlayerActivity : AppCompatActivity() {

    // FIX: Use databinding
    private lateinit var binding: ActivityPlayerBinding
    private var player: ExoPlayer? = null
    private var currentStream: Stream? = null
    // ... existing code ...
    private var currentPosition: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // FIX: Use databinding
        binding = ActivityPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        currentStream = intent.getSerializableExtra("stream") as? Stream
        currentMeta = intent.getSerializableExtra("meta") as? Meta

        if (currentStream == null) {
            Log.e("PlayerActivity", "No stream data provided")
            finish()
            return
        }

        // FIX: Use binding
        binding.title.text = currentMeta?.name ?: currentStream?.name
        binding.subtitle.text = currentStream?.title
    }

    @OptIn(UnstableApi::class)
    private fun initializePlayer() {
        player = ExoPlayer.Builder(this).build()
        // FIX: Use binding
        binding.playerView.player = player

        player?.addListener(object : Player.Listener {
            // FIX: Correct override signature
            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_BUFFERING -> {
                        // FIX: Use binding
                        binding.progressBar.visibility = View.VISIBLE
                    }
                    Player.STATE_READY -> {
                        // FIX: Use binding
                        binding.progressBar.visibility = View.GONE
                    }
                    Player.STATE_ENDED -> {
                        // Handle end of playback
                    }
                    Player.STATE_IDLE -> {
                        // Handle idle state
                    }
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                // Handle play/pause state change
            }

            override fun onPlayerError(error: PlaybackException) {
                Log.e("PlayerActivity", "Player Error: ${error.message}", error)
            }
        })

        val mediaItem = MediaItem.fromUri(currentStream?.url ?: "")
        player?.setMediaItem(mediaItem)
        player?.prepare()
        player?.play()

        // FIX: Use binding
        binding.nextEpisode.setOnClickListener { /* TODO */ }
        binding.prevEpisode.setOnClickListener { /* TODO */ }
    }

    public override fun onStart() {
        super.onStart()
        initializePlayer()
    }

    @OptIn(UnstableApi::class)
    public override fun onResume() {
        super.onResume()
        if (player == null) {
            initializePlayer()
        }
    }

    @OptIn(UnstableApi::class)
    public override fun onPause() {
        super.onPause()
        currentPosition = player?.currentPosition ?: 0
        releasePlayer()
    }

    @OptIn(UnstableApi::class)
    public override fun onStop() {
        super.onStop()
        releasePlayer()
    }

    private fun releasePlayer() {
        player?.let {
            it.release()
            player = null
            // Save currentPosition to database here
            Log.d("PlayerActivity", "Saving position: $currentPosition")
            // FIX: Use binding
            binding.playerView.player = null
        }
    }

    // Handle window focus changes
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            hideSystemUi()
        }
    }

    private fun hideSystemUi() {
        // FIX: Use binding
        binding.root.systemUiVisibility = (View.SYSTEM_UI_FLAG_LOW_PROFILE
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION)
    }
}