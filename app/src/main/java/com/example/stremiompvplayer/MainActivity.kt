package com.example.stremiompvplayer

import android.content.Intent
import android.os.Bundle
import android.view.ContextThemeWrapper
import android.view.KeyEvent
import android.view.View
import android.widget.PopupMenu
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.stremiompvplayer.ui.home.HomeFragment
import com.example.stremiompvplayer.data.ServiceLocator
import com.example.stremiompvplayer.databinding.ActivityMainBinding
import com.example.stremiompvplayer.ui.discover.DiscoverFragment
import com.example.stremiompvplayer.ui.library.LibraryFragment
import com.example.stremiompvplayer.ui.livetv.LiveTVFragment
import com.example.stremiompvplayer.ui.movies.MoviesFragment
import com.example.stremiompvplayer.ui.search.SearchFragment
import com.example.stremiompvplayer.ui.series.SeriesFragment
import com.example.stremiompvplayer.utils.SharedPreferencesManager
import com.example.stremiompvplayer.utils.FocusMemoryManager
import com.example.stremiompvplayer.viewmodels.MainViewModel
import com.example.stremiompvplayer.viewmodels.MainViewModelFactory
import com.google.android.material.chip.Chip
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val focusMemoryManager = FocusMemoryManager.getInstance()
    private var currentFragmentKey: String? = null

    private val viewModel: MainViewModel by viewModels {
        MainViewModelFactory(
            ServiceLocator.getInstance(applicationContext),
            SharedPreferencesManager.getInstance(this)
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val userId = SharedPreferencesManager.getInstance(this).getCurrentUserId()
        if (userId == null) {
            startActivity(Intent(this, UserSelectionActivity::class.java))
            finish()
            return
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Show startup loading dialog
        val loadingDialog = StartupLoadingDialog(this)
        loadingDialog.show()

        // Load everything in parallel
        lifecycleScope.launch {
            // Task 1: Load user lists
            loadingDialog.setUserListsLoading()
            launch {
                viewModel.checkTMDBAuthAndSync()

                // Sync Trakt on app startup if enabled
                if (SharedPreferencesManager.getInstance(this@MainActivity).isTraktEnabled()) {
                    viewModel.syncTraktLibrary()
                }

                // Refresh home content (discover lists) on app startup
                viewModel.loadHomeContent()

                loadingDialog.setUserListsComplete()
            }

            // Task 2: Load missing metadata
            loadingDialog.setMetadataLoading()
            launch {
                viewModel.ensureLibraryMetadata()
                loadingDialog.setMetadataComplete()
            }

            // Task 3: Parse TV channels (only if 24 hours have passed)
            launch {
                val prefsManager = SharedPreferencesManager.getInstance(this@MainActivity)
                if (prefsManager.shouldRefreshTV()) {
                    loadingDialog.setTvChannelsLoading()
                    // Clear cache and refresh TV channels
                    LiveTVFragment.clearCache()
                    LiveTVFragment.loadEPGInBackground(this@MainActivity)
                    // Update last refresh time
                    prefsManager.setLastTVRefreshTime(System.currentTimeMillis())
                    loadingDialog.setTvChannelsComplete()
                } else {
                    // Skip refresh - using cached data
                    loadingDialog.setTvChannelsComplete()
                }
            }
        }

        if (savedInstanceState == null) {
            // Default to Home
            loadFragment(HomeFragment.newInstance())
        }

        handleIntent(intent)

        setupNetflixNavigation()
        setupLegacyNavigation()
    }

    private fun setupNetflixNavigation() {
        // Get the included layouts
        val sidebar = binding.root.findViewById<View>(R.id.netflixSidebar)
        val topBar = binding.root.findViewById<View>(R.id.netflixTopBar)

        // Setup sidebar navigation
        sidebar.findViewById<View>(R.id.sidebarHome).setOnClickListener {
            loadFragment(HomeFragment.newInstance())
        }

        sidebar.findViewById<View>(R.id.sidebarDiscover).setOnClickListener {
            val currentType = topBar.findViewById<android.widget.TextView>(R.id.dropdownMediaType).text.toString()
            val type = if (currentType.contains("Series", ignoreCase = true)) "series" else "movie"
            loadFragment(DiscoverFragment.newInstance(type))
        }

        sidebar.findViewById<View>(R.id.sidebarLibrary).setOnClickListener {
            val currentType = topBar.findViewById<android.widget.TextView>(R.id.dropdownMediaType).text.toString()
            val type = if (currentType.contains("Series", ignoreCase = true)) "series" else "movie"
            loadFragment(LibraryFragment.newInstance(type))
        }

        // LIVE TV - only show if configured
        val liveTvConfigured = SharedPreferencesManager.getInstance(this).getLiveTVM3UUrl()?.isNotEmpty() == true
        if (liveTvConfigured) {
            sidebar.findViewById<View>(R.id.sidebarLiveTV).visibility = View.VISIBLE
            sidebar.findViewById<View>(R.id.sidebarLiveTV).setOnClickListener {
                loadFragment(LiveTVFragment.newInstance())
            }
        } else {
            sidebar.findViewById<View>(R.id.sidebarLiveTV).visibility = View.GONE
        }

        sidebar.findViewById<View>(R.id.sidebarSettings).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        // Setup top bar
        val dropdownMediaType = topBar.findViewById<android.widget.TextView>(R.id.dropdownMediaType)
        val currentListLabel = topBar.findViewById<android.widget.TextView>(R.id.currentListLabel)
        val searchField = topBar.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.searchField)
        val btnUserProfile = topBar.findViewById<View>(R.id.btnUserProfile)

        // Store reference to currentListLabel for DiscoverFragment to update
        currentListLabel.tag = "currentListLabel"

        // Media Type dropdown (Movies/Series)
        dropdownMediaType.setOnClickListener { view ->
            showMenu(view, listOf("Movies", "Series")) { selection ->
                dropdownMediaType.text = selection
                // Refresh current fragment with new media type
                val currentFragment = supportFragmentManager.findFragmentById(R.id.fragmentContainer)
                when (currentFragment) {
                    is DiscoverFragment -> {
                        val type = if (selection == "Series") "series" else "movie"
                        loadFragment(DiscoverFragment.newInstance(type))
                    }
                    is LibraryFragment -> {
                        val type = if (selection == "Series") "series" else "movie"
                        loadFragment(LibraryFragment.newInstance(type))
                    }
                }
            }
        }

        // Search field
        searchField.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH) {
                val query = searchField.text.toString()
                if (query.isNotEmpty()) {
                    viewModel.searchTMDB(query)
                    val searchFragment = SearchFragment()
                    loadFragment(searchFragment)
                    searchFragment.setSearchText(query)
                }
                true
            } else {
                false
            }
        }

        // User profile button
        btnUserProfile.setOnClickListener {
            startActivity(Intent(this, UserSelectionActivity::class.java))
        }

        // Implement sidebar auto-hide behavior
        setupSidebarAutoHide(sidebar)
    }

    private fun setupSidebarAutoHide(sidebar: View) {
        // Auto-hide sidebar when focus leaves it
        sidebar.setOnFocusChangeListener { v, hasFocus ->
            lifecycleScope.launch {
                if (!hasFocus) {
                    delay(3000) // Wait 3 seconds before hiding
                    if (!sidebar.hasFocus()) {
                        // Animate sidebar width to icon-only mode
                        sidebar.animate().translationX(-sidebar.width.toFloat() + 80).setDuration(300).start()
                    }
                } else {
                    // Show full sidebar when focused
                    sidebar.animate().translationX(0f).setDuration(300).start()
                }
            }
        }

        // Also check for focus on child views
        val childViews = listOf(
            R.id.sidebarHome, R.id.sidebarDiscover, R.id.sidebarLibrary,
            R.id.sidebarLiveTV, R.id.sidebarSettings
        )
        childViews.forEach { viewId ->
            sidebar.findViewById<View>(viewId)?.setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) {
                    sidebar.animate().translationX(0f).setDuration(300).start()
                }
            }
        }
    }

    private fun setupLegacyNavigation() {
        // HOME
        binding.chipHome.setOnClickListener {
            loadFragment(HomeFragment.newInstance())
        }

        // DISCOVER DROP DOWN
        binding.chipDiscover.setOnClickListener { view ->
            showMenu(view, listOf("Movies", "Series")) { selection ->
                when (selection) {
                    "Movies" -> {
                        binding.chipDiscover.text = "Discover: Movies"
                        loadFragment(DiscoverFragment.newInstance("movie"))
                    }
                    "Series" -> {
                        binding.chipDiscover.text = "Discover: Series"
                        loadFragment(DiscoverFragment.newInstance("series"))
                    }
                }
            }
        }

        // LIBRARY DROP DOWN
        binding.chipLibrary.setOnClickListener { view ->
            showMenu(view, listOf("Movies", "Series")) { selection ->
                when (selection) {
                    "Movies" -> {
                        binding.chipLibrary.text = "Library: Movies"
                        loadFragment(LibraryFragment.newInstance("movie"))
                    }
                    "Series" -> {
                        binding.chipLibrary.text = "Library: Series"
                        loadFragment(LibraryFragment.newInstance("series"))
                    }
                }
            }
        }

        binding.chipSearch.setOnClickListener {
            loadFragment(SearchFragment())
        }

        // LIVE TV - only show if configured
        val liveTvConfigured = SharedPreferencesManager.getInstance(this).getLiveTVM3UUrl()?.isNotEmpty() == true
        if (liveTvConfigured) {
            binding.chipLiveTV.visibility = View.VISIBLE
            binding.chipLiveTV.setOnClickListener {
                loadFragment(LiveTVFragment.newInstance())
            }
        } else {
            binding.chipLiveTV.visibility = View.GONE
        }

        binding.chipMore.setOnClickListener { startActivity(Intent(this, SettingsActivity::class.java)) }
    }

    private fun showMenu(view: View, items: List<String>, onSelect: (String) -> Unit) {
        // Use ContextThemeWrapper for Black Text on Light Background popup
        val wrapper = ContextThemeWrapper(this, android.R.style.Theme_DeviceDefault_Light_NoActionBar)
        val popup = PopupMenu(wrapper, view)
        items.forEach { popup.menu.add(it) }
        popup.setOnMenuItemClickListener {
            onSelect(it.title.toString())
            true
        }
        popup.show()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val personId = intent?.getIntExtra("SEARCH_PERSON_ID", -1) ?: -1
        val query = intent?.getStringExtra("SEARCH_QUERY")

        if (personId != -1) {
            binding.chipSearch.requestFocus()
            binding.chipSearch.isChecked = true

            val searchFragment = SearchFragment()
            loadFragment(searchFragment)

            binding.root.postDelayed({
                val currentFragment = supportFragmentManager.findFragmentById(R.id.fragmentContainer)
                if (currentFragment is SearchFragment) {
                    if (!query.isNullOrEmpty()) {
                        currentFragment.setSearchText(query)
                    }
                    currentFragment.searchByPersonId(personId)
                }
            }, 100)

        } else if (!query.isNullOrEmpty()) {
            binding.chipSearch.requestFocus()
            binding.chipSearch.isChecked = true
            val searchFragment = SearchFragment()
            loadFragment(searchFragment)

            binding.root.postDelayed({
                val currentFragment = supportFragmentManager.findFragmentById(R.id.fragmentContainer)
                if (currentFragment is SearchFragment) {
                    currentFragment.setSearchText(query)
                    viewModel.searchTMDB(query)
                }
            }, 100)
        }
    }

    private fun loadFragment(fragment: Fragment, fragmentKey: String? = null) {
        // Save focus state before loading new fragment
        currentFragmentKey?.let { key ->
            currentFocus?.let { view ->
                focusMemoryManager.saveFocus(key, view)
            }
        }

        // Update current fragment key
        currentFragmentKey = fragmentKey ?: when (fragment) {
            is HomeFragment -> "home"
            is DiscoverFragment -> focusMemoryManager.getFragmentKey("discover", fragment.arguments?.getString("type") ?: "movie")
            is LibraryFragment -> focusMemoryManager.getFragmentKey("library", fragment.arguments?.getString("type") ?: "movie")
            is SearchFragment -> "search"
            is LiveTVFragment -> "livetv"
            is SeriesFragment -> focusMemoryManager.getFragmentKey("series", fragment.arguments?.getString("seriesId") ?: "unknown")
            else -> fragment.javaClass.simpleName
        }

        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()

        // Restore focus after fragment is loaded
        currentFragmentKey?.let { key ->
            if (focusMemoryManager.hasFocusMemory(key)) {
                binding.root.postDelayed({
                    focusMemoryManager.restoreFocus(key)
                }, 150)
            }
        }
    }

    override fun onPause() {
        super.onPause()
        // Save current focus state
        currentFragmentKey?.let { key ->
            currentFocus?.let { view ->
                focusMemoryManager.saveFocus(key, view)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Clean up stale focus memory
        focusMemoryManager.cleanupStale()

        // Restore focus if available
        currentFragmentKey?.let { key ->
            if (focusMemoryManager.hasFocusMemory(key)) {
                binding.root.postDelayed({
                    focusMemoryManager.restoreFocus(key)
                }, 100)
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        val currentFragment = supportFragmentManager.findFragmentById(R.id.fragmentContainer)

        if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN && event?.action == KeyEvent.ACTION_DOWN) {
            val focus = currentFocus
            val isFocusOnMenu = focus == binding.btnAppLogo ||
                    focus == binding.chipHome ||
                    focus == binding.chipDiscover ||
                    focus == binding.chipLibrary ||
                    focus == binding.chipSearch ||
                    focus == binding.chipLiveTV ||
                    focus == binding.chipMore

            if (isFocusOnMenu) {
                // Check if we have saved focus to restore
                currentFragmentKey?.let { key ->
                    if (focusMemoryManager.hasFocusMemory(key)) {
                        val restored = focusMemoryManager.restoreFocus(key)
                        if (restored) return true
                    }
                }

                // Otherwise use default fragment-specific focus
                val focusedSuccessfully = when (currentFragment) {
                    is SearchFragment -> {
                        currentFragment.focusSearch()
                        true
                    }
                    is DiscoverFragment -> {
                        currentFragment.focusSidebar()
                    }
                    is HomeFragment -> {
                        currentFragment.focusSidebar()
                    }
                    is LibraryFragment -> {
                        currentFragment.focusSidebar()
                    }
                    is LiveTVFragment -> {
                        currentFragment.focusSidebar()
                    }
                    else -> false
                }

                // If focus was successfully moved, consume the event
                if (focusedSuccessfully) {
                    return true
                }

                // Fallback: try to find any focusable view in the fragment
                val fragmentContainer = findViewById<View>(R.id.fragmentContainer)
                val focusableView = fragmentContainer?.findFocus() ?: fragmentContainer?.focusSearch(View.FOCUS_DOWN)
                if (focusableView != null && focusableView != focus) {
                    focusableView.requestFocus()
                    return true
                }
            }
        }

        if (keyCode == KeyEvent.KEYCODE_BACK && event?.action == KeyEvent.ACTION_DOWN) {
            if (currentFragment is SeriesFragment && currentFragment.handleBackPress()) return true
            if (currentFragment is DiscoverFragment && currentFragment.handleBackPress()) return true

            val focus = currentFocus
            val isFocusOnMenu = focus == binding.btnAppLogo ||
                    focus == binding.chipHome ||
                    focus == binding.chipDiscover ||
                    focus == binding.chipLibrary ||
                    focus == binding.chipSearch ||
                    focus == binding.chipLiveTV ||
                    focus == binding.chipMore

            if (isFocusOnMenu) {
                // Navigate to user selection instead of exiting app
                startActivity(Intent(this, UserSelectionActivity::class.java))
                finish()
                return true
            } else {
                // Save current focus before returning to menu
                currentFragmentKey?.let { key ->
                    currentFocus?.let { view ->
                        focusMemoryManager.saveFocus(key, view)
                    }
                }

                when {
                    binding.chipHome.isChecked -> binding.chipHome.requestFocus()
                    binding.chipDiscover.isChecked -> binding.chipDiscover.requestFocus()
                    binding.chipLibrary.isChecked -> binding.chipLibrary.requestFocus()
                    binding.chipSearch.isChecked -> binding.chipSearch.requestFocus()
                    binding.chipLiveTV.isChecked -> binding.chipLiveTV.requestFocus()
                    else -> binding.navigationScroll.requestFocus()
                }
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }
}