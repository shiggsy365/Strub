package com.example.stremiompvplayer

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
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
import com.example.stremiompvplayer.models.Video
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

    // Renamed for clarity in its new role: Handles Season/Episode navigation
    private lateinit var seasonEpisodeAdapter: PosterAdapter
    private lateinit var streamAdapter: StreamAdapter

    // State Variables for Series Drill-down
    private var currentMetaItem: MetaItem? = null
    private var currentSeriesMeta: Meta? = null
    private var currentType = "movie"
    private var currentSeason: Int? = null // Null for Movie/Series Top Level, Int for Season Drill-down
    private var currentEpisode: Video? = null // Null if not drilled to episode streams

    // Special MetaItem for "Up" navigation
    private val UP_NAVIGATION_ITEM = MetaItem(
        id = "UP_NAV",
        type = "up",
        name = "[...]",
        poster = null,
        background = null,
        description = "Go back to the previous view"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetails2Binding.inflate(layoutInflater)
        setContentView(binding.root)

        // The single RecyclerView is rvNavigation
        binding.rvNavigation.layoutManager = LinearLayoutManager(this)

        // Initialize adapters early
        streamAdapter = StreamAdapter { stream ->
            // Stream Clicked: Start playback
            val intent = Intent(this, PlayerActivity::class.java).apply {
                putExtra("stream", stream)
                // Pass the original meta or the current episode's derived meta
                putExtra("meta", currentMetaItem)
            }
            startActivity(intent)
        }

        // Renamed/repurposed seasonsAdapter for series navigation
        seasonEpisodeAdapter = PosterAdapter(
            items = emptyList(),
            onClick = { item -> onNavigationItemClicked(item) }
        )

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
            // MOVIE: Load streams and show in the right pane
            viewModel.loadStreams("movie", id)
            binding.rvNavigation.adapter = streamAdapter
            setButtonsEnabled(true)
        } else {
            // SERIES: Load meta and show seasons in the right pane
            viewModel.loadSeriesMeta(id)
            binding.rvNavigation.adapter = seasonEpisodeAdapter
            setButtonsEnabled(true) // Top-level series buttons are enabled
        }

        // Check Library & Watchlist Status
        viewModel.checkLibraryStatus(id)
        viewModel.checkWatchlistStatus(id, currentType)
    }

    private fun setupUI() {
        // Handle Play button click - Plays the first stream in the list
        binding.btnPlay.setOnClickListener {
            if (binding.btnPlay.isEnabled) {
                // If the current view is streams (movie or episode level)
                if (binding.rvNavigation.adapter == streamAdapter) {
                    val firstStream = streamAdapter.currentList.firstOrNull()
                    firstStream?.let { stream ->
                        val intent = Intent(this, PlayerActivity::class.java).apply {
                            putExtra("stream", stream)
                            putExtra("meta", currentMetaItem)
                        }
                        startActivity(intent)
                    }
                } else if (currentType == "series") {
                    // If at Series Top/Season level, focus the navigation pane for selection
                    binding.rvNavigation.requestFocus()
                }
            }
        }

        // Library Toggle
        binding.btnLibrary.setOnClickListener {
            if (binding.btnLibrary.isEnabled) {
                currentMetaItem?.let { item -> viewModel.toggleLibrary(item) }
            }
        }

        // Watchlist Toggle
        binding.btnWatchlist.setOnClickListener {
            if (binding.btnWatchlist.isEnabled) {
                currentMetaItem?.let { item -> viewModel.toggleWatchlist(item) }
            }
        }

        // Trailer Button Click (TMDB button)
        binding.btnTrailer.setOnClickListener {
            // Existing trailer logic
        }
    }

    private fun setButtonsEnabled(enabled: Boolean) {
        binding.btnPlay.isEnabled = enabled
        binding.btnTrailer.isEnabled = enabled
        binding.btnLibrary.isEnabled = enabled
        binding.btnWatchlist.isEnabled = enabled

        // Update visual state
        binding.btnPlay.alpha = if (enabled) 1.0f else 0.5f
        binding.btnTrailer.alpha = if (enabled) 1.0f else 0.5f
        binding.btnLibrary.alpha = if (enabled) 1.0f else 0.5f
        binding.btnWatchlist.alpha = if (enabled) 1.0f else 0.5f
    }

    private fun setupObservers() {
        viewModel.streams.observe(this) { streams ->
            // Only update streams if the streamAdapter is currently being used
            if (binding.rvNavigation.adapter == streamAdapter) {
                // The streams already contain regex-filtered titles via Stream.formattedTitle
                streamAdapter.submitList(streams)
            }
        }

        viewModel.metaDetails.observe(this) { meta ->
            // Only load if at the Series top level (currentSeason is null)
            if (meta != null && currentType == "series" && currentSeason == null) {
                currentSeriesMeta = meta
                displaySeasons(meta)

                // Update background if better quality available
                if (!meta.background.isNullOrEmpty()) {
                    Glide.with(this).load(meta.background).into(binding.imgBackground)
                }
            }
        }

        // ... (Other observers remain unchanged) ...
        viewModel.isLoading.observe(this) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        viewModel.isItemInLibrary.observe(this) { inLibrary ->
            if (inLibrary) {
                binding.btnLibrary.text = "Remove From Library"
                binding.btnLibrary.background.setTint(Color.parseColor("#006400"))
            } else {
                binding.btnLibrary.text = "Add To Library"
                binding.btnLibrary.background.setTint(Color.parseColor("#555555"))
            }
        }

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

    // --- SERIES NAVIGATION LOGIC ---

    // Top Level (Series Detail) -> Show Seasons
    private fun displaySeasons(meta: Meta) {
        currentSeason = null
        currentEpisode = null
        setButtonsEnabled(true)
        updateHeaderUI(meta.name, meta.description, meta.poster, meta.background)

        // Filter seasons from videos
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

        // Ensure the correct adapter is active
        if (binding.rvNavigation.adapter != seasonEpisodeAdapter) {
            binding.rvNavigation.adapter = seasonEpisodeAdapter
        }

        seasonEpisodeAdapter.updateData(items)
    }

    // Season Level -> Show Episodes
    private fun displayEpisodes(seasonNum: Int) {
        currentSeason = seasonNum
        currentEpisode = null
        setButtonsEnabled(false) // Disable buttons at season/episode list level

        val meta = currentSeriesMeta ?: return

        // Filter episodes for this season
        val episodes = meta.videos?.filter { it.season == seasonNum }?.sortedBy { it.number } ?: emptyList()

        val episodeItems = episodes.map { vid ->
            MetaItem(
                id = vid.id, // e.g., "tmdb:id:season:episode"
                name = "Ep ${vid.number}: ${vid.title ?: "Episode ${vid.number}"}",
                poster = vid.thumbnail ?: currentSeriesMeta?.poster,
                type = "episode",
                description = vid.title, // Store episode title/description for later header update
                background = vid.thumbnail
            )
        }.toMutableList()

        // Add UP NAVIGATION as the first item
        episodeItems.add(0, UP_NAVIGATION_ITEM)

        seasonEpisodeAdapter.updateData(episodeItems)
    }

    // Episode Level -> Show Streams
    private fun displayStreams(episodeId: String, episodeTitle: String, episodeDescription: String?, season: Int, episode: Int) {
        val meta = currentSeriesMeta ?: return

        currentSeason = season
        currentEpisode = meta.videos?.find { it.id == episodeId }
        setButtonsEnabled(true) // Re-enable buttons at stream level

        // Update header description
        val headerTitle = "${meta.name} - S$season E$episode"
        val headerDescription = episodeDescription ?: "No description available."
        binding.tvTitle.text = headerTitle
        binding.tvDescription.text = headerDescription

        // Load the streams
        viewModel.loadEpisodeStreams(currentMetaItem!!.id, season, episode)

        // Switch to stream adapter
        if (binding.rvNavigation.adapter != streamAdapter) {
            binding.rvNavigation.adapter = streamAdapter
        }
    }


    private fun onNavigationItemClicked(item: MetaItem) {
        // Handle Up Navigation
        if (item.id == UP_NAVIGATION_ITEM.id) {
            navigateUp()
            return
        }

        // Handle Season Click
        if (item.type == "season") {
            val seasonNum = item.id.toIntOrNull() ?: return
            displayEpisodes(seasonNum)
        }

        // Handle Episode Click
        else if (item.type == "episode") {
            val episodeParts = item.id.split(":") // Expected format: tmdb:id:season:episode
            if (episodeParts.size >= 4) {
                val season = episodeParts[2].toIntOrNull() ?: 0
                val episode = episodeParts[3].toIntOrNull() ?: 0

                displayStreams(
                    episodeId = item.id,
                    episodeTitle = item.name,
                    episodeDescription = item.description,
                    season = season,
                    episode = episode
                )
            }
        }
    }

    // Custom Back Navigation
    override fun onBackPressed() {
        if (currentType == "series") {
            if (binding.rvNavigation.adapter == streamAdapter) {
                // Back from streams (Episode Level) to episode list (Season Level)
                val season = currentSeason ?: return
                displayEpisodes(season)
                // Restore primary meta info
                updateHeaderUI(currentSeriesMeta?.name ?: currentMetaItem!!.name, currentSeriesMeta?.description, currentSeriesMeta?.poster, currentSeriesMeta?.background)
                // Clear streams
                viewModel.clearStreams()
                return
            } else if (currentSeason != null) {
                // Back from episode list (Season Level) to season list (Series Top Level)
                val meta = currentSeriesMeta ?: return
                displaySeasons(meta)
                return
            }
        }

        // Default behavior (Movie or Series Top Level)
        super.onBackPressed()
    }

    private fun navigateUp() {
        onBackPressed()
    }
}