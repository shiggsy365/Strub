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
import com.google.android.material.chip.Chip

class DetailsActivity2 : AppCompatActivity() {

    private lateinit var binding: ActivityDetails2Binding
    private val viewModel: MainViewModel by viewModels {
        MainViewModelFactory(
            ServiceLocator.getInstance(applicationContext),
            SharedPreferencesManager.getInstance(this)
        )
    }

    // Adapters
    private lateinit var navigationAdapter: PosterAdapter // For Seasons / Episodes
    private lateinit var streamAdapter: StreamAdapter     // For Streams

    // State
    private var currentMetaItem: MetaItem? = null
    private var currentSeriesMeta: Meta? = null
    private var currentType = "movie"
    private var currentSeason: Int? = null

    private val UP_NAVIGATION_ITEM = MetaItem("UP_NAV", "up", "[...]", null, null, "Go Back")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetails2Binding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.rvNavigation.layoutManager = LinearLayoutManager(this)

        // Initialize Adapters
        streamAdapter = StreamAdapter { stream ->
            val intent = Intent(this, PlayerActivity::class.java).apply {
                putExtra("stream", stream)
                putExtra("meta", currentMetaItem)
            }
            startActivity(intent)
        }

        navigationAdapter = PosterAdapter(
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
        val bg = intent.getStringExtra("background")
        val desc = intent.getStringExtra("description")
        currentType = intent.getStringExtra("type") ?: "movie"

        currentMetaItem = MetaItem(id, currentType, title, poster, bg, desc)
        updateHeaderUI(title, desc, poster, bg)

        // 1. Fetch Credits (Director & Cast)
        viewModel.fetchCast(id, currentType)

        // 2. Load Content based on type
        if (currentType == "movie") {
            viewModel.loadStreams("movie", id)
            binding.rvNavigation.adapter = streamAdapter
        } else {
            // Series: Load metadata which contains seasons
            viewModel.loadSeriesMeta(id)
            binding.rvNavigation.adapter = navigationAdapter
        }

        // 3. Check Status
        viewModel.checkLibraryStatus(id)
        viewModel.checkWatchlistStatus(id, currentType)
    }

    private fun setupObservers() {
        // Director Chip
        viewModel.director.observe(this) { name ->
            binding.directorChipGroup.removeAllViews()
            if (!name.isNullOrEmpty()) {
                val chip = Chip(this).apply {
                    text = name
                    setChipBackgroundColorResource(R.color.chip_background_selector)
                    setTextColor(getColor(R.color.chip_text_color))
                    isClickable = true
                    isFocusable = true
                    setOnClickListener { performSearch(name) }
                }
                binding.directorChipGroup.addView(chip)
            }
        }

        // Cast Chips
        viewModel.castList.observe(this) { cast ->
            binding.castChipGroup.removeAllViews()
            cast.forEach { c ->
                val chip = Chip(this).apply {
                    text = c.name
                    setChipBackgroundColorResource(R.color.chip_background_selector)
                    setTextColor(getColor(R.color.chip_text_color))
                    isClickable = true
                    isFocusable = true
                    setOnClickListener { performSearch(c.name) }
                }
                binding.castChipGroup.addView(chip)
            }
        }

        // Streams
        viewModel.streams.observe(this) { streams ->
            if (binding.rvNavigation.adapter == streamAdapter) {
                streamAdapter.submitList(streams)
            }
        }

        // Loading
        viewModel.isLoading.observe(this) { loading ->
            binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        }

        // --- Series Specific ---

        // 1. Series Meta Loaded -> Show Seasons
        viewModel.metaDetails.observe(this) { meta ->
            if (meta != null && currentType == "series" && currentSeason == null) {
                currentSeriesMeta = meta

                // Update background if better one found
                if (!meta.background.isNullOrEmpty()) {
                    Glide.with(this).load(meta.background).into(binding.imgBackground)
                }

                // Map seasons to MetaItems
                val seasons = meta.videos?.map { vid ->
                    MetaItem(
                        id = vid.season.toString(),
                        type = "season",
                        name = "Season ${vid.season}",
                        poster = vid.thumbnail ?: meta.poster,
                        background = null,
                        description = null
                    )
                } ?: emptyList()

                navigationAdapter.updateData(seasons)
            }
        }

        // 2. Season Details Loaded -> Show Episodes
        viewModel.seasonEpisodes.observe(this) { episodes ->
            val listWithUp = mutableListOf(UP_NAVIGATION_ITEM)
            listWithUp.addAll(episodes)

            binding.rvNavigation.adapter = navigationAdapter
            navigationAdapter.updateData(listWithUp)
        }

        // Library & Watchlist Status (Buttons)
        viewModel.isItemInLibrary.observe(this) { inLib ->
            binding.btnLibrary.text = if (inLib) "Remove From Library" else "Add To Library"
            val color = if (inLib) Color.parseColor("#006400") else Color.parseColor("#555555")
            binding.btnLibrary.background.setTint(color)
        }

        viewModel.isItemInWatchlist.observe(this) { inWatch ->
            binding.btnWatchlist.text = if (inWatch) "Remove From Watchlist" else "Add To Watchlist"
            val color = if (inWatch) Color.parseColor("#006400") else Color.parseColor("#555555")
            binding.btnWatchlist.background.setTint(color)
        }
    }

    private fun onNavigationItemClicked(item: MetaItem) {
        // 1. Handle UP Navigation
        if (item.type == "up") {
            navigateUp()
            return
        }

        // 2. Handle Season Click -> Load Episodes
        if (item.type == "season") {
            val seasonNum = item.id.toIntOrNull() ?: return
            currentSeason = seasonNum
            viewModel.loadSeasonEpisodes(currentMetaItem!!.id, seasonNum)
        }
        // 3. Handle Episode Click -> Load Streams
        else if (item.type == "episode") {
            // ID format from ViewModel: tmdb:id:season:episode
            val parts = item.id.split(":")
            if (parts.size >= 4) {
                val season = parts[2].toInt()
                val episode = parts[3].toInt()

                // Load Streams for this episode
                viewModel.loadEpisodeStreams(currentMetaItem!!.id, season, episode)
                binding.rvNavigation.adapter = streamAdapter

                // Update Header to show specific episode info
                binding.tvTitle.text = "${currentSeriesMeta?.name} - S${season} E${episode}"
                binding.tvDescription.text = item.description ?: item.name
            }
        }
    }

    private fun navigateUp() {
        if (binding.rvNavigation.adapter == streamAdapter) {
            // Back from Streams -> Episode List
            binding.rvNavigation.adapter = navigationAdapter
            if (currentSeason != null) {
                // Re-display episode list header
                updateHeaderUI(currentSeriesMeta?.name ?: "", currentSeriesMeta?.description, currentSeriesMeta?.poster, currentSeriesMeta?.background)
                // We don't need to reload if we cache, but calling load again is safe
                viewModel.loadSeasonEpisodes(currentMetaItem!!.id, currentSeason!!)
            }
        } else if (currentSeason != null) {
            // Back from Episodes -> Season List
            currentSeason = null
            updateHeaderUI(currentSeriesMeta?.name ?: "", currentSeriesMeta?.description, currentSeriesMeta?.poster, currentSeriesMeta?.background)
            // Trigger reload of main series meta to refresh season list
            viewModel.loadSeriesMeta(currentMetaItem!!.id)
        } else {
            // Back from Season List -> Exit Activity
            finish()
        }
    }

    override fun onBackPressed() {
        if (currentType == "series") {
            if (binding.rvNavigation.adapter == streamAdapter || currentSeason != null) {
                navigateUp()
                return
            }
        }
        super.onBackPressed()
    }

    private fun updateHeaderUI(title: String, desc: String?, poster: String?, bg: String?) {
        binding.tvTitle.text = title
        binding.tvDescription.text = desc ?: ""
        if (!poster.isNullOrEmpty()) Glide.with(this).load(poster).into(binding.imgPoster)
        if (!bg.isNullOrEmpty()) Glide.with(this).load(bg).into(binding.imgBackground)
    }

    private fun performSearch(query: String) {
        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra("SEARCH_QUERY", query)
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        startActivity(intent)
    }

    private fun setupUI() {
        binding.btnLibrary.setOnClickListener { currentMetaItem?.let { viewModel.toggleLibrary(it) } }
        binding.btnWatchlist.setOnClickListener { currentMetaItem?.let { viewModel.toggleWatchlist(it) } }

        binding.btnPlay.setOnClickListener {
            if (binding.rvNavigation.adapter == streamAdapter) {
                // Play first stream
                streamAdapter.currentList.firstOrNull()?.let { stream ->
                    val intent = Intent(this, PlayerActivity::class.java).apply {
                        putExtra("stream", stream)
                        putExtra("meta", currentMetaItem)
                    }
                    startActivity(intent)
                }
            } else {
                // Focus navigation to pick season/episode
                binding.rvNavigation.requestFocus()
            }
        }
    }
}