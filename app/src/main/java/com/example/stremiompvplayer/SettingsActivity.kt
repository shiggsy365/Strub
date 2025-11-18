package com.example.stremiompvplayer

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.stremiompvplayer.adapters.AddonListAdapter
// REMOVED UNUSED IMPORTS: AppDatabase, CoroutineScope, Dispatchers, Snackbar, ItemAddonListBinding
import com.example.stremiompvplayer.databinding.ActivitySettingsBinding
import com.example.stremiompvplayer.utils.SharedPreferencesManager
import com.example.stremiompvplayer.utils.UserSettings
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class SettingsActivity : AppCompatActivity() {

    private lateinit var prefsManager: SharedPreferencesManager
    private lateinit var binding: ActivitySettingsBinding
    private lateinit var settings: UserSettings

    private lateinit var addonAdapter: AddonListAdapter
    private var currentUserId: String? = null

    // NOTE: The code assumes the following IDs from the layout are correct:
    // autoPlaySwitch, subtitlesEnabledSwitch, subtitleSizeSeekBar, subtitleSizeValue,
    // colorWhite, colorYellow, colorCyan, colorGreen, subtitlePreview, addonsRecycler, noAddonsText, addAddonButton, switchUserButton, signOutButton

    private fun setupAddonsList() {
        currentUserId?.let { userId ->
            // FIXED: Call manager directly
            val addons = prefsManager.getUserAddonUrls()

            if (addons.isEmpty()) {
                binding.addonsRecycler.visibility = View.GONE
                binding.noAddonsText.visibility = View.VISIBLE
            } else {
                binding.addonsRecycler.visibility = View.VISIBLE
                binding.noAddonsText.visibility = View.GONE

                addonAdapter = AddonListAdapter(addons.toMutableList()) { addonUrl ->
                    MaterialAlertDialogBuilder(this)
                        .setTitle("Remove Addon")
                        .setMessage("Are you sure you want to remove this addon?")
                        .setPositiveButton("Remove") { _, _ ->
                            // FIXED: Call manager directly
                            prefsManager.removeAddonUrl(addonUrl)
                            setupAddonsList()
                            Toast.makeText(this, "Addon removed", Toast.LENGTH_SHORT).show()
                        }
                        .setNegativeButton("Cancel", null)
                        .show()
                }

                binding.addonsRecycler.apply {
                    layoutManager = LinearLayoutManager(this@SettingsActivity)
                    adapter = addonAdapter
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Initialization must happen before super.onCreate if using applicationContext in getInstance
        prefsManager = SharedPreferencesManager.getInstance(this)
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // FIXED: Use prefsManager for user ID
        currentUserId = prefsManager.getCurrentUserId()

        if (currentUserId == null) {
            Toast.makeText(this, "No user logged in", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // FIXED: Use prefsManager for settings
        settings = prefsManager.getUserSettings()

        setupToolbar()
        loadSettings()
        setupListeners()
        setupAddonsList()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun loadSettings() {
        // Playback settings
        binding.autoPlaySwitch.isChecked = settings.autoPlayFirstStream

        // Subtitle settings
        binding.subtitlesEnabledSwitch.isChecked = settings.subtitlesEnabled
        binding.subtitleSizeSeekBar.progress = settings.subtitleSize
        binding.subtitleSizeValue.text = "${settings.subtitleSize}sp"

        updateSubtitlePreview()
    }

    private fun setupListeners() {
        // Auto-play toggle
        binding.autoPlaySwitch.setOnCheckedChangeListener { _, isChecked ->
            // FIXED: The 'copy' method now correctly uses the data class properties
            settings = settings.copy(autoPlayFirstStream = isChecked)
            saveSettings()
        }

        // Subtitles enabled toggle
        binding.subtitlesEnabledSwitch.setOnCheckedChangeListener { _, isChecked ->
            // FIXED: The 'copy' method now correctly uses the data class properties
            settings = settings.copy(subtitlesEnabled = isChecked)
            saveSettings()
            updateSubtitlePreview()
        }

        // Subtitle size
        binding.subtitleSizeSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                binding.subtitleSizeValue.text = "${progress}sp"
                settings = settings.copy(subtitleSize = progress)
                updateSubtitlePreview()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                saveSettings()
            }
        })

        // Subtitle color buttons
        // FIXED: Using Color constants (Ints) as required by the UserSettings data class.
        binding.colorWhite.setOnClickListener {
            settings = settings.copy(subtitleColor = Color.WHITE)
            saveSettings()
            updateSubtitlePreview()
        }

        binding.colorYellow.setOnClickListener {
            settings = settings.copy(subtitleColor = Color.YELLOW)
            saveSettings()
            updateSubtitlePreview()
        }

        binding.colorCyan.setOnClickListener {
            settings = settings.copy(subtitleColor = Color.CYAN)
            saveSettings()
            updateSubtitlePreview()
        }

        binding.colorGreen.setOnClickListener {
            settings = settings.copy(subtitleColor = Color.GREEN)
            saveSettings()
            updateSubtitlePreview()
        }

        // Addon management
        binding.addAddonButton.setOnClickListener {
            showAddAddonDialog()
        }

        // Account
        binding.switchUserButton.setOnClickListener {
            val intent = Intent(this, UserSelectionActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        binding.signOutButton.setOnClickListener {
            MaterialAlertDialogBuilder(this)
                .setTitle("Sign Out")
                .setMessage("Are you sure you want to sign out?")
                .setPositiveButton("Sign Out") { _, _ ->
                    // FIXED: Use prefsManager
                    prefsManager.setCurrentUser("")
                    val intent = Intent(this, UserSelectionActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun updateSubtitlePreview() {
        binding.subtitlePreview.apply {
            // FIXED: textSize is now correctly a Float
            textSize = settings.subtitleSize.toFloat()
            // FIXED: Using settings.subtitleColor directly (an Int Color)
            setTextColor(settings.subtitleColor)
            alpha = if (settings.subtitlesEnabled) 1.0f else 0.3f
        }
    }

    private fun saveSettings() {
        // FIXED: Call manager directly
        prefsManager.saveUserSettings(settings)
        Log.d("SettingsActivity", "Settings saved: $settings")
    }

    private fun showAddAddonDialog() {
        val input = EditText(this).apply {
            hint = "Addon URL (e.g., https://v3-cinemeta.strem.io/manifest.json)"
            setPadding(48, 32, 48, 32)
            textSize = 16f
            setTextColor(Color.WHITE)
            setHintTextColor(Color.GRAY)
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("Add Stremio Addon")
            .setMessage("Enter the full URL of the Stremio addon manifest.json")
            .setView(input)
            .setPositiveButton("Add") { dialog, _ ->
                val url = input.text.toString().trim()
                Log.d("SettingsActivity", "Attempting to add addon: $url")

                if (url.isEmpty()) {
                    Toast.makeText(this, "Please enter a URL", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                if (!url.startsWith("http://") && !url.startsWith("https://")) {
                    Toast.makeText(this, "URL must start with http:// or https://", Toast.LENGTH_LONG).show()
                    return@setPositiveButton
                }

                if (!url.contains("manifest.json")) {
                    Toast.makeText(this, "URL should contain manifest.json", Toast.LENGTH_LONG).show()
                    return@setPositiveButton
                }

                currentUserId?.let { userId ->
                    try {
                        // FIXED: Call manager directly
                        prefsManager.addAddonUrl(url)
                        Log.d("SettingsActivity", "Addon added successfully: $url")
                        Toast.makeText(this, "Addon added successfully!", Toast.LENGTH_LONG).show()
                        setupAddonsList()

                        dialog.dismiss()

                    } catch (e: Exception) {
                        Log.e("SettingsActivity", "Error adding addon", e)
                        Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.cancel()
            }
            .show()
    }
}