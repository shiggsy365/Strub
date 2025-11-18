package com.example.stremiompvplayer

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.stremiompvplayer.databinding.ActivityMainBinding
import com.example.stremiompvplayer.ui.discover.DiscoverFragment
import com.example.stremiompvplayer.ui.library.LibraryFragment
import com.example.stremiompvplayer.ui.movies.MoviesFragment
import com.example.stremiompvplayer.ui.search.SearchFragment
import com.example.stremiompvplayer.ui.series.SeriesFragment
// IMPORTS FIXED: Point to the utils package where we defined User and SharedPreferencesManager
import com.example.stremiompvplayer.utils.SharedPreferencesManager
import com.example.stremiompvplayer.utils.User
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    // CHANGED: Use SharedPreferencesManager instead of AppDatabase for users
    private lateinit var prefsManager: SharedPreferencesManager
    private var currentUserId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // CHANGED: Initialize the manager
        prefsManager = SharedPreferencesManager.getInstance(this)
        currentUserId = prefsManager.getCurrentUserId()

        setupUserAvatar()
        setupNavigation()

        // Load default tab
        if (savedInstanceState == null) {
            loadFragment(DiscoverFragment())
            binding.chipDiscover.isChecked = true
        }
    }

    private fun setupUserAvatar() {
        currentUserId?.let { userId ->
            // CHANGED: Use prefsManager
            val user = prefsManager.getUser(userId)
            user?.let {
                // Set avatar color
                binding.userAvatar.setBackgroundColor(it.avatarColor)

                // Set first initial
                binding.userInitial.text = it.name.first().uppercase()

                // Click listener for menu
                binding.userAvatarContainer.setOnClickListener {
                    showUserMenu()
                }
            }
        }
    }

    private fun showUserMenu() {
        val dialog = BottomSheetDialog(this)
        // Ensure dialog_user_menu.xml exists or replace with a standard view construction
        val view = layoutInflater.inflate(R.layout.dialog_user_menu, null)

        currentUserId?.let { userId ->
            // CHANGED: Use prefsManager
            val user = prefsManager.getUser(userId)
            user?.let {
                view.findViewById<TextView>(R.id.userName).text = it.name
                view.findViewById<View>(R.id.userAvatar).setBackgroundColor(it.avatarColor)
                view.findViewById<TextView>(R.id.userInitial).text = it.name.first().uppercase()
            }
        }

        view.findViewById<MaterialButton>(R.id.switchUserButton).apply {
            textSize = 22f // Larger font
            setOnClickListener {
                dialog.dismiss()
                startActivity(Intent(this@MainActivity, UserSelectionActivity::class.java))
                finish()
            }
        }

        view.findViewById<MaterialButton>(R.id.signOutButton).apply {
            textSize = 22f // Larger font
            setOnClickListener {
                dialog.dismiss()
                // CHANGED: Use prefsManager
                prefsManager.setCurrentUser("")
                val intent = Intent(this@MainActivity, UserSelectionActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
        }

        dialog.setContentView(view)
        dialog.show()
    }

    private fun setupNavigation() {
        binding.chipDiscover.setOnClickListener {
            loadFragment(DiscoverFragment())
        }

        binding.chipMovies.setOnClickListener {
            loadFragment(MoviesFragment())
        }

        binding.chipSeries.setOnClickListener {
            loadFragment(SeriesFragment())
        }

        binding.chipLibrary.setOnClickListener {
            loadFragment(LibraryFragment())
        }

        binding.chipSearch.setOnClickListener {
            loadFragment(SearchFragment())
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
}