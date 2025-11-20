package com.example.stremiompvplayer

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.inputmethod.EditorInfo
import android.widget.LinearLayout
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.stremiompvplayer.adapters.CatalogConfigAdapter
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
        MainViewModelFactory(
            ServiceLocator.getInstance(applicationContext),
            SharedPreferencesManager.getInstance(this)
        )
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
        setupAIOStreamsSection()
        setupCatalogList()
    }

    // ... (setupTMDBSection, updateTMDBTokenDisplay, showTMDBTokenDialog remain the same) ...
    private fun setupTMDBSection() {
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
            .setMessage("Enter your TMDB Access Token")
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

    private fun setupAIOStreamsSection() {
        updateAIOStreamsDisplay()

        binding.btnConfigureAIOStreams.setOnClickListener {
            showAIOStreamsDialog()
        }
    }

    private fun updateAIOStreamsDisplay() {
        val username = prefsManager.getAIOStreamsUsername()
        val password = prefsManager.getAIOStreamsPassword()

        if (username.isNullOrEmpty() || password.isNullOrEmpty()) {
            binding.tvAIOStreamsStatus.text = "AIOStreams: Not configured"
            binding.tvAIOStreamsStatus.setTextColor(getColor(android.R.color.holo_red_light))
        } else {
            binding.tvAIOStreamsStatus.text = "AIOStreams: Configured (${username.take(8)}...)"
            binding.tvAIOStreamsStatus.setTextColor(getColor(android.R.color.holo_green_light))
        }
    }

    private fun showAIOStreamsDialog() {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 16, 48, 16)
        }

        // Input 2: Password
        val passwordInput = TextInputEditText(this).apply {
            id = 200
            hint = "Password"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            setText(prefsManager.getAIOStreamsPassword() ?: "")
            imeOptions = EditorInfo.IME_ACTION_DONE
            setSingleLine(true)
        }

        // Input 1: Username (UUID)
        val usernameInput = TextInputEditText(this).apply {
            id = 100
            hint = "Username (UUID)"
            inputType = InputType.TYPE_CLASS_TEXT
            setText(prefsManager.getAIOStreamsUsername() ?: "")
            imeOptions = EditorInfo.IME_ACTION_NEXT
            setSingleLine(true)
            nextFocusDownId = 200

            // Force focus jump on "Next"
            setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_NEXT) {
                    passwordInput.requestFocus()
                    true
                } else {
                    false
                }
            }
        }

        container.addView(usernameInput)
        container.addView(passwordInput)

        MaterialAlertDialogBuilder(this)
            .setTitle("Configure AIOStreams")
            .setMessage("Enter your AIOStreams credentials")
            .setView(container)
            .setPositiveButton("Save") { _, _ ->
                val username = usernameInput.text.toString().trim()
                val password = passwordInput.text.toString().trim()
                if (username.isNotEmpty() && password.isNotEmpty()) {
                    prefsManager.saveAIOStreamsUsername(username)
                    prefsManager.saveAIOStreamsPassword(password)
                    updateAIOStreamsDisplay()
                }
            }
            .setNegativeButton("Cancel", null)
            .setNeutralButton("Clear") { _, _ ->
                prefsManager.clearAIOStreamsCredentials()
                updateAIOStreamsDisplay()
            }
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
    }
}