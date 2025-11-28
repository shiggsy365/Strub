package com.example.stremiompvplayer

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
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
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.stremiompvplayer.adapters.CatalogConfigAdapter
import com.example.stremiompvplayer.data.AppDatabase
import com.example.stremiompvplayer.data.ServiceLocator
import com.example.stremiompvplayer.databinding.ActivitySettingsBinding
import com.example.stremiompvplayer.models.UserCatalog
import com.example.stremiompvplayer.network.TraktDeviceCodeResponse
import com.example.stremiompvplayer.utils.SharedPreferencesManager
import com.example.stremiompvplayer.viewmodels.MainViewModel
import com.example.stremiompvplayer.viewmodels.MainViewModelFactory
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private val viewModel: MainViewModel by viewModels {
        MainViewModelFactory(
            ServiceLocator.getInstance(applicationContext),
            SharedPreferencesManager.getInstance(this)
        )
    }
    private lateinit var movieAdapter: CatalogConfigAdapter
    private lateinit var seriesAdapter: CatalogConfigAdapter
    private lateinit var prefsManager: SharedPreferencesManager

    private var userInteracted = false
    // Track active Trakt dialog to dismiss it on success
    private var traktAuthDialog: androidx.appcompat.app.AlertDialog? = null

    // Activity result launchers for import/export
    private val exportLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let { exportToFile(it) }
    }

    private val importLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { importFromFile(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefsManager = SharedPreferencesManager.getInstance(this)

        setupUserSection()
        setupImportExportButtons()
        setupTMDBSection()
        setupTraktSection()
        setupAIOStreamsSection()
        setupLiveTVSection()
        setupSubtitleSettings()
        setupCatalogList()
        setupObservers()

        // NEW: Setup Trakt progress observers
        setupTraktProgressObservers()
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

        // Trakt Observers
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

        // EPG Parsing Status Observer
        viewModel.epgParsingStatus.observe(this) { status ->
            if (status.isNullOrEmpty()) {
                binding.tvEPGParsingStatus.visibility = View.GONE
            } else {
                binding.tvEPGParsingStatus.visibility = View.VISIBLE
                binding.tvEPGParsingStatus.text = status

                // Color based on status
                when {
                    status.startsWith("Error") -> {
                        binding.tvEPGParsingStatus.setTextColor(getColor(android.R.color.holo_red_light))
                    }
                    status.startsWith("Success") || status.startsWith("Completed") -> {
                        binding.tvEPGParsingStatus.setTextColor(getColor(android.R.color.holo_green_light))
                        // Auto-hide success message after 5 seconds
                        binding.tvEPGParsingStatus.postDelayed({
                            binding.tvEPGParsingStatus.visibility = View.GONE
                        }, 5000)
                    }
                    else -> {
                        binding.tvEPGParsingStatus.setTextColor(getColor(R.color.text_secondary))
                    }
                }
            }
        }
    }

    // ==============================
    //   TRAKT PROGRESS OBSERVERS (NEW!)
    // ==============================

    private fun setupTraktProgressObservers() {
        // Progress counter (e.g., "Movies: 45/100")
        viewModel.traktSyncProgress.observe(this) { progress ->
            if (progress.isNullOrEmpty() || progress == "Sync complete!") {
                binding.tvTraktSyncProgress.visibility = View.GONE
            } else {
                binding.tvTraktSyncProgress.visibility = View.VISIBLE
                binding.tvTraktSyncProgress.text = progress
            }
        }

        // Status messages and UI state management
        viewModel.traktSyncStatus.observe(this) { status ->
            when (status) {
                is MainViewModel.TraktSyncStatus.Idle -> {
                    binding.tvTraktSyncStatus.visibility = View.GONE
                    binding.traktSyncProgressBar.visibility = View.GONE
                    binding.btnTraktSync.isEnabled = true
                    binding.btnTraktSync.text = "Sync Trakt"
                }
                is MainViewModel.TraktSyncStatus.Syncing -> {
                    binding.tvTraktSyncStatus.visibility = View.VISIBLE
                    binding.tvTraktSyncStatus.text = status.progress
                    binding.tvTraktSyncStatus.setTextColor(getColor(R.color.text_secondary))
                    binding.traktSyncProgressBar.visibility = View.VISIBLE
                    binding.btnTraktSync.isEnabled = false
                    binding.btnTraktSync.text = "Syncing..."
                }
                is MainViewModel.TraktSyncStatus.Success -> {
                    binding.tvTraktSyncStatus.visibility = View.VISIBLE
                    binding.tvTraktSyncStatus.text = status.message
                    binding.tvTraktSyncStatus.setTextColor(getColor(android.R.color.holo_green_light))
                    binding.traktSyncProgressBar.visibility = View.GONE
                    binding.btnTraktSync.isEnabled = true
                    binding.btnTraktSync.text = "Sync Trakt"

                    // Auto-hide success message after 3 seconds
                    binding.tvTraktSyncStatus.postDelayed({
                        binding.tvTraktSyncStatus.visibility = View.GONE
                    }, 3000)
                }
                is MainViewModel.TraktSyncStatus.Error -> {
                    binding.tvTraktSyncStatus.visibility = View.VISIBLE
                    binding.tvTraktSyncStatus.text = status.message
                    binding.tvTraktSyncStatus.setTextColor(getColor(android.R.color.holo_red_light))
                    binding.traktSyncProgressBar.visibility = View.GONE
                    binding.btnTraktSync.isEnabled = true
                    binding.btnTraktSync.text = "Retry Sync"
                }
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

        // Trakt Settings Button
        binding.btnTraktSettings.setOnClickListener {
            showTraktSettingsDialog()
        }

        // Library Health Check Button
        binding.btnLibraryHealth.setOnClickListener {
            showLibraryHealthDialog()
        }

        // Sync Analytics Button
        binding.btnSyncAnalytics.setOnClickListener {
            showSyncAnalyticsDialog()
        }
    }

    private fun updateTraktUI(isEnabled: Boolean) {
        if (isEnabled) {
            binding.tvTraktStatus.text = "Trakt: Connected"
            binding.tvTraktStatus.setTextColor(getColor(android.R.color.holo_green_light))
            binding.btnTraktAuth.text = "Disconnect Trakt"
            binding.btnTraktSync.visibility = View.VISIBLE
            binding.btnTraktSettings.visibility = View.VISIBLE
            binding.btnLibraryHealth.visibility = View.VISIBLE
            binding.btnSyncAnalytics.visibility = View.VISIBLE
        } else {
            binding.tvTraktStatus.text = "Trakt: Disconnected"
            binding.tvTraktStatus.setTextColor(getColor(android.R.color.holo_red_light))
            binding.btnTraktAuth.text = "Connect Trakt"
            binding.btnTraktSync.visibility = View.GONE
            binding.btnTraktSettings.visibility = View.GONE
            binding.btnLibraryHealth.visibility = View.GONE
            binding.btnSyncAnalytics.visibility = View.GONE
        }
    }

    private fun showTraktAuthDialog(data: TraktDeviceCodeResponse) {
        val msg = "1. Visit: ${data.verification_url}\n2. Enter Code: ${data.user_code}"

        // Create a container for the dialog view
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 40, 50, 40)
        }

        // Add Trakt logo at the top left
        val logoImage = ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                dpToPx(120),
                dpToPx(30)
            ).apply {
                gravity = android.view.Gravity.START
                bottomMargin = dpToPx(16)
            }
            scaleType = ImageView.ScaleType.FIT_START
            contentDescription = "Trakt Logo"
        }
        val traktLogoUrl = "https://walter.trakt.tv/public/production/images/logo-red.svg"
        Glide.with(this).load(traktLogoUrl).into(logoImage)
        container.addView(logoImage)

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

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
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
                    syncLists = cbLists.isChecked,
                    fetchMetadata = true // Full sync with posters
                )
                Toast.makeText(this, "Sync started...", Toast.LENGTH_SHORT).show()
            }
            .setNeutralButton("Fast Sync") { _, _ ->
                // NEW: Fast sync option without metadata
                viewModel.performTraktSync(
                    syncHistory = cbHistory.isChecked,
                    syncNextUp = cbNextUp.isChecked,
                    syncLists = cbLists.isChecked,
                    fetchMetadata = false // Skip posters for faster sync
                )
                Toast.makeText(this, "Fast sync started (no posters)...", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showTraktSettingsDialog() {
        val dialogView = layoutInflater.inflate(android.R.layout.select_dialog_item, null)
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 40, 50, 40)
        }

        // Auto-sync on startup
        val cbAutoSync = CheckBox(this).apply {
            text = "Auto-sync on app startup"
            isChecked = prefsManager.isAutoSyncOnStartup()
            setOnCheckedChangeListener { _, isChecked ->
                prefsManager.setAutoSyncOnStartup(isChecked)
            }
        }

        // Background sync enabled
        val cbBackgroundSync = CheckBox(this).apply {
            text = "Enable background sync"
            isChecked = prefsManager.isBackgroundSyncEnabled()
            setOnCheckedChangeListener { _, isChecked ->
                prefsManager.setBackgroundSyncEnabled(isChecked)
                if (isChecked) {
                    com.example.stremiompvplayer.utils.TraktSyncScheduler.schedulePeriodicSync(this@SettingsActivity)
                } else {
                    com.example.stremiompvplayer.utils.TraktSyncScheduler.cancelPeriodicSync(this@SettingsActivity)
                }
            }
        }

        // Wi-Fi only
        val cbWifiOnly = CheckBox(this).apply {
            text = "Sync on Wi-Fi only"
            isChecked = prefsManager.isSyncOnWifiOnly()
            setOnCheckedChangeListener { _, isChecked ->
                prefsManager.setSyncOnWifiOnly(isChecked)
            }
        }

        // Sync interval section
        val intervalLabel = TextView(this).apply {
            text = "Background sync interval:"
            textSize = 16f
            setPadding(0, 40, 0, 10)
        }

        val intervalValue = TextView(this).apply {
            val hours = prefsManager.getBackgroundSyncInterval() / (60 * 60 * 1000)
            text = "$hours hours"
            textSize = 14f
            setTextColor(getColor(R.color.text_secondary))
            setPadding(0, 0, 0, 20)
        }

        val interval6h = com.google.android.material.button.MaterialButton(this).apply {
            text = "6 hours"
            setOnClickListener {
                prefsManager.setBackgroundSyncInterval(6 * 60 * 60 * 1000L)
                intervalValue.text = "6 hours"
                com.example.stremiompvplayer.utils.TraktSyncScheduler.schedulePeriodicSync(this@SettingsActivity)
            }
        }

        val interval12h = com.google.android.material.button.MaterialButton(this).apply {
            text = "12 hours"
            setOnClickListener {
                prefsManager.setBackgroundSyncInterval(12 * 60 * 60 * 1000L)
                intervalValue.text = "12 hours"
                com.example.stremiompvplayer.utils.TraktSyncScheduler.schedulePeriodicSync(this@SettingsActivity)
            }
        }

        val interval24h = com.google.android.material.button.MaterialButton(this).apply {
            text = "24 hours"
            setOnClickListener {
                prefsManager.setBackgroundSyncInterval(24 * 60 * 60 * 1000L)
                intervalValue.text = "24 hours"
                com.example.stremiompvplayer.utils.TraktSyncScheduler.schedulePeriodicSync(this@SettingsActivity)
            }
        }

        val intervalButtons = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            addView(interval6h, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            addView(interval12h, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            addView(interval24h, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        }

        // Last sync time
        val lastSyncLabel = TextView(this).apply {
            val lastSync = prefsManager.getLastTraktSyncTime()
            val text = if (lastSync > 0) {
                val format = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
                "Last sync: ${format.format(Date(lastSync))}"
            } else {
                "Last sync: Never"
            }
            this.text = text
            textSize = 12f
            setTextColor(getColor(R.color.text_secondary))
            setPadding(0, 20, 0, 0)
        }

        container.addView(cbAutoSync)
        container.addView(cbBackgroundSync)
        container.addView(cbWifiOnly)
        container.addView(intervalLabel)
        container.addView(intervalValue)
        container.addView(intervalButtons)
        container.addView(lastSyncLabel)

        MaterialAlertDialogBuilder(this)
            .setTitle("Trakt Sync Settings")
            .setView(container)
            .setPositiveButton("Close", null)
            .show()
    }

    private fun showLibraryHealthDialog() {
        val progressDialog = MaterialAlertDialogBuilder(this)
            .setTitle("Library Health Check")
            .setMessage("Analyzing library...")
            .setCancelable(false)
            .create()

        progressDialog.show()

        // Perform health check
        viewModel.performLibraryHealthCheck()

        // Observer for health report
        viewModel.libraryHealthReport.observe(this) { report ->
            if (report != null) {
                progressDialog.dismiss()
                showHealthReportDialog(report)
                // Remove observer after showing dialog
                viewModel.libraryHealthReport.removeObservers(this)
            }
        }
    }

    private fun showHealthReportDialog(report: MainViewModel.LibraryHealthReport) {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 40, 50, 40)
        }

        // Summary
        val totalIssues = report.traktOnlyMovies.size + report.traktOnlySeries.size +
                         report.localOnlyMovies.size + report.localOnlySeries.size

        val summaryText = TextView(this).apply {
            text = if (totalIssues == 0) {
                "✓ Library is fully synced with Trakt!"
            } else {
                "Found $totalIssues sync discrepancies:"
            }
            textSize = 16f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(0, 0, 0, 20)
            setTextColor(if (totalIssues == 0) getColor(android.R.color.holo_green_light) else getColor(android.R.color.holo_orange_light))
        }

        // Statistics
        val statsText = TextView(this).apply {
            text = """
                Movies:
                  • Trakt: ${report.totalTraktMovies}
                  • Local: ${report.totalLocalMovies}
                  • In Trakt only: ${report.traktOnlyMovies.size}
                  • In Local only: ${report.localOnlyMovies.size}

                Series:
                  • Trakt: ${report.totalTraktSeries}
                  • Local: ${report.totalLocalSeries}
                  • In Trakt only: ${report.traktOnlySeries.size}
                  • In Local only: ${report.localOnlySeries.size}
            """.trimIndent()
            textSize = 14f
            setTextColor(getColor(R.color.text_secondary))
        }

        container.addView(summaryText)
        container.addView(statsText)

        val builder = MaterialAlertDialogBuilder(this)
            .setTitle("Library Health Report")
            .setView(container)
            .setNegativeButton("Close", null)

        // Add action buttons if there are issues
        if (totalIssues > 0) {
            if (report.traktOnlyMovies.isNotEmpty() || report.traktOnlySeries.isNotEmpty()) {
                builder.setNeutralButton("Import from Trakt") { _, _ ->
                    viewModel.importMissingFromTrakt()
                    Toast.makeText(this, "Importing ${report.traktOnlyMovies.size + report.traktOnlySeries.size} items from Trakt...", Toast.LENGTH_SHORT).show()
                }
            }
            if (report.localOnlyMovies.isNotEmpty() || report.localOnlySeries.isNotEmpty()) {
                builder.setPositiveButton("Export to Trakt") { _, _ ->
                    viewModel.exportMissingToTrakt()
                    Toast.makeText(this, "Exporting ${report.localOnlyMovies.size + report.localOnlySeries.size} items to Trakt...", Toast.LENGTH_SHORT).show()
                }
            }
        }

        builder.show()
    }

    private fun showSyncAnalyticsDialog() {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 40, 50, 40)
        }

        // Sync Statistics
        val lastSync = prefsManager.getLastTraktSyncTime()
        val syncInterval = prefsManager.getBackgroundSyncInterval() / (60 * 60 * 1000)
        val backgroundSyncEnabled = prefsManager.isBackgroundSyncEnabled()
        val autoSyncEnabled = prefsManager.isAutoSyncOnStartup()
        val wifiOnly = prefsManager.isSyncOnWifiOnly()

        val statsText = TextView(this).apply {
            val lastSyncText = if (lastSync > 0) {
                val format = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
                format.format(Date(lastSync))
            } else {
                "Never"
            }

            val timeSinceSync = if (lastSync > 0) {
                val hours = (System.currentTimeMillis() - lastSync) / (60 * 60 * 1000)
                "$hours hours ago"
            } else {
                "N/A"
            }

            text = """
                📊 Sync Statistics

                Last Sync: $lastSyncText
                Time Since Last Sync: $timeSinceSync

                ⚙️ Settings
                Auto-sync on Startup: ${if (autoSyncEnabled) "✓ Enabled" else "✗ Disabled"}
                Background Sync: ${if (backgroundSyncEnabled) "✓ Enabled" else "✗ Disabled"}
                Sync Interval: $syncInterval hours
                Wi-Fi Only: ${if (wifiOnly) "✓ Yes" else "✗ No"}

                📦 Database
                Database Version: 7
                Pending Sync Actions: Queued for offline

                🔄 Sync Coverage
                • Collection sync: ✓ Bidirectional
                • Watchlist sync: ✓ Bidirectional
                • Watched history: ✓ Bidirectional
                • Continue watching: ✓ Import
                • Ratings: ✓ Export
                • Custom lists: ✓ Management
            """.trimIndent()
            textSize = 14f
            setTextColor(getColor(R.color.text_secondary))
        }

        // Next Sync Time
        val nextSyncText = TextView(this).apply {
            if (backgroundSyncEnabled && lastSync > 0) {
                val nextSync = lastSync + prefsManager.getBackgroundSyncInterval()
                val format = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
                text = "\n⏰ Next Background Sync: ${format.format(Date(nextSync))}"
                textSize = 12f
                setTextColor(getColor(android.R.color.holo_blue_light))
                setTypeface(null, android.graphics.Typeface.BOLD)
            } else if (backgroundSyncEnabled) {
                text = "\n⏰ Next Background Sync: On next app restart"
                textSize = 12f
                setTextColor(getColor(android.R.color.holo_blue_light))
            } else {
                text = "\n⏰ Background Sync: Disabled"
                textSize = 12f
                setTextColor(getColor(android.R.color.holo_orange_light))
            }
        }

        container.addView(statsText)
        container.addView(nextSyncText)

        MaterialAlertDialogBuilder(this)
            .setTitle("Sync Analytics")
            .setView(container)
            .setPositiveButton("Close", null)
            .setNeutralButton("Sync Now") { _, _ ->
                viewModel.syncTraktLibrary()
                Toast.makeText(this, "Manual sync started...", Toast.LENGTH_SHORT).show()
            }
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

        binding.btnConfigureAIOStreams.setOnClickListener {
            showAIOStreamsDialog()
        }
    }

    private fun updateAIOStreamsDisplay() {
        val manifestUrl = prefsManager.getAIOStreamsManifestUrl()

        if (manifestUrl.isNullOrEmpty()) {
            binding.tvAIOStreamsUrl.text = "Not configured"
            binding.tvAIOStreamsStatus.text = "AIOStreams: Not configured"
            binding.tvAIOStreamsStatus.setTextColor(getColor(android.R.color.holo_red_light))
        } else {
            // Show truncated URL for display
            val displayUrl = if (manifestUrl.length > 50) {
                manifestUrl.take(47) + "..."
            } else {
                manifestUrl
            }
            binding.tvAIOStreamsUrl.text = "Manifest: $displayUrl"
            binding.tvAIOStreamsStatus.text = "AIOStreams: Configured"
            binding.tvAIOStreamsStatus.setTextColor(getColor(android.R.color.holo_green_light))
        }
    }

    private fun showAIOStreamsDialog() {
        val input = TextInputEditText(this).apply {
            hint = "Manifest URL"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI
            setText(prefsManager.getAIOStreamsManifestUrl() ?: "")
            setPadding(48, 32, 48, 32)
            imeOptions = EditorInfo.IME_ACTION_DONE
            setSingleLine(true)
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("Configure AIOStreams")
            .setMessage("Enter your AIOStreams manifest.json URL")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                val url = input.text.toString().trim()
                if (url.isNotEmpty()) {
                    prefsManager.saveAIOStreamsManifestUrl(url)
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
    //      LIVE TV SECTION
    // ==============================

    private fun setupLiveTVSection() {
        updateLiveTVDisplay()

        binding.btnConfigureLiveTV.setOnClickListener {
            showLiveTVDialog()
        }

        binding.btnRefreshTVGuide.setOnClickListener {
            viewModel.refreshTVGuide()
        }

        binding.btnTVSettings.setOnClickListener {
            startActivity(Intent(this, TVSettingsActivity::class.java))
        }
    }

    private fun updateLiveTVDisplay() {
        val m3uUrl = prefsManager.getLiveTVM3UUrl()
        val epgUrl = prefsManager.getLiveTVEPGUrl()

        if (m3uUrl.isNullOrEmpty() && epgUrl.isNullOrEmpty()) {
            binding.tvLiveTVUrls.text = "Not configured"
            binding.tvLiveTVStatus.text = "Live TV: Not configured"
            binding.tvLiveTVStatus.setTextColor(getColor(android.R.color.holo_red_light))
        } else {
            val displayText = buildString {
                if (!m3uUrl.isNullOrEmpty()) {
                    append("M3U: ${if (m3uUrl.length > 30) m3uUrl.take(27) + "..." else m3uUrl}")
                }
                if (!epgUrl.isNullOrEmpty()) {
                    if (isNotEmpty()) append("\n")
                    append("EPG: ${if (epgUrl.length > 30) epgUrl.take(27) + "..." else epgUrl}")
                }
            }
            binding.tvLiveTVUrls.text = displayText
            binding.tvLiveTVStatus.text = "Live TV: Configured"
            binding.tvLiveTVStatus.setTextColor(getColor(android.R.color.holo_green_light))
        }
    }

    private fun showLiveTVDialog() {
        // Store old URLs to detect changes
        val oldM3u = prefsManager.getLiveTVM3UUrl() ?: ""
        val oldEpg = prefsManager.getLiveTVEPGUrl() ?: ""

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 16, 48, 16)
        }

        val m3uInput = TextInputEditText(this).apply {
            hint = "M3U Playlist URL"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI
            setText(oldM3u)
            setSingleLine(true)
            isFocusable = true
            isFocusableInTouchMode = true
            imeOptions = EditorInfo.IME_ACTION_NEXT
        }

        val epgInput = TextInputEditText(this).apply {
            hint = "EPG XML URL"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI
            setText(oldEpg)
            setSingleLine(true)
            isFocusable = true
            isFocusableInTouchMode = true
            imeOptions = EditorInfo.IME_ACTION_DONE
        }

        container.addView(m3uInput)
        container.addView(epgInput)

        MaterialAlertDialogBuilder(this)
            .setTitle("Configure Live TV")
            .setMessage("Enter URLs for M3U playlist and EPG (XMLTV) guide data")
            .setView(container)
            .setPositiveButton("Save") { _, _ ->
                val m3u = m3uInput.text.toString().trim()
                val epg = epgInput.text.toString().trim()

                // Check if URLs changed
                val urlsChanged = (m3u != oldM3u) || (epg != oldEpg)

                if (m3u.isNotEmpty()) {
                    prefsManager.saveLiveTVM3UUrl(m3u)
                }
                if (epg.isNotEmpty()) {
                    prefsManager.saveLiveTVEPGUrl(epg)
                }

                if (m3u.isNotEmpty() || epg.isNotEmpty()) {
                    updateLiveTVDisplay()

                    // Clear database mappings if TV guide source changed
                    if (urlsChanged) {
                        clearTVSettingsMappings()
                    }

                    Toast.makeText(this, "Live TV configured", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .setNeutralButton("Clear") { _, _ ->
                prefsManager.clearLiveTVCredentials()
                updateLiveTVDisplay()
                // Clear all TV settings when clearing URLs
                clearTVSettingsMappings()
            }
            .show()
    }

    private fun clearTVSettingsMappings() {
        lifecycleScope.launch {
            try {
                val userId = prefsManager.getCurrentUserId() ?: ""
                withContext(Dispatchers.IO) {
                    val database = AppDatabase.getInstance(this@SettingsActivity)
                    database.channelGroupDao().deleteByUser(userId)
                    database.channelMappingDao().deleteByUser(userId)
                }
                android.util.Log.d("Settings", "Cleared TV settings mappings due to source change")
            } catch (e: Exception) {
                android.util.Log.e("Settings", "Error clearing TV settings mappings", e)
            }
        }
    }

    // ==============================
    //      SUBTITLE SETTINGS
    // ==============================

    private fun setupSubtitleSettings() {
        binding.btnSubtitleSettings.setOnClickListener {
            showSubtitleCustomizationDialog()
        }
    }

    private fun showSubtitleCustomizationDialog() {
        val scrollView = android.widget.ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 32, 48, 32)
        }

        scrollView.addView(container)

        // Current values (mutable for tracking selections)
        var currentTextSize = prefsManager.getSubtitleTextSize()
        var currentTextColor = prefsManager.getSubtitleTextColor()
        var currentBackgroundColor = prefsManager.getSubtitleBackgroundColor()
        var currentWindowColor = prefsManager.getSubtitleWindowColor()
        var currentEdgeType = prefsManager.getSubtitleEdgeType()
        var currentEdgeColor = prefsManager.getSubtitleEdgeColor()

        // Text Size Section
        val textSizeLabel = TextView(this).apply {
            text = "Text Size"
            textSize = 16f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(0, 0, 0, 8)
        }

        val textSizeValue = TextView(this).apply {
            val sizeText = when {
                currentTextSize < 0.75f -> "Very Small"
                currentTextSize < 1.0f -> "Small"
                currentTextSize == 1.0f -> "Medium"
                currentTextSize < 1.5f -> "Large"
                else -> "Very Large"
            }
            text = sizeText
            textSize = 14f
            setTextColor(getColor(R.color.text_secondary))
            setPadding(0, 0, 0, 12)
        }

        // Helper function to update button selection states
        fun updateSizeButtonStates(buttons: List<com.google.android.material.button.MaterialButton>, selectedSize: Float) {
            buttons.forEach { btn ->
                val isSelected = when (btn.text.toString()) {
                    "Very Small" -> selectedSize < 0.75f
                    "Small" -> selectedSize >= 0.75f && selectedSize < 1.0f
                    "Medium" -> selectedSize == 1.0f
                    "Large" -> selectedSize > 1.0f && selectedSize < 1.5f
                    "Very Large" -> selectedSize >= 1.5f
                    else -> false
                }
                if (isSelected) {
                    btn.strokeWidth = dpToPx(3)
                    btn.strokeColor = android.content.res.ColorStateList.valueOf(getColor(R.color.md_theme_primary))
                } else {
                    btn.strokeWidth = dpToPx(1)
                    btn.strokeColor = android.content.res.ColorStateList.valueOf(getColor(R.color.md_theme_outline))
                }
            }
        }

        val sizeVerySmall = com.google.android.material.button.MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
            text = "Very Small"
        }

        val sizeSmall = com.google.android.material.button.MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
            text = "Small"
        }

        val sizeMedium = com.google.android.material.button.MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
            text = "Medium"
        }

        val sizeLarge = com.google.android.material.button.MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
            text = "Large"
        }

        val sizeVeryLarge = com.google.android.material.button.MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
            text = "Very Large"
        }

        val sizeButtons = listOf(sizeVerySmall, sizeSmall, sizeMedium, sizeLarge, sizeVeryLarge)

        // Set click listeners
        sizeVerySmall.setOnClickListener {
            currentTextSize = 0.5f
            prefsManager.setSubtitleTextSize(currentTextSize)
            textSizeValue.text = "Very Small"
            updateSizeButtonStates(sizeButtons, currentTextSize)
        }

        sizeSmall.setOnClickListener {
            currentTextSize = 0.75f
            prefsManager.setSubtitleTextSize(currentTextSize)
            textSizeValue.text = "Small"
            updateSizeButtonStates(sizeButtons, currentTextSize)
        }

        sizeMedium.setOnClickListener {
            currentTextSize = 1.0f
            prefsManager.setSubtitleTextSize(currentTextSize)
            textSizeValue.text = "Medium"
            updateSizeButtonStates(sizeButtons, currentTextSize)
        }

        sizeLarge.setOnClickListener {
            currentTextSize = 1.25f
            prefsManager.setSubtitleTextSize(currentTextSize)
            textSizeValue.text = "Large"
            updateSizeButtonStates(sizeButtons, currentTextSize)
        }

        sizeVeryLarge.setOnClickListener {
            currentTextSize = 1.5f
            prefsManager.setSubtitleTextSize(currentTextSize)
            textSizeValue.text = "Very Large"
            updateSizeButtonStates(sizeButtons, currentTextSize)
        }

        // Initialize selection states
        updateSizeButtonStates(sizeButtons, currentTextSize)

        val sizeButtonsRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            addView(sizeVerySmall, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                setMargins(0, 0, dpToPx(4), 0)
            })
            addView(sizeSmall, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                setMargins(0, 0, dpToPx(4), 0)
            })
            addView(sizeMedium, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                setMargins(0, 0, dpToPx(4), 0)
            })
            addView(sizeLarge, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                setMargins(0, 0, dpToPx(4), 0)
            })
            addView(sizeVeryLarge, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        }

        // Text Color Section
        val textColorLabel = TextView(this).apply {
            text = "Text Color"
            textSize = 16f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(0, 24, 0, 12)
        }

        fun updateColorButtonStates(buttons: List<com.google.android.material.button.MaterialButton>, selectedColor: Int) {
            buttons.forEach { btn ->
                val btnColor = when (btn.text.toString()) {
                    "White" -> android.graphics.Color.WHITE
                    "Yellow" -> android.graphics.Color.YELLOW
                    "Cyan" -> android.graphics.Color.CYAN
                    "Green" -> android.graphics.Color.GREEN
                    else -> android.graphics.Color.WHITE
                }
                if (btnColor == selectedColor) {
                    btn.strokeWidth = dpToPx(3)
                    btn.strokeColor = android.content.res.ColorStateList.valueOf(getColor(R.color.md_theme_primary))
                } else {
                    btn.strokeWidth = dpToPx(1)
                    btn.strokeColor = android.content.res.ColorStateList.valueOf(getColor(R.color.md_theme_outline))
                }
            }
        }

        val colorWhite = com.google.android.material.button.MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
            text = "White"
        }

        val colorYellow = com.google.android.material.button.MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
            text = "Yellow"
        }

        val colorCyan = com.google.android.material.button.MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
            text = "Cyan"
        }

        val colorGreen = com.google.android.material.button.MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
            text = "Green"
        }

        val colorButtons = listOf(colorWhite, colorYellow, colorCyan, colorGreen)

        colorWhite.setOnClickListener {
            currentTextColor = android.graphics.Color.WHITE
            prefsManager.setSubtitleTextColor(currentTextColor)
            updateColorButtonStates(colorButtons, currentTextColor)
        }

        colorYellow.setOnClickListener {
            currentTextColor = android.graphics.Color.YELLOW
            prefsManager.setSubtitleTextColor(currentTextColor)
            updateColorButtonStates(colorButtons, currentTextColor)
        }

        colorCyan.setOnClickListener {
            currentTextColor = android.graphics.Color.CYAN
            prefsManager.setSubtitleTextColor(currentTextColor)
            updateColorButtonStates(colorButtons, currentTextColor)
        }

        colorGreen.setOnClickListener {
            currentTextColor = android.graphics.Color.GREEN
            prefsManager.setSubtitleTextColor(currentTextColor)
            updateColorButtonStates(colorButtons, currentTextColor)
        }

        updateColorButtonStates(colorButtons, currentTextColor)

        val textColorButtons = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            addView(colorWhite, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                setMargins(0, 0, dpToPx(4), 0)
            })
            addView(colorYellow, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                setMargins(0, 0, dpToPx(4), 0)
            })
            addView(colorCyan, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                setMargins(0, 0, dpToPx(4), 0)
            })
            addView(colorGreen, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        }

        // Background Color Section
        val bgColorLabel = TextView(this).apply {
            text = "Background"
            textSize = 16f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(0, 24, 0, 12)
        }

        fun updateBgButtonStates(buttons: List<com.google.android.material.button.MaterialButton>, selectedBg: Int) {
            buttons.forEach { btn ->
                val btnBg = when (btn.text.toString()) {
                    "Transparent" -> android.graphics.Color.TRANSPARENT
                    "Semi-Black" -> android.graphics.Color.argb(128, 0, 0, 0)
                    "Black" -> android.graphics.Color.BLACK
                    else -> android.graphics.Color.TRANSPARENT
                }
                if (btnBg == selectedBg) {
                    btn.strokeWidth = dpToPx(3)
                    btn.strokeColor = android.content.res.ColorStateList.valueOf(getColor(R.color.md_theme_primary))
                } else {
                    btn.strokeWidth = dpToPx(1)
                    btn.strokeColor = android.content.res.ColorStateList.valueOf(getColor(R.color.md_theme_outline))
                }
            }
        }

        val bgTransparent = com.google.android.material.button.MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
            text = "Transparent"
        }

        val bgSemiBlack = com.google.android.material.button.MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
            text = "Semi-Black"
        }

        val bgBlack = com.google.android.material.button.MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
            text = "Black"
        }

        val bgButtons = listOf(bgTransparent, bgSemiBlack, bgBlack)

        bgTransparent.setOnClickListener {
            currentBackgroundColor = android.graphics.Color.TRANSPARENT
            prefsManager.setSubtitleBackgroundColor(currentBackgroundColor)
            updateBgButtonStates(bgButtons, currentBackgroundColor)
        }

        bgSemiBlack.setOnClickListener {
            currentBackgroundColor = android.graphics.Color.argb(128, 0, 0, 0)
            prefsManager.setSubtitleBackgroundColor(currentBackgroundColor)
            updateBgButtonStates(bgButtons, currentBackgroundColor)
        }

        bgBlack.setOnClickListener {
            currentBackgroundColor = android.graphics.Color.BLACK
            prefsManager.setSubtitleBackgroundColor(currentBackgroundColor)
            updateBgButtonStates(bgButtons, currentBackgroundColor)
        }

        updateBgButtonStates(bgButtons, currentBackgroundColor)

        val bgColorButtons = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            addView(bgTransparent, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                setMargins(0, 0, dpToPx(4), 0)
            })
            addView(bgSemiBlack, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                setMargins(0, 0, dpToPx(4), 0)
            })
            addView(bgBlack, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        }

        // Edge Type Section
        val edgeTypeLabel = TextView(this).apply {
            text = "Edge Style"
            textSize = 16f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(0, 24, 0, 12)
        }

        fun updateEdgeButtonStates(buttons: List<com.google.android.material.button.MaterialButton>, selectedEdge: Int) {
            buttons.forEach { btn ->
                val btnEdge = when (btn.text.toString()) {
                    "None" -> 0
                    "Outline" -> 1
                    "Shadow" -> 2
                    "Raised" -> 3
                    else -> 1
                }
                if (btnEdge == selectedEdge) {
                    btn.strokeWidth = dpToPx(3)
                    btn.strokeColor = android.content.res.ColorStateList.valueOf(getColor(R.color.md_theme_primary))
                } else {
                    btn.strokeWidth = dpToPx(1)
                    btn.strokeColor = android.content.res.ColorStateList.valueOf(getColor(R.color.md_theme_outline))
                }
            }
        }

        val edgeNone = com.google.android.material.button.MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
            text = "None"
        }

        val edgeOutline = com.google.android.material.button.MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
            text = "Outline"
        }

        val edgeDropShadow = com.google.android.material.button.MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
            text = "Shadow"
        }

        val edgeRaised = com.google.android.material.button.MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
            text = "Raised"
        }

        val edgeButtons = listOf(edgeNone, edgeOutline, edgeDropShadow, edgeRaised)

        edgeNone.setOnClickListener {
            currentEdgeType = 0
            prefsManager.setSubtitleEdgeType(currentEdgeType)
            updateEdgeButtonStates(edgeButtons, currentEdgeType)
        }

        edgeOutline.setOnClickListener {
            currentEdgeType = 1
            prefsManager.setSubtitleEdgeType(currentEdgeType)
            updateEdgeButtonStates(edgeButtons, currentEdgeType)
        }

        edgeDropShadow.setOnClickListener {
            currentEdgeType = 2
            prefsManager.setSubtitleEdgeType(currentEdgeType)
            updateEdgeButtonStates(edgeButtons, currentEdgeType)
        }

        edgeRaised.setOnClickListener {
            currentEdgeType = 3
            prefsManager.setSubtitleEdgeType(currentEdgeType)
            updateEdgeButtonStates(edgeButtons, currentEdgeType)
        }

        updateEdgeButtonStates(edgeButtons, currentEdgeType)

        val edgeTypeButtons = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            addView(edgeNone, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                setMargins(0, 0, dpToPx(4), 0)
            })
            addView(edgeOutline, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                setMargins(0, 0, dpToPx(4), 0)
            })
            addView(edgeDropShadow, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                setMargins(0, 0, dpToPx(4), 0)
            })
            addView(edgeRaised, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        }

        // Reset to Defaults Button
        val resetButton = com.google.android.material.button.MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
            text = "Reset to Defaults"
            setOnClickListener {
                currentTextSize = 1.0f
                currentTextColor = android.graphics.Color.WHITE
                currentBackgroundColor = android.graphics.Color.TRANSPARENT
                currentWindowColor = android.graphics.Color.TRANSPARENT
                currentEdgeType = 1
                currentEdgeColor = android.graphics.Color.BLACK

                prefsManager.setSubtitleTextSize(currentTextSize)
                prefsManager.setSubtitleTextColor(currentTextColor)
                prefsManager.setSubtitleBackgroundColor(currentBackgroundColor)
                prefsManager.setSubtitleWindowColor(currentWindowColor)
                prefsManager.setSubtitleEdgeType(currentEdgeType)
                prefsManager.setSubtitleEdgeColor(currentEdgeColor)

                textSizeValue.text = "Medium"
                updateSizeButtonStates(sizeButtons, currentTextSize)
                updateColorButtonStates(colorButtons, currentTextColor)
                updateBgButtonStates(bgButtons, currentBackgroundColor)
                updateEdgeButtonStates(edgeButtons, currentEdgeType)

                Toast.makeText(this@SettingsActivity, "Subtitle settings reset to defaults", Toast.LENGTH_SHORT).show()
            }
        }

        resetButton.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            topMargin = dpToPx(24)
        }

        // Add all views to container
        container.addView(textSizeLabel)
        container.addView(textSizeValue)
        container.addView(sizeButtonsRow)
        container.addView(textColorLabel)
        container.addView(textColorButtons)
        container.addView(bgColorLabel)
        container.addView(bgColorButtons)
        container.addView(edgeTypeLabel)
        container.addView(edgeTypeButtons)
        container.addView(resetButton)

        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle("Customize Subtitles")
            .setView(scrollView)
            .setPositiveButton("Done") { _, _ ->
                Toast.makeText(this, "Subtitle settings saved", Toast.LENGTH_SHORT).show()
            }
            .create()

        dialog.show()

        // Make dialog wider (90% of screen width) and set max height
        val maxHeight = (resources.displayMetrics.heightPixels * 0.8).toInt()
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.9).toInt(),
            maxHeight
        )
    }

    // ==============================
    //      CATALOG SECTION
    // ==============================

    private fun setupCatalogList() {
        // Setup Movie Adapter
        movieAdapter = CatalogConfigAdapter(
            onUpdate = { catalog -> viewModel.updateCatalogConfig(catalog) },
            onMoveUp = { catalog, pos ->
                val list = movieAdapter.currentList
                if (pos > 0) {
                    val prev = list[pos - 1]
                    viewModel.swapCatalogOrder(catalog, prev)
                }
            },
            onMoveDown = { catalog, pos ->
                val list = movieAdapter.currentList
                if (pos < list.size - 1) {
                    val next = list[pos + 1]
                    viewModel.swapCatalogOrder(catalog, next)
                }
            },
            onDelete = { catalog -> deleteCatalog(catalog) }
        )

        binding.rvCatalogConfigs.layoutManager = LinearLayoutManager(this)
        binding.rvCatalogConfigs.adapter = movieAdapter

        // Setup Series Adapter
        seriesAdapter = CatalogConfigAdapter(
            onUpdate = { catalog -> viewModel.updateCatalogConfig(catalog) },
            onMoveUp = { catalog, pos ->
                val list = seriesAdapter.currentList
                if (pos > 0) {
                    val prev = list[pos - 1]
                    viewModel.swapCatalogOrder(catalog, prev)
                }
            },
            onMoveDown = { catalog, pos ->
                val list = seriesAdapter.currentList
                if (pos < list.size - 1) {
                    val next = list[pos + 1]
                    viewModel.swapCatalogOrder(catalog, next)
                }
            },
            onDelete = { catalog -> deleteCatalog(catalog) }
        )

        binding.rvSeriesCatalogConfigs.layoutManager = LinearLayoutManager(this)
        binding.rvSeriesCatalogConfigs.adapter = seriesAdapter

        viewModel.initDefaultCatalogs()
        viewModel.initUserLists()

        // Observe movie catalogs
        viewModel.movieCatalogs.observe(this) { list ->
            movieAdapter.submitList(list)
        }

        // Observe series catalogs
        viewModel.seriesCatalogs.observe(this) { list ->
            seriesAdapter.submitList(list)
        }

        // Add Custom List Buttons
        binding.btnAddMovieList.setOnClickListener {
            showAddCustomListDialog("movies")
        }

        binding.btnAddSeriesList.setOnClickListener {
            showAddCustomListDialog("series")
        }
    }

    private fun deleteCatalog(catalog: UserCatalog) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Delete List")
            .setMessage("Are you sure you want to delete '${catalog.displayName}'?")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deleteCatalog(catalog)
                Toast.makeText(this, "List deleted", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showAddCustomListDialog(pageType: String) {
        val listTypeItems = arrayOf("mdblist", "IMDB List", "TMDB List", "Trakt List")

        MaterialAlertDialogBuilder(this)
            .setTitle("Add Custom List - ${pageType.capitalize()}")
            .setItems(listTypeItems) { _, which ->
                when (which) {
                    0 -> showListUrlDialog("mdblist", pageType)
                    1 -> showListUrlDialog("imdb", pageType)
                    2 -> showListUrlDialog("tmdb", pageType)
                    3 -> showTraktListsDialog(pageType)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showListUrlDialog(listType: String, pageType: String) {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 16, 48, 16)
        }

        val urlInput = TextInputEditText(this).apply {
            hint = when (listType) {
                "mdblist" -> "mdblist.com URL or ID"
                "imdb" -> "IMDB List ID (e.g., ls123456789)"
                "tmdb" -> "TMDB List ID"
                "trakt" -> "Trakt List URL or username/listname"
                else -> "List URL or ID"
            }
            inputType = InputType.TYPE_CLASS_TEXT
            setSingleLine(true)
        }

        val nameInput = TextInputEditText(this).apply {
            hint = "Custom Name (optional)"
            inputType = InputType.TYPE_CLASS_TEXT
            setSingleLine(true)
        }

        container.addView(urlInput)
        container.addView(nameInput)

        MaterialAlertDialogBuilder(this)
            .setTitle("Enter ${listType.toUpperCase()} List Details")
            .setView(container)
            .setPositiveButton("Add") { _, _ ->
                val url = urlInput.text.toString().trim()
                val customName = nameInput.text.toString().trim()
                if (url.isNotEmpty()) {
                    addCustomList(listType, url, customName, pageType)
                } else {
                    Toast.makeText(this, "Please enter a valid URL or ID", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun addCustomList(listType: String, urlOrId: String, customName: String, pageType: String) {
        viewModel.addCustomList(listType, urlOrId, customName, pageType)
        Toast.makeText(this, "Custom list added", Toast.LENGTH_SHORT).show()
    }

    private fun showTraktListsDialog(pageType: String) {
        // Check if Trakt is enabled
        if (!prefsManager.isTraktEnabled()) {
            Toast.makeText(this, "Please connect your Trakt account first", Toast.LENGTH_SHORT).show()
            return
        }

        val accessToken = prefsManager.getTraktAccessToken()
        if (accessToken.isNullOrEmpty()) {
            Toast.makeText(this, "Trakt authentication required", Toast.LENGTH_SHORT).show()
            return
        }

        // Show loading dialog
        val loadingDialog = MaterialAlertDialogBuilder(this)
            .setTitle("Loading Trakt Lists")
            .setMessage("Fetching your lists from Trakt...")
            .setCancelable(false)
            .create()
        loadingDialog.show()

        // Fetch lists from Trakt
        lifecycleScope.launch {
            try {
                val lists = withContext(Dispatchers.IO) {
                    com.example.stremiompvplayer.network.TraktClient.api.getUserLists(
                        token = "Bearer $accessToken",
                        clientId = com.example.stremiompvplayer.utils.Secrets.TRAKT_CLIENT_ID,
                        username = "me" // "me" refers to the authenticated user
                    )
                }

                loadingDialog.dismiss()

                if (lists.isEmpty()) {
                    MaterialAlertDialogBuilder(this@SettingsActivity)
                        .setTitle("No Lists Found")
                        .setMessage("You don't have any custom lists on Trakt yet. Create lists on Trakt.tv to add them here.")
                        .setPositiveButton("OK", null)
                        .show()
                    return@launch
                }

                // Show list selection dialog
                val listNames = lists.map { "${it.name} (${it.item_count} items)" }.toTypedArray()
                MaterialAlertDialogBuilder(this@SettingsActivity)
                    .setTitle("Select Trakt List - ${pageType.capitalize()}")
                    .setItems(listNames) { _, which ->
                        val selectedList = lists[which]
                        addTraktList(selectedList, pageType)
                    }
                    .setNegativeButton("Cancel", null)
                    .show()

            } catch (e: Exception) {
                loadingDialog.dismiss()
                MaterialAlertDialogBuilder(this@SettingsActivity)
                    .setTitle("Error")
                    .setMessage("Failed to fetch Trakt lists: ${e.message}")
                    .setPositiveButton("OK", null)
                    .show()
            }
        }
    }

    private fun addTraktList(traktList: com.example.stremiompvplayer.network.TraktList, pageType: String) {
        // The list slug is used as the identifier
        viewModel.addCustomList("trakt", "me/${traktList.ids.slug}", traktList.name, pageType)
        Toast.makeText(this, "${traktList.name} added to $pageType", Toast.LENGTH_SHORT).show()
    }

    private fun setupUserSection() {
        val userId = prefsManager.getCurrentUserId()
        binding.tvCurrentUser.text = "Current User ID: $userId"
    }

    // ==============================
    //   IMPORT/EXPORT SECTION
    // ==============================

    private fun setupImportExportButtons() {
        binding.btnExportProfile.setOnClickListener {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "strub_profile_$timestamp.json"
            try {
                exportLauncher.launch(fileName)
            } catch (e: Exception) {
                // Fallback: Share via ACTION_SEND if document picker unavailable
                exportViaShare(fileName)
            }
        }

        binding.btnImportProfile.setOnClickListener {
            MaterialAlertDialogBuilder(this)
                .setTitle("Import Profile Settings")
                .setMessage("Importing will replace all current settings, users, and integrations. Continue?")
                .setPositiveButton("Import") { _, _ ->
                    importLauncher.launch(arrayOf("application/json"))
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun exportToFile(uri: Uri) {
        try {
            val exportJson = prefsManager.exportProfileSettings()
            contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(exportJson.toByteArray())
            }
            Toast.makeText(this, "Profile settings exported successfully!", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Export failed: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun exportViaShare(fileName: String) {
        try {
            val exportJson = prefsManager.exportProfileSettings()

            // Save to cache directory first
            val cacheFile = File(cacheDir, fileName)
            cacheFile.writeText(exportJson)

            // Create content URI using FileProvider
            val contentUri = androidx.core.content.FileProvider.getUriForFile(
                this,
                "${applicationContext.packageName}.fileprovider",
                cacheFile
            )

            // Share via ACTION_SEND
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/json"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                putExtra(Intent.EXTRA_SUBJECT, "Strub Profile Settings")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            startActivity(Intent.createChooser(shareIntent, "Export Settings"))
            Toast.makeText(this, "Share settings file to save it", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Export failed: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun importFromFile(uri: Uri) {
        try {
            val inputStream = contentResolver.openInputStream(uri)
            val reader = BufferedReader(InputStreamReader(inputStream))
            val jsonString = reader.use { it.readText() }

            val success = prefsManager.importProfileSettings(jsonString)
            if (success) {
                Toast.makeText(this, "Profile settings imported successfully! Please restart the app.", Toast.LENGTH_LONG).show()
                // Update UI to reflect imported data
                setupUserSection()
                updateTMDBTokenDisplay()
                updateTraktUI(prefsManager.isTraktEnabled())
                updateAIOStreamsDisplay()
                updateLiveTVDisplay()
            } else {
                Toast.makeText(this, "Import failed: Invalid file format", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Import failed: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}