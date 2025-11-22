package com.example.stremiompvplayer

import android.os.Bundle
import android.text.InputType
import android.view.inputmethod.EditorInfo
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.stremiompvplayer.adapters.CatalogConfigAdapter
import com.example.stremiompvplayer.data.ServiceLocator
import com.example.stremiompvplayer.databinding.ActivitySettingsBinding
import com.example.stremiompvplayer.utils.SharedPreferencesManager
import com.example.stremiompvplayer.viewmodels.MainViewModel
import com.example.stremiompvplayer.viewmodels.MainViewModelFactory
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
        setupObservers()
    }

    private fun setupObservers() {
        viewModel.requestToken.observe(this) { token ->
            if (token != null) {
                showAuthDialog(token)
            }
        }

        viewModel.sessionId.observe(this) { sessionId ->
            if (sessionId != null) {
                Toast.makeText(this, "TMDB Authorization Successful!", Toast.LENGTH_SHORT).show()
                updateTMDBTokenDisplay()
            }
        }

        viewModel.error.observe(this) { error ->
            if (error != null && error.contains("Auth")) {
                Toast.makeText(this, error, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun setupTMDBSection() {
        updateTMDBTokenDisplay()

        binding.btnConfigureTMDB.setOnClickListener {
            showApiKeyDialog()
        }

        binding.btnAuthoriseTMDB.setOnClickListener {
            if (prefsManager.hasTMDBApiKey()) {
                viewModel.fetchRequestToken()
            } else {
                Toast.makeText(this, "Please set API Key first", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateTMDBTokenDisplay() {
        val hasKey = prefsManager.hasTMDBApiKey()
        val sessionId = prefsManager.getTMDBSessionId()

        val status = StringBuilder()
        if (hasKey) status.append("API Key: Set | ") else status.append("API Key: Missing | ")
        if (!sessionId.isNullOrEmpty()) status.append("Session: Active") else status.append("Session: Inactive")

        binding.tvTMDBStatus.text = status.toString()

        if (hasKey && !sessionId.isNullOrEmpty()) {
            binding.tvTMDBStatus.setTextColor(getColor(android.R.color.holo_green_light))
        } else if (hasKey) {
            binding.tvTMDBStatus.setTextColor(getColor(android.R.color.holo_orange_light))
        } else {
            binding.tvTMDBStatus.setTextColor(getColor(android.R.color.holo_red_light))
        }
    }

    private fun showApiKeyDialog() {
        val input = TextInputEditText(this).apply {
            hint = "TMDB API Key"
            inputType = InputType.TYPE_CLASS_TEXT
            setText(prefsManager.getTMDBApiKey() ?: "")
            setPadding(48, 32, 48, 32)
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("Set TMDB API Key")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                val key = input.text.toString().trim()
                if (key.isNotEmpty()) {
                    prefsManager.saveTMDBApiKey(key)
                    updateTMDBTokenDisplay()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showAuthDialog(requestToken: String) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_tmdb_auth, null)
        val qrImage = dialogView.findViewById<ImageView>(R.id.qrCodeImage)
        val urlText = dialogView.findViewById<TextView>(R.id.authUrlText)
        val logoImage = dialogView.findViewById<ImageView>(R.id.tmdbLogo)

        val authUrl = "https://www.themoviedb.org/authenticate/$requestToken"
        val qrApiUrl = "https://api.qrserver.com/v1/create-qr-code/?size=400x400&data=$authUrl"
        val logoUrl = "https://files.readme.io/29c6fee-blue_short.svg"

        urlText.text = authUrl

        Glide.with(this).load(qrApiUrl).into(qrImage)
        Glide.with(this).load(logoUrl).into(logoImage)

        MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .setCancelable(false)
            .setPositiveButton("Connect") { _, _ ->
                viewModel.createSession(requestToken)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun setupAIOStreamsSection() {
        updateAIOStreamsDisplay()

        binding.btnConfigureAIOStreamsUrl.setOnClickListener {
            showAIOStreamsUrlDialog()
        }

        binding.btnConfigureAIOStreams.setOnClickListener {
            showAIOStreamsDialog()
        }
    }

    private fun updateAIOStreamsDisplay() {
        val username = prefsManager.getAIOStreamsUsername()
        val password = prefsManager.getAIOStreamsPassword()
        val url = prefsManager.getAIOStreamsUrl()

        // URL Status
        if (url.isNullOrEmpty()) {
            binding.tvAIOStreamsUrlStatus.text = "AIOStreams URL: Not set"
            binding.tvAIOStreamsUrlStatus.setTextColor(getColor(android.R.color.holo_red_light))
        } else {
            binding.tvAIOStreamsUrlStatus.text = "AIOStreams URL: ${url.take(30)}..."
            binding.tvAIOStreamsUrlStatus.setTextColor(getColor(android.R.color.holo_green_light))
        }

        // Credentials Status
        if (username.isNullOrEmpty() || password.isNullOrEmpty()) {
            binding.tvAIOStreamsStatus.text = "AIOStreams: Not configured"
            binding.tvAIOStreamsStatus.setTextColor(getColor(android.R.color.holo_red_light))
        } else {
            binding.tvAIOStreamsStatus.text = "AIOStreams: Configured (${username.take(8)}...)"
            binding.tvAIOStreamsStatus.setTextColor(getColor(android.R.color.holo_green_light))
        }
    }

    private fun showAIOStreamsUrlDialog() {
        val input = TextInputEditText(this).apply {
            hint = "AIOStreams Base URL"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI
            setText(prefsManager.getAIOStreamsUrl() ?: "https://aiostreams.shiggsy.co.uk")
            setPadding(48, 32, 48, 32)
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("Set AIOStreams URL")
            .setMessage("Enter the base URL for your AIOStreams instance")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                val url = input.text.toString().trim()
                if (url.isNotEmpty()) {
                    prefsManager.saveAIOStreamsUrl(url)
                    updateAIOStreamsDisplay()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showAIOStreamsDialog() {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 16, 48, 16)
        }

        val passwordInput = TextInputEditText(this).apply {
            id = 200
            hint = "Password"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            setText(prefsManager.getAIOStreamsPassword() ?: "")
            imeOptions = EditorInfo.IME_ACTION_DONE
            setSingleLine(true)
        }

        val usernameInput = TextInputEditText(this).apply {
            id = 100
            hint = "Username (UUID)"
            inputType = InputType.TYPE_CLASS_TEXT
            setText(prefsManager.getAIOStreamsUsername() ?: "")
            imeOptions = EditorInfo.IME_ACTION_NEXT
            setSingleLine(true)
            nextFocusDownId = 200

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
        viewModel.initUserLists()

        viewModel.allCatalogConfigs.observe(this) { list ->
            adapter.submitList(list)
        }
    }

    private fun setupUserSection() {
        val userId = prefsManager.getCurrentUserId()
        binding.tvCurrentUser.text = "Current User ID: $userId"
    }
}
