package com.example.stremiompvplayer

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.stremiompvplayer.data.AppDatabase
import com.example.stremiompvplayer.databinding.ActivityMainBinding
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private var binding: ActivityMainBinding? = null
    private lateinit var database: AppDatabase
    private var currentUserId: String? = null
    private var isInitialized = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            binding = ActivityMainBinding.inflate(layoutInflater)
            setContentView(binding?.root)

            database = AppDatabase.getInstance(this)
            currentUserId = database.getCurrentUserId()

            // AUTO-CREATE USER - Don't redirect to UserSelectionActivity
            if (currentUserId == null) {
                Log.d("MainActivity", "No user found, creating default user")
                try {
                    val defaultUser = database.createUser("My Profile", 0xFFE50914.toInt())
                    database.setCurrentUser(defaultUser.id)
                    currentUserId = defaultUser.id
                    Toast.makeText(this, "Welcome! Created your profile", Toast.LENGTH_LONG).show()
                } catch (e: Exception) {
                    Log.e("MainActivity", "Error creating user", e)
                    currentUserId = "default_user"
                    Toast.makeText(this, "Running in demo mode", Toast.LENGTH_SHORT).show()
                }
            }

            Log.d("MainActivity", "Current user ID: $currentUserId")

            setupUI()
            setupFocusHandling()
            isInitialized = true
            loadContent()

        } catch (e: Exception) {
            Log.e("MainActivity", "Error in onCreate", e)
            isInitialized = false

            // Show error on screen
            setContentView(TextView(this).apply {
                text = "MainActivity Error:\n\n${e.message}\n\nStack trace:\n${e.stackTraceToString()}"
                textSize = 12f
                setTextColor(android.graphics.Color.WHITE)
                setBackgroundColor(android.graphics.Color.BLACK)
                setPadding(50, 50, 50, 50)
            })
        }
    }

    private fun setupUI() {
        binding?.apply {
            // User profile button
            userProfileButton.setOnClickListener {
                try {
                    startActivity(Intent(this@MainActivity, SettingsActivity::class.java))
                } catch (e: Exception) {
                    Toast.makeText(this@MainActivity, "Settings not available yet", Toast.LENGTH_SHORT).show()
                    Log.e("MainActivity", "Error opening settings", e)
                }
            }

            // Search button
            searchButton.setOnClickListener {
                Toast.makeText(this@MainActivity, "Search coming soon", Toast.LENGTH_SHORT).show()
            }

            // Navigation chips
            chipHome.setOnClickListener {
                loadContent()
            }

            chipMovies.setOnClickListener {
                Toast.makeText(this@MainActivity, "Movies - Coming soon", Toast.LENGTH_SHORT).show()
            }

            chipSeries.setOnClickListener {
                Toast.makeText(this@MainActivity, "Series - Coming soon", Toast.LENGTH_SHORT).show()
            }

            chipMyList.setOnClickListener {
                loadMyList()
            }
        }
    }

    private fun setupFocusHandling() {
        binding?.apply {
            // Set up RecyclerViews with horizontal layout for TV
            continueWatchingRecycler.layoutManager =
                LinearLayoutManager(this@MainActivity, LinearLayoutManager.HORIZONTAL, false)

            myListRecycler.layoutManager =
                LinearLayoutManager(this@MainActivity, LinearLayoutManager.HORIZONTAL, false)

            // Request focus on first chip when activity starts
            chipHome.requestFocus()
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            finish()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun loadContent() {
        if (!isInitialized || binding == null) {
            Log.w("MainActivity", "Cannot load content - not initialized")
            return
        }

        lifecycleScope.launch {
            try {
                binding?.progressBar?.visibility = View.VISIBLE

                // Safely load continue watching - BEFORE binding?.apply
                val continueWatching = try {
                    currentUserId?.let { database.getContinueWatchingItems(it) } ?: emptyList()
                } catch (e: Exception) {
                    Log.e("MainActivity", "Error loading continue watching", e)
                    emptyList()
                }

                // Safely load my list - BEFORE binding?.apply
                val myList = try {
                    currentUserId?.let { database.getLibraryItems(it) } ?: emptyList()
                } catch (e: Exception) {
                    Log.e("MainActivity", "Error loading library", e)
                    emptyList()
                }

                binding?.apply {
                    if (continueWatching.isNotEmpty()) {
                        continueWatchingSection.visibility = View.VISIBLE
                        emptyView.visibility = View.GONE
                    } else {
                        continueWatchingSection.visibility = View.GONE
                    }

                    if (myList.isNotEmpty()) {
                        myListSection.visibility = View.VISIBLE
                        emptyView.visibility = View.GONE
                    } else {
                        myListSection.visibility = View.GONE
                    }

                    // Show welcome message
                    if (continueWatching.isEmpty() && myList.isEmpty()) {
                        emptyView.visibility = View.VISIBLE
                        emptyView.text = """
                            Welcome to Stremio Player!
                            
                            To get started:
                            1. Press Settings button (top right)
                            2. Add Stremio addons
                            3. Browse and watch content
                            
                            Use arrow keys to navigate
                        """.trimIndent()
                    }

                    progressBar.visibility = View.GONE
                }

                Log.d("MainActivity", "Content loaded: ${continueWatching.size} continue watching, ${myList.size} in library")

            } catch (e: Exception) {
                Log.e("MainActivity", "Error in loadContent", e)
                binding?.apply {
                    progressBar.visibility = View.GONE
                    emptyView.visibility = View.VISIBLE
                    emptyView.text = "Error loading content:\n${e.message}"
                }
                Toast.makeText(this@MainActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun loadMyList() {
        if (!isInitialized) return

        lifecycleScope.launch {
            try {
                val myList = currentUserId?.let { database.getLibraryItems(it) } ?: emptyList()

                binding?.apply {
                    if (myList.isEmpty()) {
                        Toast.makeText(
                            this@MainActivity,
                            "Your list is empty. Add content from settings!",
                            Toast.LENGTH_LONG
                        ).show()
                    } else {
                        myListSection.visibility = View.VISIBLE
                        continueWatchingSection.visibility = View.GONE
                        emptyView.visibility = View.GONE
                        Toast.makeText(
                            this@MainActivity,
                            "My List: ${myList.size} items",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Error in loadMyList", e)
                Toast.makeText(this@MainActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (isInitialized) {
            try {
                loadContent()
            } catch (e: Exception) {
                Log.e("MainActivity", "Error in onResume", e)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        binding = null
    }
}