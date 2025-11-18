package com.example.stremiompvplayer

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.stremiompvplayer.databinding.ActivityDetails2Binding
import com.example.stremiompvplayer.models.MetaItem
import com.example.stremiompvplayer.utils.SharedPreferencesManager
// Ensure you have an adapter for streams (assuming StreamAdapter exists based on your file list)
import com.example.stremiompvplayer.adapters.StreamAdapter

class DetailsActivity2 : AppCompatActivity() {

    private lateinit var binding: ActivityDetails2Binding
    private lateinit var prefsManager: SharedPreferencesManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetails2Binding.inflate(layoutInflater)
        setContentView(binding.root)

        prefsManager = SharedPreferencesManager.getInstance(this)

        setupToolbar()

        // Get data passed from previous activity
        val metaId = intent.getStringExtra("metaId")
        val title = intent.getStringExtra("title")
        val poster = intent.getStringExtra("poster")
        val background = intent.getStringExtra("background")
        val description = intent.getStringExtra("description")

        if (metaId == null) {
            Toast.makeText(this, "Error loading details", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupUI(title, poster, background, description)
        loadDetails(metaId)
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupUI(title: String?, poster: String?, background: String?, description: String?) {
        // Match IDs with the XML we fixed
        binding.title.text = title ?: "Unknown Title"
        binding.description.text = description ?: "No description available."

        // Load images using Glide
        if (!poster.isNullOrEmpty()) {
            Glide.with(this)
                .load(poster)
                .into(binding.poster)
        }

        if (!background.isNullOrEmpty()) {
            Glide.with(this)
                .load(background)
                .into(binding.backgroundImage)
        }

        // Initialize RecyclerView
        binding.streamRecyclerView.layoutManager = LinearLayoutManager(this)
    }

    private fun loadDetails(metaId: String) {
        binding.progressBar.visibility = View.VISIBLE
        binding.contentGroup.visibility = View.GONE

        // Simulate loading or call your ViewModel/Repository here
        // For now, we just show the content to verify the build works
        binding.progressBar.visibility = View.GONE
        binding.contentGroup.visibility = View.VISIBLE

        // Example: Setting no streams text if list is empty
        binding.textNoStreams.visibility = View.VISIBLE
        binding.streamRecyclerView.visibility = View.GONE
    }
}