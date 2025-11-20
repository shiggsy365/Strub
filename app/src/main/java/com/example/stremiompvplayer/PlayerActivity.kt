package com.example.stremiompvplayer

import android.os.Bundle
import android.util.Log
import android.view.SurfaceHolder
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.stremiompvplayer.databinding.ActivityPlayerBinding
import com.example.stremiompvplayer.models.MetaItem
import com.example.stremiompvplayer.models.Stream
// IMPORT the wrapper we created
import `is`.xyz.mpv.MPVLib

class PlayerActivity : AppCompatActivity(), MPVLib.EventObserver, SurfaceHolder.Callback {

    private lateinit var binding: ActivityPlayerBinding

    // Data variables
    private var currentMeta: MetaItem? = null
    private var currentStream: Stream? = null

    private var isMpvInitialized: Boolean = false
    // We don't hold a direct object ref because MPVLib is a static singleton object in this pattern

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        currentStream = intent.getSerializableExtra("stream") as? Stream
        currentMeta = intent.getSerializableExtra("meta") as? MetaItem

        if (currentStream == null) {
            Log.e("PlayerActivity", "No stream data provided")
            finish()
            return
        }

        binding.titleTextView.text = currentStream?.title ?: "Unknown Stream"

        // Initialize SurfaceView callback
        binding.videoSurface.holder.addCallback(this)
    }

    private fun initializeMpvPlayer() {
        if (isMpvInitialized) return

        try {
            // 1. Create and Init MPV
            MPVLib.create(this)
            MPVLib.setOptionString("vo", "gpu")
            MPVLib.setOptionString("gpu-context", "android")
            MPVLib.init()

            // 2. Attach Observer
            MPVLib.addObserver(this)
            MPVLib.observeProperty("time-pos", MPVLib.MPV_EVENT_NONE)
            MPVLib.observeProperty("duration", MPVLib.MPV_EVENT_NONE)

            // 3. Load File
            val url = currentStream?.url ?: ""
            if (url.isNotEmpty()) {
                Log.d("PlayerActivity", "Loading URL: $url")
                MPVLib.command(arrayOf("loadfile", url))
            }

            isMpvInitialized = true

        } catch (e: Exception) {
            Log.e("PlayerActivity", "Failed to initialize MPV: ${e.message}")
            binding.errorText.text = "Error initializing player: ${e.message}"
            binding.errorText.visibility = View.VISIBLE
        }
    }

    private fun releaseMpvPlayer() {
        if (!isMpvInitialized) return

        MPVLib.removeObserver(this)
        MPVLib.destroy()
        isMpvInitialized = false
    }

    // SurfaceHolder.Callback implementation
    override fun surfaceCreated(holder: SurfaceHolder) {
        Log.d("PlayerActivity", "Surface created")
        if (!isMpvInitialized) initializeMpvPlayer()
        // Attach the surface to MPV
        MPVLib.attachSurface(holder.surface)
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        // MPV handles surface changes dynamically usually, but you can pass config here if needed
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        Log.d("PlayerActivity", "Surface destroyed")
        MPVLib.detachSurface()
    }

    // MPVLib.EventObserver implementation
    override fun event(eventId: Int) {
        runOnUiThread {
            when (eventId) {
                MPVLib.MPV_EVENT_PLAYBACK_RESTART -> {
                    // Video actually started playing
                    binding.loadingProgress.visibility = View.GONE
                }
                MPVLib.MPV_EVENT_END_FILE -> {
                    finish() // Close player when video ends
                }
            }
        }
    }
    override fun eventProperty(property: String) {
        // Optional: Handle properties with no value if needed
    }
    override fun eventProperty(property: String, value: Long) {
        // Handle time/duration updates
    }
    override fun eventProperty(property: String, value: Double) {
        // Optional: Handle double values (like precise time) here if needed
    }
    override fun eventProperty(property: String, value: Boolean) {}
    override fun eventProperty(property: String, value: String) {}

    override fun onResume() {
        super.onResume()
        // If initialized, MPV usually keeps playing or we can send "play" command
        if (isMpvInitialized) {
            MPVLib.setPropertyString("pause", "no")
        }
    }

    override fun onPause() {
        super.onPause()
        if (isMpvInitialized) {
            MPVLib.setPropertyString("pause", "yes")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        releaseMpvPlayer()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemUi()
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