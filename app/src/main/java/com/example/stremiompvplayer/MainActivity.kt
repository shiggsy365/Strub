package com.example.stremiompvplayer

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
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

        // Default to Discover Movies
        if (savedInstanceState == null) {
            binding.chipDiscoverMovies.isChecked = true
            loadFragment(DiscoverFragment.newInstance("movie"))
        }

        handleIntent(intent)

        binding.appLogo.setOnClickListener {
            startActivity(Intent(this, UserSelectionActivity::class.java))
        }

        // NAVIGATION SETUP
        binding.chipDiscoverMovies.setOnClickListener { loadFragment(DiscoverFragment.newInstance("movie")) }
        binding.chipDiscoverSeries.setOnClickListener { loadFragment(DiscoverFragment.newInstance("series")) }
        binding.chipMovies.setOnClickListener { loadFragment(MoviesFragment()) } // Library
        binding.chipSeries.setOnClickListener { loadFragment(SeriesFragment()) } // Library
        binding.chipSearch.setOnClickListener { loadFragment(SearchFragment()) }
        binding.chipMore.setOnClickListener { startActivity(Intent(this, SettingsActivity::class.java)) }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val query = intent?.getStringExtra("SEARCH_QUERY")
        if (!query.isNullOrEmpty()) {
            // 1. Focus the search chip immediately for visual feedback
            binding.chipSearch.requestFocus()

            // 2. Load the Search Fragment
            binding.chipSearch.isChecked = true
            val searchFragment = SearchFragment()
            loadFragment(searchFragment)

            // 3. Trigger the search via a post-delayed action to ensure fragment view is ready
            binding.root.postDelayed({
                // Check if the loaded fragment is indeed SearchFragment before triggering search
                val currentFragment = supportFragmentManager.findFragmentById(R.id.fragmentContainer)
                if (currentFragment is SearchFragment) {
                    viewModel.searchTMDB(query)

                    // OPTIONAL: Request focus for the actual Search View inside the fragment
                    // This assumes SearchFragment exposes a function like requestSearchViewFocus()
                    // For now, relying on the user's intent to switch to SearchFragment is enough.
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
        if (keyCode == KeyEvent.KEYCODE_BACK && event?.action == KeyEvent.ACTION_DOWN) {

            val currentFragment = supportFragmentManager.findFragmentById(R.id.fragmentContainer)

            // Check if the fragment handles back press (e.g., dismissing nested views)
            if (currentFragment is SeriesFragment && currentFragment.handleBackPress()) return true
            if (currentFragment is DiscoverFragment && currentFragment.handleBackPress()) return true

            val focus = currentFocus
            val isFocusOnMenu = focus == binding.appLogo ||
                    focus == binding.chipDiscoverMovies ||
                    focus == binding.chipDiscoverSeries ||
                    focus == binding.chipMovies ||
                    focus == binding.chipSeries ||
                    focus == binding.chipSearch ||
                    focus == binding.chipMore

            if (isFocusOnMenu) {
                // If focus is already on the menu, let the default Android back behaviour take over (i.e., exit the app)
                // If you want the app to stay open when D-pad Back is pressed from the menu, change this to 'return true'
                // For now, letting it exit:
                return super.onKeyDown(keyCode, event)
            } else {
                // If focus is elsewhere in the main fragment, move it back to the selected menu item.
                when {
                    binding.chipDiscoverMovies.isChecked -> binding.chipDiscoverMovies.requestFocus()
                    binding.chipDiscoverSeries.isChecked -> binding.chipDiscoverSeries.requestFocus()
                    binding.chipMovies.isChecked -> binding.chipMovies.requestFocus()
                    binding.chipSeries.isChecked -> binding.chipSeries.requestFocus()
                    binding.chipSearch.isChecked -> binding.chipSearch.requestFocus()
                    else -> binding.navigationScroll.requestFocus()
                }
                return true // Consume the back press to restore focus
            }
        }
        return super.onKeyDown(keyCode, event)
    }
}