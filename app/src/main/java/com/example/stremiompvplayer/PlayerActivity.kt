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
    }

    public override fun onStart() {
        super.onStart()
        if (android.os.Build.VERSION.SDK_INT > 23) {
            initializePlayer()
        }
    }

    public override fun onResume() {
        super.onResume()
        hideSystemUi()
        if (android.os.Build.VERSION.SDK_INT <= 23 || player == null) {
            initializePlayer()
        }
    }



    @OptIn(UnstableApi::class) private fun initializePlayer() {
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
                    // (Ideally pass specific progress via Intent to avoid DB call on main thread here,
                    // or use the modified MetaItem which now has progress)
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
                        if (isPlaying) {
                            // Keep screen on logic handled by flag, but can do extra UI updates here
                        }
                    }
                })
            }
    }

    private fun releasePlayer() {
        player?.let { exoPlayer ->
            playbackPosition = exoPlayer.currentPosition
            currentItem = exoPlayer.currentMediaItemIndex
            playWhenReady = exoPlayer.playWhenReady
            exoPlayer.release()
        }
        player = null
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

    override fun onPause() {
        super.onPause()
        saveProgress() // Save whenever we pause or leave
        if (android.os.Build.VERSION.SDK_INT <= 23) {
            releasePlayer()
        }
    }

    override fun onStop() {
        super.onStop()
        saveProgress() // Save whenever we stop
        if (android.os.Build.VERSION.SDK_INT > 23) {
            releasePlayer()
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