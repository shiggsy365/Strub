package com.example.stremiompvplayer

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
// Removed androidx.annotation.OptIn and all androidx.media3 imports
import com.example.stremiompvplayer.databinding.ActivityPlayerBinding
// NOTE: Ensure your models package contains MetaItem and Stream classes
import com.example.stremiompvplayer.models.MetaItem
import com.example.stremiompvplayer.models.Stream

class PlayerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPlayerBinding

    // VARIABLES: Ensured these are present and correctly typed (MetaItem and Stream).
    private var currentMeta: MetaItem? = null
    private var currentStreamUrl: String? = null
    private var nextEpisode: Stream? = null
    private var prevEpisode: Stream? = null

    // MPV Player State & Placeholder
    private var mpvPlayer: Any? = null // Placeholder for the MPV controller object
    private var isMpvInitialized: Boolean = false
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
        // NOTE: The second line overwrites the first, suggesting the intent is to show stream title.
        binding.titleTextView.text = currentStream?.title

        // Hide ExoPlayer controls placeholder if not using a custom MPV control set
        // binding.playerView.useController = false
    }

    // New MPV Initialization function replacing initializePlayer()
    private fun initializeMpvPlayer() {
        if (isMpvInitialized) return

        // --- MPV SETUP PLACEHOLDERS ---
        // TODO: 1. Integrate the MPV Android binding library.
        // TODO: 2. Initialize the MPV context (e.g., MpvController.create(this)).
        // mpvPlayer = MpvController.create(this)

        // TODO: 3. Attach MPV to the surface view (binding.playerView.videoSurface).
        // mpvPlayer?.setSurface(binding.playerView.videoSurface)

        // TODO: 4. Implement event listeners for loading/buffering/errors
        // (Use MPV events to set binding.loadingProgress visibility).

        // TODO: 5. Load media item
        val url = currentStream?.url ?: ""
        // mpvPlayer?.command(arrayOf("loadfile", url))

        // Start playback (if not automatic)
        // mpvPlayer?.play()

        isMpvInitialized = true
        Log.d("PlayerActivity", "MPV Player initialization placeholder executed.")
        binding.loadingProgress.visibility = View.VISIBLE // Assume loading starts immediately
        // binding.playerView.player = null // Ensure no lingering ExoPlayer reference
        // --- END MPV SETUP PLACEHOLDERS ---
    }

    // New MPV Release function replacing releasePlayer()
    private fun releaseMpvPlayer() {
        if (!isMpvInitialized) return

        // --- MPV RELEASE PLACEHOLDERS ---
        // TODO: 1. Save playback position (mpvPlayer?.getProperty("time-pos")?.toLong() * 1000)
        // currentPosition = mpvPlayer?.timePosition ?: 0

        // TODO: 2. Stop playback and release resources
        // mpvPlayer?.command(arrayOf("stop"))
        // mpvPlayer?.destroy()

        isMpvInitialized = false
        Log.d("PlayerActivity", "Saving position: $currentPosition")
        // --- END MPV RELEASE PLACEHOLDERS ---
    }


    public override fun onStart() {
        super.onStart()
        // Use the new MPV initializer
        initializeMpvPlayer()
    }

    public override fun onResume() {
        super.onResume()
        if (!isMpvInitialized) {
            initializeMpvPlayer()
        }
        // mpvPlayer?.play() // Resume playback if MPV is initialized
    }

    public override fun onPause() {
        super.onPause()
        // Save position and release resources
        releaseMpvPlayer()
    }

    public override fun onStop() {
        super.onStop()
        // Ensure resources are fully released
        releaseMpvPlayer()
    }

    // The deprecated system UI hiding logic remains the same
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