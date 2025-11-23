package com.example.stremiompvplayer

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.CheckBox
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
import com.example.stremiompvplayer.network.TraktDeviceCodeResponse
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

    // Define URLs
    private val VIREN_URL = "https://aiostreams.viren070.me"
    private val NHYIRA_URL = "https://aiostreamsfortheweak.nhyira.dev/"

    private var userInteracted = false
    // Track active Trakt dialog to dismiss it on success
    private var traktAuthDialog: androidx.appcompat.app.AlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefsManager = SharedPreferencesManager.getInstance(this)

        setupUserSection()
        setupTMDBSection()
        setupTraktSection() // [NEW]
        setupAIOStreamsSection()
        setupCatalogList()
        setupObservers()
    }

    private fun setupObservers() {
        // TMDB Observers
        viewModel.requestToken.observe(this) { token ->
            if (token != null) {
                showTMDBAuthDialog(token)
            }
        }

        viewModel.sessionId.observe(this) { sessionId ->
            if (sessionId != null) {
                Toast.makeText(this, "TMDB Authorization Successful!", Toast.LENGTH_SHORT).show()
                updateTMDBTokenDisplay()
            }
        }

        // Trakt Observers [NEW]
        viewModel.traktDeviceCode.observe(this) { codeData ->
            if (codeData != null) {
                showTraktAuthDialog(codeData)
            } else {
                // If null, it might mean success (dialog dismissed)
                traktAuthDialog?.dismiss()
            }
        }

        viewModel.isTraktEnabled.observe(this) { enabled ->
            updateTraktUI(enabled)
        }

        // General Error Observer
        viewModel.error.observe(this) { error ->
            if (error != null) {
                Toast.makeText(this, error, Toast.LENGTH_LONG).show()
            }
        }
    }

    // ==============================
    //        TRAKT SECTION
    // ==============================

    private fun setupTraktSection() {
        // Auth Button
        binding.btnTraktAuth.setOnClickListener {
            if (viewModel.isTraktEnabled.value == true) {
                // Logout
                MaterialAlertDialogBuilder(this)
                    .setTitle("Disconnect Trakt")
                    .setMessage("Are you sure you want to disconnect your Trakt account?")
                    .setPositiveButton("Disconnect") { _, _ ->
                        viewModel.logoutTrakt()
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            } else {
                // Login
                viewModel.startTraktAuth()
            }
        }

        // Sync Button
        binding.btnTraktSync.setOnClickListener {
            showTraktSyncDialog()
        }
    }

    private fun updateTraktUI(isEnabled: Boolean) {
        if (isEnabled) {
            binding.tvTraktStatus.text = "Trakt: Connected"
            binding.tvTraktStatus.setTextColor(getColor(android.R.color.holo_green_light))
            binding.btnTraktAuth.text = "Disconnect Trakt"
            binding.btnTraktSync.visibility = View.VISIBLE // Show Sync
        } else {
            binding.tvTraktStatus.text = "Trakt: Disconnected"
            binding.tvTraktStatus.setTextColor(getColor(android.R.color.holo_red_light))
            binding.btnTraktAuth.text = "Connect Trakt"
            binding.btnTraktSync.visibility = View.GONE // Hide Sync
        }
    }

    private fun showTraktAuthDialog(data: TraktDeviceCodeResponse) {
        val msg = "1. Visit: ${data.verification_url}\n2. Enter Code: ${data.user_code}"

        // Create a container for the dialog view
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 40, 50, 40)
        }

        val codeText = TextView(this).apply {
            text = data.user_code
            textSize = 32f
            textAlignment = View.TEXT_ALIGNMENT_CENTER
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(getColor(android.R.color.holo_blue_light))
            setPadding(0, 20, 0, 20)
            setTextIsSelectable(true)
        }

        val urlText = TextView(this).apply {
            text = "Visit: ${data.verification_url}"
            textSize = 16f
            textAlignment = View.TEXT_ALIGNMENT_CENTER
            setPadding(0, 0, 0, 20)
            setOnClickListener {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(data.verification_url))
                startActivity(intent)
            }
        }

        container.addView(urlText)
        container.addView(codeText)
        container.addView(TextView(this).apply {
            text = "Use a web browser to complete authentication. This dialog will close automatically when done."
            textAlignment = View.TEXT_ALIGNMENT_CENTER
            textSize = 12f
        })

        traktAuthDialog = MaterialAlertDialogBuilder(this)
            .setTitle("Connect Trakt")
            .setView(container)
            .setNegativeButton("Cancel") { _, _ ->
                // User cancelled manually
            }
            .show()
    }

    private fun showTraktSyncDialog() {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 40, 50, 40)
        }

        val cbHistory = CheckBox(this).apply {
            text = "Import Watched History\n(Adds to Library & marks watched)"
            isChecked = true
        }
        val cbNextUp = CheckBox(this).apply {
            text = "Add 'Next Up' List\n(Adds Trakt Next Up row to Home)"
            isChecked = true
        }
        val cbLists = CheckBox(this).apply {
            text = "Add Trakt Lists\n(Popular, Watchlist, Trending)"
            isChecked = true
        }

        container.addView(cbHistory)
        container.addView(cbNextUp)
        container.addView(cbLists)

        MaterialAlertDialogBuilder(this)
            .setTitle("Sync Options")
            .setView(container)
            .setPositiveButton("Sync") { _, _ ->
                viewModel.performTraktSync(
                    syncHistory = cbHistory.isChecked,
                    syncNextUp = cbNextUp.isChecked,
                    syncLists = cbLists.isChecked
                )
                Toast.makeText(this, "Sync started in background...", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // ==============================
    //        TMDB SECTION
    // ==============================

    private fun setupTMDBSection() {
        updateTMDBTokenDisplay()

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

    private fun showTMDBAuthDialog(requestToken: String) {
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

    // ==============================
    //      AIOSTREAMS SECTION
    // ==============================

    private fun setupAIOStreamsSection() {
        updateAIOStreamsDisplay()

        val options = listOf("Viren's Server", "Nhyira's Server", "Other Server")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, options)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerAioStreamsUrl.adapter = adapter

        val currentUrl = prefsManager.getAIOStreamsUrl()
        val selectionIndex = when {
            currentUrl?.contains("viren070.me") == true -> 0
            currentUrl?.contains("nhyira.dev") == true -> 1
            else -> 2
        }
        binding.spinnerAioStreamsUrl.setSelection(selectionIndex)

        binding.spinnerAioStreamsUrl.setOnTouchListener { _, _ ->
            userInteracted = true
            false
        }

        binding.spinnerAioStreamsUrl.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                if (!userInteracted) return

                when (position) {
                    0 -> {
                        prefsManager.saveAIOStreamsUrl(VIREN_URL)
                        updateAIOStreamsDisplay()
                    }
                    1 -> {
                        prefsManager.saveAIOStreamsUrl(NHYIRA_URL)
                        updateAIOStreamsDisplay()
                    }
                    2 -> {
                        showAIOStreamsUrlDialog()
                    }
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

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

    private fun showAIOStreamsUrlDialog() {
        val input = TextInputEditText(this).apply {
            hint = "AIOStreams Base URL"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI
            val current = prefsManager.getAIOStreamsUrl()
            if (current != VIREN_URL && current != NHYIRA_URL) {
                setText(current)
            }
            setPadding(48, 32, 48, 32)
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("Set Custom Server URL")
            .setMessage("Enter the base URL for your AIOStreams instance")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                val url = input.text.toString().trim()
                if (url.isNotEmpty()) {
                    prefsManager.saveAIOStreamsUrl(url)
                    updateAIOStreamsDisplay()
                }
            }
            .setNegativeButton("Cancel") { _, _ ->
                val currentUrl = prefsManager.getAIOStreamsUrl()
                val selectionIndex = when {
                    currentUrl?.contains("viren070.me") == true -> 0
                    currentUrl?.contains("nhyira.dev") == true -> 1
                    else -> 2
                }
                binding.spinnerAioStreamsUrl.setSelection(selectionIndex)
            }
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

    // ==============================
    //      CATALOG SECTION
    // ==============================

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