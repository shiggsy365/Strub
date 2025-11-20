package com.example.stremiompvplayer

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.stremiompvplayer.adapters.PosterAdapter
import com.example.stremiompvplayer.adapters.StreamAdapter
import com.example.stremiompvplayer.data.ServiceLocator
import com.example.stremiompvplayer.databinding.ActivityDetails2Binding
import com.example.stremiompvplayer.models.Meta
import com.example.stremiompvplayer.models.MetaItem
import com.example.stremiompvplayer.utils.SharedPreferencesManager
import com.example.stremiompvplayer.viewmodels.MainViewModel
import com.example.stremiompvplayer.viewmodels.MainViewModelFactory

class DetailsActivity2 : AppCompatActivity() {

    private lateinit var binding: ActivityDetails2Binding

    private val viewModel: MainViewModel by viewModels {
        MainViewModelFactory(
            ServiceLocator.getInstance(applicationContext),
            SharedPreferencesManager.getInstance(this)
        )
    }

    private lateinit var seasonsAdapter: PosterAdapter
    private lateinit var streamAdapter: StreamAdapter

    private var currentMetaItem: MetaItem? = null
    private var currentSeriesMeta: Meta? = null
    private var currentType = "movie"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetails2Binding.inflate(layoutInflater)
        setContentView(binding.root)

        handleIntent()
        setupUI()
        setupObservers()
    }

    private fun handleIntent() {
        val id = intent.getStringExtra("metaId") ?: return
        val title = intent.getStringExtra("title") ?: ""
        val poster = intent.getStringExtra("poster")
        val background = intent.getStringExtra("background")
        val description = intent.getStringExtra("description")
        currentType = intent.getStringExtra("type") ?: "movie"

        currentMetaItem = MetaItem(
            id = id,
            type = currentType,
            name = title,
            poster = poster,
            background = background,
            description = description
        )

        updateHeaderUI(title, description, poster, background)

        // Initial data loading
        if (currentType == "movie") {
            viewModel.loadStreams("movie", id)
            binding.rvSeasons.visibility = View.GONE
        } else {
            viewModel.loadSeriesMeta(id)
            binding.rvSeasons.visibility = View.VISIBLE
        }

        // Check Library & Watchlist Status
        viewModel.checkLibraryStatus(id)
        viewModel.checkWatchlistStatus(id, currentType)
    }

    private fun setupUI() {
        binding.btnPlay.setOnClickListener {
            // Logic to start playback or scroll to streams
            binding.rvStreams.requestFocus()
        }

        // Library Toggle
        binding.btnLibrary.setOnClickListener {
            currentMetaItem?.let { item -> viewModel.toggleLibrary(item) }
        }

        // Watchlist Toggle
        binding.btnWatchlist.setOnClickListener {
            currentMetaItem?.let { item -> viewModel.toggleWatchlist(item) }
        }

        seasonsAdapter = PosterAdapter(
            items = emptyList(),
            onClick = { item -> onSeasonEpisodeClicked(item) }
        )
        binding.rvSeasons.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.rvSeasons.adapter = seasonsAdapter

        streamAdapter = StreamAdapter { stream ->
            val intent = Intent(this, PlayerActivity::class.java).apply {
                putExtra("stream", stream)
                putExtra("meta", currentMetaItem)
            }
            startActivity(intent)
        }
        binding.rvStreams.layoutManager = LinearLayoutManager(this)
        binding.rvStreams.adapter = streamAdapter
    }

    private fun setupObservers() {
        viewModel.streams.observe(this) { streams ->
            streamAdapter.submitList(streams)
        }

        viewModel.metaDetails.observe(this) { meta ->
            if (meta != null && currentType == "series") {
                currentSeriesMeta = meta
                displaySeasons(meta)
                // Update background if better quality available
                if (!meta.background.isNullOrEmpty()) {
                    Glide.with(this).load(meta.background).into(binding.imgBackground)
                }
            }
        }

        viewModel.isLoading.observe(this) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        // LIBRARY STATUS OBSERVER
        viewModel.isItemInLibrary.observe(this) { inLibrary ->
            if (inLibrary) {
                binding.btnLibrary.text = "Remove From Library"
                binding.btnLibrary.background.setTint(Color.parseColor("#006400")) // Dark Green
            } else {
                binding.btnLibrary.text = "Add To Library"
                binding.btnLibrary.background.setTint(Color.parseColor("#555555"))
            }
        }

        // WATCHLIST STATUS OBSERVER
        viewModel.isItemInWatchlist.observe(this) { inWatchlist ->
            if (inWatchlist) {
                binding.btnWatchlist.text = "Remove From Watchlist"
                binding.btnWatchlist.background.setTint(Color.parseColor("#006400"))
            } else {
                binding.btnWatchlist.text = "Add To Watchlist"
                binding.btnWatchlist.background.setTint(Color.parseColor("#555555"))
            }
        }
    }

    private fun updateHeaderUI(title: String, description: String?, poster: String?, background: String?) {
        binding.tvTitle.text = title
        binding.tvDescription.text = description ?: ""

        if (!poster.isNullOrEmpty()) {
            Glide.with(this).load(poster).into(binding.imgPoster)
        }
        if (!background.isNullOrEmpty()) {
            Glide.with(this).load(background).into(binding.imgBackground)
        } else if (!poster.isNullOrEmpty()) {
            Glide.with(this).load(poster).into(binding.imgBackground)
        }
    }

    private fun displaySeasons(meta: Meta) {
        // FIX: Use mapNotNull ensures we have List<Int> (Comparable) instead of List<Int?>
        val seasons = meta.videos?.mapNotNull { it.season }?.distinct()?.sorted() ?: emptyList()

        val items = seasons.map { season ->
            MetaItem(
                id = season.toString(),
                name = "Season $season",
                poster = meta.poster,
                type = "season",
                description = null,
                background = null
            )
        }
        seasonsAdapter.updateData(items)
    }

    private fun onSeasonEpisodeClicked(item: MetaItem) {
        // Logic to load episodes when a season is clicked
        // Or load streams if an episode is clicked
        // This placeholder logic assumes we want to show episodes for the selected season
        val seasonNum = item.id.toIntOrNull() ?: return

        // Filter episodes for this season
        val episodes = currentSeriesMeta?.videos?.filter { it.season == seasonNum }?.sortedBy { it.number } ?: emptyList()

        val episodeItems = episodes.map { vid ->
            MetaItem(
                id = vid.id, // "tmdb:id:season:episode"
                name = "Ep ${vid.number}: ${vid.title ?: "Episode ${vid.number}"}",
                poster = vid.thumbnail ?: currentSeriesMeta?.poster,
                type = "episode",
                description = null,
                background = null
            )
        }

        // Update the adapter to show episodes instead of seasons
        // In a real app you might want a separate list or breadcrumb navigation
        seasonsAdapter.updateData(episodeItems)

        // If it's an episode item (type="episode"), load streams
        if (item.type == "episode") {
            val parts = item.id.split(":")
            if (parts.size >= 4) {
                // parts: [tmdb, id, season, episode]
                val s = parts[2].toIntOrNull() ?: 0
                val e = parts[3].toIntOrNull() ?: 0
                viewModel.loadEpisodeStreams(item.id, s, e)
            }
        }
    }
}