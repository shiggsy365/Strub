package com.example.stremiompvplayer

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.example.stremiompvplayer.databinding.ActivityPlayerBinding
// NOTE: Ensure your models package contains MetaItem and Stream classes
import com.example.stremiompvplayer.models.MetaItem
import com.example.stremiompvplayer.models.Stream

class PlayerActivity : AppCompatActivity() {

    // Removed the duplicate 'binding' declaration.
    private lateinit var binding: ActivityPlayerBinding

    // VARIABLES: Ensured these are present and correctly typed (MetaItem and Stream).
    // Assuming the intent passes a MetaItem object, not a simple Meta object.
    private var currentMeta: MetaItem? = null
    private var currentStreamUrl: String? = null
    private var nextEpisode: Stream? = null
    private var prevEpisode: Stream? = null

    private var player: ExoPlayer? = null
    private var currentStream: Stream? = null
    private var currentPosition: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // FIXED: Casting intent extras to the correct model types (Stream and MetaItem)
        currentStream = intent.getSerializableExtra("stream") as? Stream
        currentMeta = intent.getSerializableExtra("meta") as? MetaItem

        if (currentStream == null) {
            Log.e("PlayerActivity", "No stream data provided")
            finish()
            return
        }

        // FIXED: Using binding.title/subtitle (TextViews in the custom overlay)
        binding.titleTextView.text = currentMeta?.name ?: currentStream?.name
        binding.titleTextView.text = currentStream?.title
    }

    @OptIn(UnstableApi::class)
    private fun initializePlayer() {
        player = ExoPlayer.Builder(this).build()

        binding.playerView.player = player

        player?.addListener(object : Player.Listener {

            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_BUFFERING -> {
                        // FIXED ID: Using progressBar, which we added to the XML.
                        binding.loadingProgress.visibility = View.VISIBLE
                    }
                    Player.STATE_READY -> {
                        binding.loadingProgress.visibility = View.GONE
                    }
                    Player.STATE_ENDED -> {}
                    Player.STATE_IDLE -> {}
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

        // FIXED ID: Ensuring these button IDs match the XML.
        // Assuming your custom control view has nextEpisode and prevEpisode buttons.
        // If these are actually views in activity_player.xml, they must be defined in the XML.
        // For now, we assume they are defined in the external controller layout.

        // binding.nextEpisode.setOnClickListener { /* TODO */ }
        // binding.prevEpisode.setOnClickListener { /* TODO */ }
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
            Log.d("PlayerActivity", "Saving position: $currentPosition")

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
        // This is a deprecated method, but keeping the original code structure.
        binding.root.systemUiVisibility = (View.SYSTEM_UI_FLAG_LOW_PROFILE
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION)
    }
}