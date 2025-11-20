package com.example.stremiompvplayer

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.stremiompvplayer.databinding.ActivityMainBinding
import com.example.stremiompvplayer.ui.library.LibraryFragment
import com.example.stremiompvplayer.ui.movies.MoviesFragment
import com.example.stremiompvplayer.ui.series.SeriesFragment
import com.example.stremiompvplayer.ui.search.SearchFragment
import com.example.stremiompvplayer.utils.SharedPreferencesManager

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Check if user is selected
        val userId = SharedPreferencesManager.getInstance(this).getCurrentUserId()
        if (userId == null) {
            startActivity(Intent(this, UserSelectionActivity::class.java))
            finish()
            return
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Default fragment
        if (savedInstanceState == null) {
            loadFragment(MoviesFragment())
        }

        // CLICK LISTENER FOR LOGO -> USER SELECTION
        binding.appLogo.setOnClickListener {
            startActivity(Intent(this, UserSelectionActivity::class.java))
        }

        // Setup navigation chips
        binding.chipMovies.isChecked = true

        binding.chipMovies.setOnClickListener {
            loadFragment(MoviesFragment())
        }

        binding.chipSeries.setOnClickListener {
            loadFragment(SeriesFragment())
        }

        binding.chipSearch.setOnClickListener {
            loadFragment(SearchFragment())
        }

        binding.chipLibrary.setOnClickListener {
            loadFragment(LibraryFragment())
        }

        binding.chipMore.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }

    // Handle DPAD keys for TV navigation
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK && event?.action == KeyEvent.ACTION_DOWN) {

            // 1. Ask the current fragment if it wants to handle the back press (Drill Up)
            val currentFragment = supportFragmentManager.findFragmentById(R.id.fragmentContainer)
            if (currentFragment is SeriesFragment) {
                if (currentFragment.handleBackPress()) {
                    return true // Fragment handled it (went up a level)
                }
            }

            // 2. If Fragment didn't handle it, we are at the top level.
            // Check if focus is already on the Navigation/Menu or Logo
            val focus = currentFocus
            val isFocusOnMenu = focus == binding.appLogo ||
                    focus == binding.chipMovies ||
                    focus == binding.chipSeries ||
                    focus == binding.chipLibrary ||
                    focus == binding.chipSearch ||
                    focus == binding.chipDiscover ||
                    focus == binding.chipMore

            if (isFocusOnMenu) {
                // If we are on the menu, DO NOT exit. Consume event.
                return true
            } else {
                // If we are in the content, Move focus to the active Menu Chip
                when {
                    binding.chipMovies.isChecked -> binding.chipMovies.requestFocus()
                    binding.chipSeries.isChecked -> binding.chipSeries.requestFocus()
                    binding.chipLibrary.isChecked -> binding.chipLibrary.requestFocus()
                    binding.chipSearch.isChecked -> binding.chipSearch.requestFocus()
                    binding.chipDiscover.isChecked -> binding.chipDiscover.requestFocus()
                    else -> binding.navigationScroll.requestFocus()
                }
                return true // Consumed
            }
        }
        return super.onKeyDown(keyCode, event)
    }
}