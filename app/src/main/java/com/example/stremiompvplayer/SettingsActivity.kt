package com.example.stremiompvplayer

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.stremiompvplayer.adapters.CatalogConfigAdapter
import com.example.stremiompvplayer.data.AppDatabase
import com.example.stremiompvplayer.databinding.ActivitySettingsBinding
import com.example.stremiompvplayer.viewmodels.MainViewModel
import com.example.stremiompvplayer.viewmodels.MainViewModelFactory
import com.example.stremiompvplayer.data.ServiceLocator
import com.example.stremiompvplayer.utils.SharedPreferencesManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private val viewModel: MainViewModel by viewModels {
        MainViewModelFactory(ServiceLocator.getInstance(applicationContext))
    }
    private lateinit var adapter: CatalogConfigAdapter
    private lateinit var prefsManager: SharedPreferencesManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefsManager = SharedPreferencesManager.getInstance(this)

        setupUserSection()
        setupTMDBSection()
        setupCatalogList()
    }

    private fun setupTMDBSection() {
        // Display current TMDB token status
        updateTMDBTokenDisplay()

        binding.btnConfigureTMDB.setOnClickListener {
            showTMDBTokenDialog()
        }
    }

    private fun updateTMDBTokenDisplay() {
        val token = prefsManager.getTMDBAccessToken()
        if (token.isNullOrEmpty()) {
            binding.tvTMDBStatus.text = "TMDB Access Token: Not configured"
            binding.tvTMDBStatus.setTextColor(getColor(android.R.color.holo_red_light))
        } else {
            val maskedToken = "••••${token.takeLast(4)}"
            binding.tvTMDBStatus.text = "TMDB Access Token: $maskedToken"
            binding.tvTMDBStatus.setTextColor(getColor(android.R.color.holo_green_light))
        }
    }

    private fun showTMDBTokenDialog() {
        val input = TextInputEditText(this).apply {
            hint = "TMDB Access Token"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            setText(prefsManager.getTMDBAccessToken() ?: "")
            setPadding(48, 32, 48, 32)
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("Configure TMDB Access Token")
            .setMessage("Enter your TMDB Access Token (Read Access Token from TMDB API settings)")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                val token = input.text.toString().trim()
                if (token.isNotEmpty()) {
                    prefsManager.saveTMDBAccessToken(token)
                    updateTMDBTokenDisplay()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun setupCatalogList() {
        adapter = CatalogConfigAdapter(
            onUpdate = { catalog -> viewModel.updateCatalogConfig(catalog) },
            onMoveUp = { catalog, pos ->
                val list = adapter.currentList
                if (pos > 0) {
                    val prev = list[pos - 1]
                    viewModel.swapCatalogOrder(catalog, prev)
                }
            },
            onMoveDown = { catalog, pos ->
                val list = adapter.currentList
                if (pos < list.size - 1) {
                    val next = list[pos + 1]
                    viewModel.swapCatalogOrder(catalog, next)
                }
            }
        )

        binding.rvCatalogConfigs.layoutManager = LinearLayoutManager(this)
        binding.rvCatalogConfigs.adapter = adapter

        viewModel.initDefaultCatalogs()

        viewModel.allCatalogConfigs.observe(this) { list ->
            adapter.submitList(list)
        }
    }

    private fun setupUserSection() {
        val userId = prefsManager.getCurrentUserId()
        binding.tvCurrentUser.text = "Current User ID: $userId"

        binding.btnSwitchUser.setOnClickListener {
            startActivity(Intent(this, UserSelectionActivity::class.java))
        }

        binding.btnManageAddons.setOnClickListener {
            // Placeholder for existing addon logic
        }
    }
}