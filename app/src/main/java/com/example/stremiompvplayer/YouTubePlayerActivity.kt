package com.example.stremiompvplayer

import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.stremiompvplayer.databinding.ActivityYoutubePlayerBinding
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer
import android.net.Uri

class YouTubePlayerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityYoutubePlayerBinding
    private var libVLC: LibVLC? = null
    private var vlcPlayer: MediaPlayer? = null
    private var youtubeKey: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityYoutubePlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Keep screen on
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Hide system UI for immersive experience
        hideSystemUi()

        youtubeKey = intent.getStringExtra("YOUTUBE_KEY")
        val title = intent.getStringExtra("TITLE") ?: "Trailer"

        if (youtubeKey == null) {
            finish()
            return
        }

        binding.trailerTitle.text = title

        // Hide WebView, show VLC surface
        binding.webView.visibility = View.GONE
        binding.vlcSurfaceView.visibility = View.VISIBLE

        initializeVLCPlayer()
    }

    private fun initializeVLCPlayer() {
        try {
            // Create LibVLC instance with YouTube-optimized options
            val options = arrayListOf(
                "--network-caching=1500",
                "--live-caching=1500",
                "--no-drop-late-frames",
                "--no-skip-frames"
            )

            libVLC = LibVLC(this, options)
            vlcPlayer = MediaPlayer(libVLC)

            // Attach to surface view
            vlcPlayer?.attachViews(binding.vlcSurfaceView, null, false, false)

            // Create YouTube URL from video key
            val youtubeUrl = "https://www.youtube.com/watch?v=$youtubeKey"
            Log.d("YouTubePlayer", "Playing YouTube URL: $youtubeUrl")

            // VLC can handle YouTube URLs directly
            val media = Media(libVLC, Uri.parse(youtubeUrl))
            vlcPlayer?.media = media
            media.release()

            // Add event listener
            vlcPlayer?.setEventListener { event ->
                when (event.type) {
                    MediaPlayer.Event.Playing -> {
                        binding.loadingProgress.visibility = View.GONE
                        Log.d("YouTubePlayer", "VLC: Playing YouTube video")
                    }
                    MediaPlayer.Event.Buffering -> {
                        binding.loadingProgress.visibility = View.VISIBLE
                    }
                    MediaPlayer.Event.EncounteredError -> {
                        Log.e("YouTubePlayer", "VLC error playing YouTube video")
                        runOnUiThread {
                            Toast.makeText(
                                this,
                                "Unable to play trailer. YouTube may have blocked playback.",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                    MediaPlayer.Event.EndReached -> {
                        Log.d("YouTubePlayer", "VLC: Video ended")
                        runOnUiThread { finish() }
                    }
                }
            }

            // Start playback
            vlcPlayer?.play()

            Log.i("YouTubePlayer", "VLC YouTube player initialized successfully")

        } catch (e: Exception) {
            Log.e("YouTubePlayer", "Error initializing VLC for YouTube", e)
            Toast.makeText(
                this,
                "Failed to play trailer: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun hideSystemUi() {
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            or View.SYSTEM_UI_FLAG_FULLSCREEN
            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        )
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        vlcPlayer?.release()
        libVLC?.release()
    }
}
