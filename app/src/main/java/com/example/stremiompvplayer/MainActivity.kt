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
import com.example.stremiompvplayer.data.ServiceLocator
import com.example.stremiompvplayer.databinding.ActivityMainBinding
import com.example.stremiompvplayer.ui.discover.DiscoverFragment
import com.example.stremiompvplayer.ui.library.LibraryFragment
import com.example.stremiompvplayer.ui.movies.MoviesFragment
import com.example.stremiompvplayer.ui.search.SearchFragment
import com.example.stremiompvplayer.ui.series.SeriesFragment
import com.example.stremiompvplayer.utils.SharedPreferencesManager
import com.example.stremiompvplayer.viewmodels.MainViewModel
import com.example.stremiompvplayer.viewmodels.MainViewModelFactory
import com.google.android.material.chip.Chip

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

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

        viewModel.checkTMDBAuthAndSync()

        if (savedInstanceState == null) {
            // Default to Discover Movies
            binding.chipDiscover.isChecked = true
            binding.chipDiscover.text = "Discover: Movies"
            loadFragment(DiscoverFragment.newInstance("movie"))
        }

        handleIntent(intent)

        binding.appLogo.setOnClickListener {
            startActivity(Intent(this, UserSelectionActivity::class.java))
        }

        setupNavigation()
    }

    private fun setupNavigation() {
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

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        val currentFragment = supportFragmentManager.findFragmentById(R.id.fragmentContainer)

        if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN && event?.action == KeyEvent.ACTION_DOWN) {
            val focus = currentFocus
            val isFocusOnMenu = focus == binding.appLogo ||
                    focus == binding.chipDiscover ||
                    focus == binding.chipLibrary ||
                    focus == binding.chipSearch ||
                    focus == binding.chipMore

            if (isFocusOnMenu) {
                if (currentFragment is SearchFragment) {
                    currentFragment.focusSearch()
                    return true
                }
                if (currentFragment is DiscoverFragment) {
                    currentFragment.focusSidebar()
                    return true
                }
            }
        }

        if (keyCode == KeyEvent.KEYCODE_BACK && event?.action == KeyEvent.ACTION_DOWN) {
            if (currentFragment is SeriesFragment && currentFragment.handleBackPress()) return true
            if (currentFragment is DiscoverFragment && currentFragment.handleBackPress()) return true

            val focus = currentFocus
            val isFocusOnMenu = focus == binding.appLogo ||
                    focus == binding.chipDiscover ||
                    focus == binding.chipLibrary ||
                    focus == binding.chipSearch ||
                    focus == binding.chipMore

            if (isFocusOnMenu) {
                return super.onKeyDown(keyCode, event)
            } else {
                when {
                    binding.chipDiscover.isChecked -> binding.chipDiscover.requestFocus()
                    binding.chipLibrary.isChecked -> binding.chipLibrary.requestFocus()
                    binding.chipSearch.isChecked -> binding.chipSearch.requestFocus()
                    else -> binding.navigationScroll.requestFocus()
                }
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }
}
