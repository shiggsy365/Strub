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
    private var similarItems: List<MetaItem> = emptyList()

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

        binding.rvContent.layoutManager = GridLayoutManager(this, 10)
        binding.rvContent.adapter = posterAdapter

        // Load similar content
        loadSimilarContent(metaId, type)

        // Set up back button
        binding.btnBack.setOnClickListener {
            finish()
        }
    }

    private fun loadSimilarContent(metaId: String, type: String) {
        lifecycleScope.launch {
            val items = viewModel.fetchSimilarContent(metaId, type)
            similarItems = items
            posterAdapter.updateData(items)

            // Update detail pane with first item if available
            if (items.isNotEmpty()) {
                updateDetailPane(items[0])
            }
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

    override fun onResume() {
        super.onResume()
        // Observe logo changes
        viewModel.currentLogo.observe(this) { logoUrl ->
            if (!logoUrl.isNullOrEmpty()) {
                binding.imgLogo.visibility = android.view.View.VISIBLE
                binding.detailTitle.visibility = android.view.View.GONE
                Glide.with(this)
                    .load(logoUrl)
                    .into(binding.imgLogo)
            } else {
                binding.imgLogo.visibility = android.view.View.GONE
                binding.detailTitle.visibility = android.view.View.VISIBLE
            }
        }
    }
}
