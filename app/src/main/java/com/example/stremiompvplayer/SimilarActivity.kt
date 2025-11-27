package com.example.stremiompvplayer

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.bumptech.glide.Glide
import com.example.stremiompvplayer.adapters.PosterAdapter
import com.example.stremiompvplayer.data.ServiceLocator
import com.example.stremiompvplayer.databinding.ActivitySimilarBinding
import com.example.stremiompvplayer.models.MetaItem
import com.example.stremiompvplayer.utils.SharedPreferencesManager
import com.example.stremiompvplayer.viewmodels.MainViewModel
import com.example.stremiompvplayer.viewmodels.MainViewModelFactory
import kotlinx.coroutines.launch

class SimilarActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySimilarBinding
    private val viewModel: MainViewModel by viewModels {
        MainViewModelFactory(
            ServiceLocator.getInstance(applicationContext),
            SharedPreferencesManager.getInstance(this)
        )
    }

    private lateinit var posterAdapter: PosterAdapter
    private var movieResults = listOf<MetaItem>()
    private var seriesResults = listOf<MetaItem>()
    private var currentResultIndex = 0  // 0 for movies, 1 for series

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySimilarBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val metaId = intent.getStringExtra("metaId") ?: return
        val type = intent.getStringExtra("type") ?: "movie"
        val title = intent.getStringExtra("title") ?: ""
        val poster = intent.getStringExtra("poster")
        val background = intent.getStringExtra("background")

        // Set up title
        binding.tvTitle.text = "Similar to $title"

        // Set up background
        background?.let {
            Glide.with(this)
                .load(it)
                .into(binding.imgBackground)
        }

        // Set up poster adapter
        posterAdapter = PosterAdapter(
            items = emptyList(),
            onClick = { item -> onItemClicked(item) }
        )

        binding.rvContent.layoutManager = com.example.stremiompvplayer.utils.AutoFitGridLayoutManager(this, 140)
        binding.rvContent.adapter = posterAdapter

        // Load similar content
        loadSimilarContent(metaId, type)

        // Set up back button
        binding.btnBack.setOnClickListener {
            finish()
        }

        // Set up key handling for cycling
        setupKeyHandling()
    }

    private fun setupKeyHandling() {
        binding.rvContent.setOnKeyListener { _, keyCode, event ->
            if (event.action == android.view.KeyEvent.ACTION_DOWN) {
                when (keyCode) {
                    android.view.KeyEvent.KEYCODE_DPAD_DOWN -> {
                        val focusedChild = binding.rvContent.focusedChild
                        if (focusedChild != null && (movieResults.isNotEmpty() || seriesResults.isNotEmpty())) {
                            cycleToNextResultType()
                            return@setOnKeyListener true
                        }
                        false
                    }
                    else -> false
                }
            } else {
                false
            }
        }
    }

    private fun cycleToNextResultType() {
        val hasMovies = movieResults.isNotEmpty()
        val hasSeries = seriesResults.isNotEmpty()

        if (!hasMovies && !hasSeries) return

        // Cycle between movies (0) and series (1)
        if (hasMovies && hasSeries) {
            currentResultIndex = (currentResultIndex + 1) % 2
        } else if (hasMovies) {
            currentResultIndex = 0
        } else {
            currentResultIndex = 1
        }

        updateDisplayedResults()
    }

    private fun updateDisplayedResults() {
        val results = if (currentResultIndex == 0 && movieResults.isNotEmpty()) {
            movieResults
        } else if (currentResultIndex == 1 && seriesResults.isNotEmpty()) {
            seriesResults
        } else if (movieResults.isNotEmpty()) {
            movieResults
        } else {
            seriesResults
        }

        posterAdapter.updateData(results)
        if (results.isNotEmpty()) {
            updateDetailPane(results[0])
        }

        // Update title (combined results, no type label needed)
        val originalTitle = intent.getStringExtra("title") ?: ""
        binding.tvTitle.text = "Similar to $originalTitle"

        // [FIX] Force focus to the first item when changing result type
        binding.rvContent.postDelayed({
            binding.rvContent.scrollToPosition(0)
            binding.rvContent.post {
                val firstView = binding.rvContent.layoutManager?.findViewByPosition(0)
                firstView?.requestFocus()
            }
        }, 200)
    }

    private fun loadSimilarContent(metaId: String, type: String) {
        lifecycleScope.launch {
            // Fetch similar content for both movies and series
            val allItems = viewModel.fetchSimilarContent(metaId, type)

            // Combine and sort by popularity (rating)
            val combinedResults = allItems.sortedByDescending {
                it.rating?.toDoubleOrNull() ?: 0.0
            }

            movieResults = combinedResults
            seriesResults = emptyList()
            currentResultIndex = 0
            updateDisplayedResults()
        }
    }

    private fun updateDetailPane(item: MetaItem) {
        binding.detailTitle.text = item.name
        binding.detailDescription.text = item.description ?: "No description available"

        item.releaseDate?.let {
            binding.detailDate.text = it
        }

        item.rating?.let {
            binding.detailRating.text = "★ $it"
        }

        item.background?.let {
            Glide.with(this)
                .load(it)
                .into(binding.imgBackground)
        }

        // Load logo
        viewModel.fetchItemLogo(item)
    }

    private fun onItemClicked(item: MetaItem) {
        val intent = Intent(this, DetailsActivity2::class.java).apply {
            putExtra("metaId", item.id)
            putExtra("title", item.name)
            putExtra("poster", item.poster)
            putExtra("background", item.background)
            putExtra("description", item.description)
            putExtra("type", item.type)
        }
        startActivity(intent)
    }
}