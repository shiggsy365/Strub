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
import com.example.stremiompvplayer.data.AppDatabase
import com.example.stremiompvplayer.databinding.ActivitySettingsBinding
import com.example.stremiompvplayer.models.UserSettings
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var database: AppDatabase
    private lateinit var settings: UserSettings
    private var currentUserId: String? = null


    private fun setupAddonsList() {
        currentUserId?.let { userId ->
            val addons = database.getUserAddonUrls(userId)

            if (addons.isEmpty()) {
                binding.addonsRecycler.visibility = View.GONE
                binding.noAddonsText.visibility = View.VISIBLE
            } else {
                binding.addonsRecycler.visibility = View.VISIBLE
                binding.noAddonsText.visibility = View.GONE

                val adapter = AddonListAdapter(addons.toMutableList()) { addonUrl ->
                    // Remove addon
                    database.removeAddonUrl(userId, addonUrl)
                    setupAddonsList() // Refresh list
                    Toast.makeText(this, "Addon removed", Toast.LENGTH_SHORT).show()
                }

                binding.addonsRecycler.apply {
                    layoutManager = LinearLayoutManager(this@SettingsActivity)
                    this.adapter = adapter
                }
            }
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        database = AppDatabase.getInstance(this)
        currentUserId = database.getCurrentUserId()

        if (currentUserId == null) {
            Toast.makeText(this, "No user logged in", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        settings = database.getUserSettings(currentUserId!!)

        setupToolbar()
        loadSettings()
        setupListeners()
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
        loadAddons()
    }

    private fun setupListeners() {
        // Auto-play toggle
        binding.autoPlaySwitch.setOnCheckedChangeListener { _, isChecked ->
            settings = settings.copy(autoPlayFirstStream = isChecked)
            saveSettings()
        }

        // Subtitles enabled toggle
        binding.subtitlesEnabledSwitch.setOnCheckedChangeListener { _, isChecked ->
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
        binding.colorWhite.setOnClickListener {
            settings = settings.copy(subtitleColor = "#FFFFFF")
            saveSettings()
            updateSubtitlePreview()
        }

        binding.colorYellow.setOnClickListener {
            settings = settings.copy(subtitleColor = "#FFFF00")
            saveSettings()
            updateSubtitlePreview()
        }

        binding.colorCyan.setOnClickListener {
            settings = settings.copy(subtitleColor = "#00FFFF")
            saveSettings()
            updateSubtitlePreview()
        }

        binding.colorGreen.setOnClickListener {
            settings = settings.copy(subtitleColor = "#00FF00")
            saveSettings()
            updateSubtitlePreview()
        }

        // Addon management - FIXED!
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

        binding.addAddonButton.apply {
            textSize = 22f // Larger font
            setOnClickListener {
                showAddAddonDialog()
            }
        }

        binding.signOutButton.setOnClickListener {
            MaterialAlertDialogBuilder(this)
                .setTitle("Sign Out")
                .setMessage("Are you sure you want to sign out?")
                .setPositiveButton("Sign Out") { _, _ ->
                    database.setCurrentUser("")
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
            textSize = settings.subtitleSize.toFloat()
            setTextColor(Color.parseColor(settings.subtitleColor))
            alpha = if (settings.subtitlesEnabled) 1.0f else 0.3f
        }
    }

    private fun saveSettings() {
        database.saveUserSettings(settings)
        Log.d("SettingsActivity", "Settings saved: $settings")
    }

    private fun loadAddons() {
        currentUserId?.let { userId ->
            val addons = database.getUserAddonUrls(userId)
            Log.d("SettingsActivity", "Loaded ${addons.size} addons for user $userId")

            if (addons.isNotEmpty()) {
                // TODO: Show addons in a list
                Toast.makeText(this, "${addons.size} addon(s) configured", Toast.LENGTH_SHORT).show()
            }
        }
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
                        database.addAddonUrl(userId, url)
                        Log.d("SettingsActivity", "Addon added successfully: $url")
                        Toast.makeText(this, "Addon added successfully!", Toast.LENGTH_LONG).show()
                        loadAddons()

                        // Success - stay on this screen so user can add more or see the confirmation
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