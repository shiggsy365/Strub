package com.example.stremiompvplayer

import android.app.AlertDialog
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.KeyEvent
import android.view.SurfaceHolder
import android.view.View
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import com.example.stremiompvplayer.databinding.ActivityPlayerBinding
import com.example.stremiompvplayer.models.MetaItem
import com.example.stremiompvplayer.models.Stream
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import `is`.xyz.mpv.MPVLib
import java.util.concurrent.TimeUnit

class PlayerActivity : AppCompatActivity(), MPVLib.EventObserver, SurfaceHolder.Callback {

    private lateinit var binding: ActivityPlayerBinding
    private var currentMeta: MetaItem? = null
    private var currentStream: Stream? = null
    private var isMpvInitialized = false

    // OSD State
    private val osdHandler = Handler(Looper.getMainLooper())
    private val osdHideRunnable = Runnable { hideOSD() }
    private var isSeeking = false
    private var duration: Long = 0
    private var position: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        currentStream = intent.getSerializableExtra("stream") as? Stream
        currentMeta = intent.getSerializableExtra("meta") as? MetaItem

        if (currentStream == null) {
            finish()
            return
        }

        binding.titleTextView.text = currentStream?.title ?: currentMeta?.name ?: "Video"
        binding.videoSurface.holder.addCallback(this)

        setupControls()
    }

    private fun setupControls() {
        // Play/Pause Button
        binding.btnPlayPause.setOnClickListener { togglePlayback() }

        // Stop Button
        binding.btnStop.setOnClickListener {
            MPVLib.command(arrayOf("stop"))
            finish()
        }

        // Audio Track Selection
        binding.btnAudio.setOnClickListener { showTrackDialog("audio") }

        // Subtitle Selection
        binding.btnSubtitle.setOnClickListener { showTrackDialog("sub") }

        // Seek Bar Logic
        binding.playerSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    // Update time text while dragging
                    binding.tvCurrentTime.text = formatTime(progress.toLong())
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                isSeeking = true
                showOSD() // Keep OSD showing while seeking
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                // Perform the seek when user releases
                val target = seekBar?.progress?.toDouble() ?: 0.0
                MPVLib.command(arrayOf("seek", target.toString(), "absolute"))
                isSeeking = false
                showOSD() // Reset timer
            }
        })

        // Show OSD on touch
        binding.playerContainer.setOnClickListener {
            if (binding.osdOverlay.visibility == View.VISIBLE) {
                hideOSD()
            } else {
                showOSD()
            }
        }
    }

    private fun showTrackDialog(type: String) {
        // 1. Fetch track list from MPV
        val trackListJson = MPVLib.getPropertyString("track-list") ?: return

        // 2. Parse JSON using Gson
        val gson = Gson()
        val listType = object : TypeToken<List<MpvTrack>>() {}.type
        val tracks: List<MpvTrack> = gson.fromJson(trackListJson, listType)

        // 3. Filter for requested type (audio or sub)
        val filtered = tracks.filter { it.type == type }

        if (filtered.isEmpty()) {
            MPVLib.command(arrayOf("show-text", "No $type tracks found"))
            return
        }

        // 4. Build Dialog Items
        val items = filtered.map { track ->
            val idStr = track.id.toString()
            val lang = track.lang ?: "unk"
            val title = track.title ?: "Track $idStr"
            val selectedMark = if (track.selected) " [x]" else ""
            "$idStr: $lang - $title$selectedMark"
        }.toTypedArray()

        // 5. Show Dialog
        AlertDialog.Builder(this)
            .setTitle("Select ${if(type == "sub") "Subtitle" else "Audio"}")
            .setItems(items) { _, which ->
                val selectedTrack = filtered[which]
                val propertyName = if (type == "sub") "sid" else "aid"
                MPVLib.setPropertyInt(propertyName, selectedTrack.id)
                MPVLib.command(arrayOf("show-text", "Selected: ${items[which]}"))
            }
            .setNegativeButton("Cancel", null)
            .setNeutralButton("Disable") { _, _ ->
                val propertyName = if (type == "sub") "sid" else "aid"
                MPVLib.setPropertyString(propertyName, "no")
            }
            .show()
    }

    // --- OSD VISIBILITY ---
    private fun showOSD() {
        binding.osdOverlay.visibility = View.VISIBLE
        binding.osdOverlay.animate().alpha(1f).setDuration(200).start()

        osdHandler.removeCallbacks(osdHideRunnable)
        osdHandler.postDelayed(osdHideRunnable, 4000) // Auto hide after 4s

        // Request focus for D-pad navigation
        binding.btnPlayPause.requestFocus()
    }

    private fun hideOSD() {
        if (!isSeeking) { // Don't hide if user is dragging seekbar
            binding.osdOverlay.animate().alpha(0f).setDuration(300).withEndAction {
                binding.osdOverlay.visibility = View.GONE
            }.start()
        }
    }

    private fun togglePlayback() {
        val paused = MPVLib.getPropertyString("pause") == "yes"
        MPVLib.setPropertyString("pause", if (paused) "no" else "yes")
        updatePlayPauseIcon(!paused)
        showOSD()
    }

    private fun updatePlayPauseIcon(isPaused: Boolean) {
        val icon = if (isPaused) android.R.drawable.ic_media_play else android.R.drawable.ic_media_pause
        binding.btnPlayPause.setImageResource(icon)

        // Flash center icon
        binding.centerStateIcon.setImageResource(icon)
        binding.centerStateIcon.alpha = 1f
        binding.centerStateIcon.visibility = View.VISIBLE
        binding.centerStateIcon.animate().alpha(0f).setDuration(500).withEndAction {
            binding.centerStateIcon.visibility = View.GONE
        }.start()
    }
    private fun initializeMpvPlayer() {
          if (isMpvInitialized) return

                try {
                    // 1. Create MPV Context
                    MPVLib.create(this)

                    // --- VIDEO OUTPUT & HARDWARE DECODING ---
                    MPVLib.setOptionString("vo", "gpu")
                    MPVLib.setOptionString("gpu-context", "android")
                    // 'mediacodec-copy' is generally most stable for Android TV, 'auto' is also good
                    MPVLib.setOptionString("hwdec", "auto")

                    // --- BUFFERING & CACHE SETTINGS ---
                    // Enable cache
                    MPVLib.setOptionString("cache", "yes")
                    // Allocate up to 128MB for cache (adjust based on device RAM, 64-128 is safe)
                    MPVLib.setOptionString("demuxer-max-bytes", "128MiB")
                    // Try to read 20 seconds ahead
                    MPVLib.setOptionString("demuxer-readahead-secs", "20")
                    // Stop buffering when 150MB is reached
                    MPVLib.setOptionString("demuxer-max-back-bytes", "150MiB")

                    // --- NETWORK SETTINGS ---
                    // Allow seeking even if data isn't fully downloaded
                    MPVLib.setOptionString("force-seekable", "yes")

                    // 2. Initialize
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

    override fun eventProperty(property: String, value: Long) { /* unused for time */ }
    override fun eventProperty(property: String, value: Boolean) {
        if (property == "pause") runOnUiThread { updatePlayPauseIcon(value) }
    }
    override fun eventProperty(property: String, value: String) { /* unused */ }
    override fun eventProperty(property: String) { } // For empty events

    // IMPORTANT: MPV sends time as Double (seconds)
    override fun eventProperty(property: String, value: Double) {
        runOnUiThread {
            if (property == "duration") {
                duration = value.toLong()
                binding.playerSeekBar.max = duration.toInt()
                binding.tvDuration.text = formatTime(duration)
            } else if (property == "time-pos") {
                position = value.toLong()
                if (!isSeeking) {
                    binding.playerSeekBar.progress = position.toInt()
                    binding.tvCurrentTime.text = formatTime(position)
                }
            }
        }
    }

    override fun event(eventId: Int) {
        if (eventId == MPVLib.MPV_EVENT_PLAYBACK_RESTART) {
            runOnUiThread { binding.loadingProgress.visibility = View.GONE }
        }

        if (eventId == MPVLib.MPV_EVENT_END_FILE) {
            // Fetch the reason why playback ended
            val reason = MPVLib.getPropertyString("end-file-reason") ?: "unknown"
            val error = MPVLib.getPropertyString("error") ?: "no error"

            Log.e("PlayerActivity", "Playback Ended. Reason: $reason, Error: $error")

            // OPTIONAL: Comment out 'finish()' temporarily to read the logs if it happens too fast
            runOnUiThread { finish() }
        }
    }

    private fun formatTime(seconds: Long): String {
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val secs = seconds % 60
        return if (hours > 0) {
            String.format("%02d:%02d:%02d", hours, minutes, secs)
        } else {
            String.format("%02d:%02d", minutes, secs)
        }
    }

    // --- KEY EVENTS (Remote Control) ---
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        showOSD() // Show OSD on any key press
        return when (keyCode) {
            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, KeyEvent.KEYCODE_MEDIA_PAUSE, KeyEvent.KEYCODE_MEDIA_PLAY -> {
                togglePlayback()
                true
            }
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                if (binding.osdOverlay.visibility == View.GONE) {
                    togglePlayback()
                } else {
                    super.onKeyDown(keyCode, event) // Allow button clicks
                }
                true
            }
            KeyEvent.KEYCODE_DPAD_LEFT -> {
                MPVLib.command(arrayOf("seek", "-10"))
                true
            }
            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                MPVLib.command(arrayOf("seek", "10"))
                true
            }
            else -> super.onKeyDown(keyCode, event)
        }
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        // 1. Initialize the player FIRST so the instance exists
        if (!isMpvInitialized) {
            initializeMpvPlayer()
        }

        // 2. THEN attach the surface to the created player
        MPVLib.attachSurface(holder.surface)
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}
    override fun surfaceDestroyed(holder: SurfaceHolder) {
        MPVLib.detachSurface()
    }

    override fun onPause() {
        super.onPause()
        MPVLib.setPropertyString("pause", "yes")
    }

    override fun onDestroy() {
        super.onDestroy()
        MPVLib.destroy()
    }

    // Data class for Gson parsing
    data class MpvTrack(
        val id: Int,
        val type: String,
        val lang: String?,
        val title: String?,
        val selected: Boolean
    )
}