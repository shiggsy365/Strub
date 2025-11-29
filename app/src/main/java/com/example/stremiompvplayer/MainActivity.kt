// Kotlin
package com.example.stremiompvplayer

import com.example.stremiompvplayer.ui.search.SearchResultsFragment
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
import com.example.stremiompvplayer.ui.movies.MoviesFragment
import com.example.stremiompvplayer.ui.series.SeriesFragment
import com.example.stremiompvplayer.ui.livetv.LiveTVFragment
import com.example.stremiompvplayer.ui.search.SearchFragment
import com.example.stremiompvplayer.utils.SharedPreferencesManager
import com.example.stremiompvplayer.utils.FocusMemoryManager
import com.example.stremiompvplayer.utils.TraktSyncScheduler
import com.example.stremiompvplayer.viewmodels.MainViewModel
import com.example.stremiompvplayer.viewmodels.MainViewModelFactory
import com.google.android.material.chip.Chip
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

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
        val userId = SharedPreferencesManager.getInstance(this).getCurrentUserId()
        if (userId == null) { startActivity(Intent(this, UserSelectionActivity::class.java)); finish(); return }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Show startup loading dialog
        val loadingDialog = StartupLoadingDialog(this)
        loadingDialog.show()

        // Load everything with proper sequencing
        lifecycleScope.launch {
            // Task 0: Remove duplicate lists (runs first, in background)
            launch {
                viewModel.removeDuplicateLists()
            }

            // Task 1: Load user lists and sync Trakt
            loadingDialog.setUserListsLoading()
            val traktSyncJob = launch {
                viewModel.checkTMDBAuthAndSync()

                // Sync Trakt on app startup if enabled
                val prefsManager = SharedPreferencesManager.getInstance(this@MainActivity)
                if (prefsManager.isTraktEnabled() && prefsManager.isAutoSyncOnStartup()) {
                    viewModel.syncTraktLibrary()
                    prefsManager.setLastTraktSyncTime(System.currentTimeMillis())
                }

                // Schedule background sync if enabled
                TraktSyncScheduler.schedulePeriodicSync(this@MainActivity)

                // Refresh home content (discover lists) on app startup
                // Note: HomeViewModel now handles its own loading, but MainViewModel might still be used for other things
                // viewModel.loadHomeContent() // Removed as HomeViewModel handles this now

                loadingDialog.setUserListsComplete()
            }

            // Task 2: Load missing metadata AFTER Trakt sync completes
            loadingDialog.setMetadataLoading()
            launch {
                // Wait for Trakt sync to complete first
                traktSyncJob.join()
                // Now ensure all library items have TMDB metadata
                viewModel.ensureLibraryMetadata()
                loadingDialog.setMetadataComplete()
            }

            // Task 3: Parse TV channels (on first load or if 24 hours have passed) - runs in parallel
            launch {
                val prefsManager = SharedPreferencesManager.getInstance(this@MainActivity)
                val cacheIsEmpty = !LiveTVFragment.isCachePopulated()
                val shouldRefresh = prefsManager.shouldRefreshTV()

                if (cacheIsEmpty || shouldRefresh) {
                    loadingDialog.setTvChannelsLoading()
                    // Clear cache and refresh TV channels
                    if (shouldRefresh) {
                        LiveTVFragment.clearCache()
                    }
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
            loadFragment(HomeFragment())
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
            loadFragment(HomeFragment())
        }

        sidebar.findViewById<View>(R.id.sidebarMovies).setOnClickListener {
            loadFragment(MoviesFragment())
        }

        sidebar.findViewById<View>(R.id.sidebarSeries).setOnClickListener {
            loadFragment(SeriesFragment())
        }

        sidebar.findViewById<View>(R.id.sidebarSearch).setOnClickListener {
            showSearchDialog()
        }

        sidebar.findViewById<View>(R.id.sidebarLibrary).setOnClickListener {
            loadFragment(com.example.stremiompvplayer.ui.library.LibraryFragment())
        }

        val liveTvConfigured = SharedPreferencesManager.getInstance(this).getLiveTVM3UUrl()?.isNotEmpty() == true
        if (liveTvConfigured) {
            sidebar.findViewById<View>(R.id.sidebarLiveTV).visibility = View.VISIBLE
            sidebar.findViewById<View>(R.id.sidebarLiveTV).setOnClickListener {
                loadFragment(LiveTVFragment())
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
                sidebar.animate().translationX(-sidebar.width.toFloat()).setDuration(100).start()
            }
        }

        sidebar.post {
            sidebar.translationX = -sidebar.width.toFloat()
        }

        val onFocusChange = View.OnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                sidebar.removeCallbacks(hideRunnable)
                sidebar.animate().translationX(0f).setDuration(100).start()
            } else {
                // CHANGED: 2000 -> 50 (almost instant, but safe for focus transfer)
                sidebar.postDelayed(hideRunnable, 50)
            }
        }

        sidebar.onFocusChangeListener = onFocusChange
        val childViews = listOf(
            R.id.sidebarHome, R.id.sidebarMovies, R.id.sidebarSeries,
            R.id.sidebarSearch, R.id.sidebarLibrary, R.id.sidebarLiveTV, R.id.sidebarSettings
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

        val dialog = com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
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

        // Apply OutlinedButton style to dialog buttons
        dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE)?.apply {
            setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_LabelLarge)
        }
        dialog.getButton(android.app.AlertDialog.BUTTON_NEGATIVE)?.apply {
            setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_LabelLarge)
        }

        // Focus search input and show keyboard
        searchInput.requestFocus()
        val imm = getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
        imm.showSoftInput(searchInput, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
    }

    private fun performSearch(query: String) {
        // Navigate to search fragment with query
        val searchFragment = SearchFragment()
        loadFragment(searchFragment)
        // Use postDelayed with longer delay to ensure fragment view is ready
        binding.root.postDelayed({
            val currentFragment = supportFragmentManager.findFragmentById(R.id.fragmentContainer)
            if (currentFragment is SearchFragment) {

                currentFragment.performSearch(query)
            }
        }, 300)
    }

    fun loadFragment(fragment: Fragment, fragmentKey: String? = null, addToStack: Boolean = true) {
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
            is MoviesFragment -> "movies"
            is SeriesFragment -> "series"
            is SearchFragment -> "search"
            is LiveTVFragment -> "livetv"
            is com.example.stremiompvplayer.ui.library.LibraryFragment -> "library"
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

        // 1. Allow specific fragments to intercept keys
        // HomeFragment now handles key events for cast results cycling
        if (currentFragment is HomeFragment) {
            if (currentFragment.handleKeyDown(keyCode, event)) return true
        }

        if (currentFragment is MoviesFragment) {
            if (currentFragment.handleKeyDown(keyCode, event)) return true
        }

        if (currentFragment is SeriesFragment) {
            if (currentFragment.handleKeyDown(keyCode, event)) return true
        }

        if (currentFragment is com.example.stremiompvplayer.ui.livetv.LiveTVFragment) {
            if (currentFragment.handleKeyDown(keyCode, event)) return true
        }

        // 2. Handle Sidebar Unhide (Left Press) and Hide (Right Press)
        val sidebar = binding.root.findViewById<View>(R.id.netflixSidebar)

        // Handle RIGHT press from sidebar - hide the sidebar
        if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT && event?.action == KeyEvent.ACTION_DOWN) {
            val currentFocus = currentFocus
            if (currentFocus != null && isViewInSidebar(currentFocus, sidebar)) {
                // Hide sidebar and move focus to fragment content
                sidebar.animate().translationX(-sidebar.width.toFloat()).setDuration(100).start()
                val currentFragment = supportFragmentManager.findFragmentById(R.id.fragmentContainer)

                // Call focusSidebar only if it exists
                if (currentFragment is HomeFragment) {
                    currentFragment.focusSidebar()
                } else if (currentFragment is MoviesFragment) {
                    currentFragment.focusSidebar()
                } else if (currentFragment is SeriesFragment) {
                    currentFragment.focusSidebar()
                } else if (currentFragment is com.example.stremiompvplayer.ui.livetv.LiveTVFragment) {
                    currentFragment.focusSidebar()
                }
                return true
            }
        }

        // Handle LEFT press to show sidebar
        if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT && event?.action == KeyEvent.ACTION_DOWN) {
            // Check if sidebar is effectively hidden (translated off mostly)
            if (sidebar.translationX < -50) {
                val currentFocus = currentFocus
                if (currentFocus != null && !isViewInSidebar(currentFocus, sidebar)) {
                    // Check if the focused view is inside a RecyclerView (poster carousel)
                    val recyclerView = getParentRecyclerView(currentFocus)

                    if (recyclerView != null) {
                        // We are in a RecyclerView. Check if we're at the first item
                        val position = recyclerView.getChildAdapterPosition(currentFocus)
                        if (position == 0) {
                            // At first item, show sidebar
                            showSidebar(sidebar)
                            return true
                        }
                        // Not at first item, let RecyclerView handle horizontal scrolling
                    } else {
                        // We are not in a RecyclerView. Check if next focus left is null (wall) or the sidebar itself
                        val nextFocus = FocusFinder.getInstance().findNextFocus(binding.root as ViewGroup, currentFocus, View.FOCUS_LEFT)

                        if (nextFocus == null || isViewInSidebar(nextFocus, sidebar)) {
                            // We hit the left edge, show sidebar
                            showSidebar(sidebar)
                            return true
                        }
                    }
                }
            }
        }

        // Legacy Navigation handling (if visible)
        if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN && event?.action == KeyEvent.ACTION_DOWN) {
            val focus = currentFocus
            val isFocusOnMenu = focus == binding.btnAppLogo ||
                    focus == binding.chipHome ||
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
            // Allow specific fragments to intercept back press
            if (currentFragment is HomeFragment && currentFragment.handleBackPress()) return true
            if (currentFragment is MoviesFragment && currentFragment.handleBackPress()) return true
            if (currentFragment is SeriesFragment && currentFragment.handleBackPress()) return true
            if (currentFragment is SearchFragment && currentFragment.handleBackPress()) return true
            if (currentFragment is com.example.stremiompvplayer.ui.library.LibraryFragment && currentFragment.handleBackPress()) return true

            // Use navigation stack for back navigation - NEVER exit app
            if (navigationStack.isNotEmpty()) {
                val (previousKey, previousFragment) = navigationStack.removeAt(navigationStack.size - 1)
                currentFragmentKey = previousKey
                supportFragmentManager.beginTransaction().replace(R.id.fragmentContainer, previousFragment).commit()
                binding.root.postDelayed({
                    if (focusMemoryManager.hasFocusMemory(previousKey)) {
                        focusMemoryManager.restoreFocus(previousKey)
                    }
                }, 150)
                return true
            }

            // If navigation stack is empty, go to Home fragment
            if (currentFragment !is HomeFragment) {
                loadFragment(HomeFragment(), addToStack = false)
                return true
            }

            // If already on Home, show sidebar instead of exiting
            val sidebar = binding.root.findViewById<View>(R.id.netflixSidebar)
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

    private fun isViewInRecyclerView(view: View): Boolean {
        var parent = view.parent
        while (parent != null) {
            if (parent is androidx.recyclerview.widget.RecyclerView) {
                return true
            }
            parent = parent.parent
        }
        return false
    }

    private fun getParentRecyclerView(view: View): androidx.recyclerview.widget.RecyclerView? {
        var parent = view.parent
        while (parent != null) {
            if (parent is androidx.recyclerview.widget.RecyclerView) {
                return parent
            }
            parent = parent.parent
        }
        return null
    }

    private fun showSidebar(sidebar: View) {
        sidebar.animate().translationX(0f).setDuration(200).start()
        // Focus home or first item
        sidebar.findViewById<View>(R.id.sidebarHome)?.requestFocus()
    }

    private fun setupLegacyNavigation() {
        // HOME
        binding.chipHome.setOnClickListener {
            loadFragment(HomeFragment())
        }

        // Legacy chips are no longer used but keeping the bindings for backward compatibility
        // The legacy navigation container is hidden by default (visibility="gone" in XML)

        binding.chipSearch.setOnClickListener {
            loadFragment(SearchFragment())
        }

        // LIVE TV - only show if configured
        val liveTvConfigured = SharedPreferencesManager.getInstance(this).getLiveTVM3UUrl()?.isNotEmpty() == true
        if (liveTvConfigured) {
            binding.chipLiveTV.visibility = View.VISIBLE
            binding.chipLiveTV.setOnClickListener {
                loadFragment(LiveTVFragment())
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

                    viewModel.searchTMDB(query)
                }
            }, 100)
        }
    }
}
