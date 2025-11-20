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
            // Switch to Search Fragment and search
            binding.chipSearch.isChecked = true
            val searchFragment = SearchFragment()
            loadFragment(searchFragment)

            // Small delay to ensure fragment view created (cleaner ways exist but this is simple)
            binding.root.postDelayed({
                viewModel.searchTMDB(query)
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
                return true
            } else {
                when {
                    binding.chipDiscoverMovies.isChecked -> binding.chipDiscoverMovies.requestFocus()
                    binding.chipDiscoverSeries.isChecked -> binding.chipDiscoverSeries.requestFocus()
                    binding.chipMovies.isChecked -> binding.chipMovies.requestFocus()
                    binding.chipSeries.isChecked -> binding.chipSeries.requestFocus()
                    binding.chipSearch.isChecked -> binding.chipSearch.requestFocus()
                    else -> binding.navigationScroll.requestFocus()
                }
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }
}