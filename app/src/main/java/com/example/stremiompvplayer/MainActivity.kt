package com.example.stremiompvplayer

import android.content.Intent
import android.os.Bundle
import android.view.ContextThemeWrapper
import android.view.FocusFinder
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
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

    // ... (Existing properties) ...
    private lateinit var binding: ActivityMainBinding
    private val focusMemoryManager = FocusMemoryManager.getInstance()
    private var currentFragmentKey: String? = null
    private val navigationStack = mutableListOf<Pair<String, Fragment>>()

    private val viewModel: MainViewModel by viewModels {
        MainViewModelFactory(
            ServiceLocator.getInstance(applicationContext),
            SharedPreferencesManager.getInstance(this)
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // ... (Init logic same as before) ...
        val userId = SharedPreferencesManager.getInstance(this).getCurrentUserId()
        if (userId == null) { startActivity(Intent(this, UserSelectionActivity::class.java)); finish(); return }

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
            loadFragment(HomeFragment.newInstance())
        }

        handleIntent(intent)
        setupNetflixNavigation()
        setupLegacyNavigation()
    }

    private fun setupNetflixNavigation() {
        val sidebar = binding.root.findViewById<View>(R.id.netflixSidebar)

        sidebar.findViewById<View>(R.id.appLogo).setOnClickListener {
            startActivity(Intent(this, UserSelectionActivity::class.java))
            finish()
        }

        sidebar.findViewById<View>(R.id.sidebarHome).setOnClickListener {
            loadFragment(HomeFragment.newInstance())
        }

        sidebar.findViewById<View>(R.id.sidebarDiscover).setOnClickListener {
            // REMOVED dropdown check, defaulting to "movie" for now.
            // Fragment will handle its own type switching if needed.
            loadFragment(DiscoverFragment.newInstance("movie"))
        }

        sidebar.findViewById<View>(R.id.sidebarLibrary).setOnClickListener {
            // REMOVED dropdown check
            loadFragment(LibraryFragment.newInstance("movie"))
        }

        sidebar.findViewById<View>(R.id.sidebarSearch).setOnClickListener {
            showSearchDialog()
        }

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

        setupSidebarAutoHide(sidebar)
    }

    private fun setupSidebarAutoHide(sidebar: View) {
        val hideRunnable = Runnable {
            if (!sidebar.hasFocus() && !hasChildFocus(sidebar)) {
                sidebar.animate().translationX(-sidebar.width.toFloat()).setDuration(300).start()
            }
        }

        sidebar.post {
            sidebar.translationX = -sidebar.width.toFloat()
        }

        val onFocusChange = View.OnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                sidebar.removeCallbacks(hideRunnable)
                sidebar.animate().translationX(0f).setDuration(200).start()
            } else {
                // CHANGED: 2000 -> 50 (almost instant, but safe for focus transfer)
                sidebar.postDelayed(hideRunnable, 50)
            }
        }

        sidebar.onFocusChangeListener = onFocusChange
        val childViews = listOf(
            R.id.sidebarHome, R.id.sidebarDiscover, R.id.sidebarLibrary,
            R.id.sidebarSearch, R.id.sidebarLiveTV, R.id.sidebarSettings
        )
        childViews.forEach { viewId ->
            sidebar.findViewById<View>(viewId)?.onFocusChangeListener = onFocusChange
        }
    }

    private fun hasChildFocus(view: View): Boolean {
        if (view is ViewGroup) return view.hasFocus()
        return false
    }

    private fun showSearchDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_search, null)
        val searchInput = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.searchInput)

        val dialog = android.app.AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle("Search")
            .setView(dialogView)
            .setPositiveButton("Search") { _, _ ->
                val query = searchInput.text.toString()
                if (query.isNotEmpty()) {
                    performSearch(query)
                }
            }
            .setNegativeButton("Cancel", null)
            .create()

        dialog.show()

        // Focus search input and show keyboard
        searchInput.requestFocus()
        val imm = getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
        imm.showSoftInput(searchInput, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
    }

    private fun performSearch(query: String) {
        // Updated to remove dropdown check
        val currentFragment = supportFragmentManager.findFragmentById(R.id.fragmentContainer)
        when (currentFragment) {
            is DiscoverFragment -> currentFragment.performSearch(query)
            else -> {
                val discoverFragment = DiscoverFragment.newInstance("movie")
                loadFragment(discoverFragment)
                discoverFragment.view?.post { discoverFragment.performSearch(query) }
            }
        }
    }

    private fun loadFragment(fragment: Fragment, fragmentKey: String? = null, addToStack: Boolean = true) {
        // Save focus state before loading new fragment
        currentFragmentKey?.let { key ->
            currentFocus?.let { view ->
                focusMemoryManager.saveFocus(key, view)
            }

            // Push current fragment to navigation stack
            if (addToStack) {
                val currentFragment = supportFragmentManager.findFragmentById(R.id.fragmentContainer)
                if (currentFragment != null) {
                    navigationStack.add(Pair(key, currentFragment))
                    // Keep stack size reasonable (max 10 items)
                    if (navigationStack.size > 10) {
                        navigationStack.removeAt(0)
                    }
                }
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

        // Update media type toggle state based on fragment type
        val sidebar = binding.root.findViewById<View>(R.id.netflixSidebar)
        val mediaTypeToggle = sidebar.findViewById<View>(R.id.mediaTypeToggle)
        if (fragment is HomeFragment) {
            // Disable toggle on Home page
            mediaTypeToggle.isEnabled = false
            mediaTypeToggle.isFocusable = false
            mediaTypeToggle.alpha = 0.5f
        } else {
            // Enable toggle on other pages
            mediaTypeToggle.isEnabled = true
            mediaTypeToggle.isFocusable = true
            mediaTypeToggle.alpha = 1.0f
        }

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

        // 1. Allow HomeFragment to intercept keys (Fixes RecyclerView Down Press)
        if (currentFragment is HomeFragment) {
            if (currentFragment.handleKeyDown(keyCode, event)) return true
        }

        // 2. Handle Sidebar Unhide (Left Press)
        if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT && event?.action == KeyEvent.ACTION_DOWN) {
            val sidebar = binding.root.findViewById<View>(R.id.netflixSidebar)
            // Check if sidebar is effectively hidden (translated off mostly)
            if (sidebar.translationX < -50) {
                val currentFocus = currentFocus
                if (currentFocus != null && !isViewInSidebar(currentFocus, sidebar)) {
                    // We are in content. Check if next focus left is null (wall) or the sidebar itself
                    val nextFocus = FocusFinder.getInstance().findNextFocus(binding.root as ViewGroup, currentFocus, View.FOCUS_LEFT)

                    if (nextFocus == null || isViewInSidebar(nextFocus, sidebar)) {
                        // We hit the left edge, show sidebar
                        showSidebar(sidebar)
                        return true
                    }
                }
            }
        }

        // Legacy Navigation handling (if visible)
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
                // Default handling...
            }
        }

        if (keyCode == KeyEvent.KEYCODE_BACK && event?.action == KeyEvent.ACTION_DOWN) {
            // ... (existing back handling)
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
                startActivity(Intent(this, UserSelectionActivity::class.java))
                finish()
                return true
            }

            if (navigationStack.isNotEmpty()) {
                val (previousKey, previousFragment) = navigationStack.removeLast()
                currentFragmentKey = previousKey
                supportFragmentManager.beginTransaction().replace(R.id.fragmentContainer, previousFragment).commit()
                binding.root.postDelayed({
                    if (focusMemoryManager.hasFocusMemory(previousKey)) {
                        focusMemoryManager.restoreFocus(previousKey)
                    }
                }, 150)
                return true
            }

            // If on sidebar, exit
            val sidebar = binding.root.findViewById<View>(R.id.netflixSidebar)
            if (isViewInSidebar(focus ?: binding.root, sidebar)) {
                // Exit app or go to user selection
                startActivity(Intent(this, UserSelectionActivity::class.java))
                finish()
                return true
            }

            // Focus sidebar
            showSidebar(sidebar)
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun isViewInSidebar(view: View, sidebar: View): Boolean {
        if (view == sidebar) return true
        if (view.parent == sidebar) return true
        if (view.parent is View) return isViewInSidebar(view.parent as View, sidebar)
        return false
    }

    private fun showSidebar(sidebar: View) {
        sidebar.animate().translationX(0f).setDuration(200).start()
        // Focus home or first item
        sidebar.findViewById<View>(R.id.sidebarHome)?.requestFocus()
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
}