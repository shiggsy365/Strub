package com.example.stremiompvplayer

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.stremiompvplayer.adapters.StreamAdapter
import com.example.stremiompvplayer.adapters.TextItem
import com.example.stremiompvplayer.adapters.TextListAdapter
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
    private lateinit var textListAdapter: TextListAdapter // For Seasons / Episodes
    private lateinit var streamAdapter: StreamAdapter     // For Streams

    // State
    private var currentMetaItem: MetaItem? = null
    private var currentSeriesMeta: Meta? = null // Stores series info
    private var currentType = "movie"
    private var currentSeason: Int? = null

    // Special TextItem for "Up" navigation
    private val UP_NAV_TEXT_ITEM = TextItem("UP_NAV", "[..^..]", "up")

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

        textListAdapter = TextListAdapter(
            items = emptyList(),
            onClick = { item -> onTextItemClicked(item) },
            onFocus = { /* Optional: Scroll logic if needed */ }
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
            updateButtonVisibility(isEpisodeLevel = true) // Movies act like "Episode Level" (ready to play)
            viewModel.loadStreams("movie", id)
            binding.rvNavigation.adapter = streamAdapter
        } else {
            updateButtonVisibility(isEpisodeLevel = false) // Top level series
            // Load Series Metadata (Seasons)
            viewModel.loadSeriesMeta(id)
            binding.rvNavigation.adapter = textListAdapter
        }

        // 3. Check Status
        viewModel.checkLibraryStatus(id)
        viewModel.checkWatchlistStatus(id, currentType)
    }

    private fun updateButtonVisibility(isEpisodeLevel: Boolean) {
        if (currentType == "movie") {
            // Movies always show everything
            binding.btnPlay.visibility = View.VISIBLE
            binding.btnLibrary.visibility = View.VISIBLE
            binding.btnWatchlist.visibility = View.VISIBLE
        } else {
            // Series Logic
            if (isEpisodeLevel) {
                // Deep level: Show Play, Hide Library/Watchlist
                binding.btnPlay.visibility = View.VISIBLE
                binding.btnLibrary.visibility = View.GONE
                binding.btnWatchlist.visibility = View.GONE
            } else {
                // Top level: Hide Play, Show Library/Watchlist
                binding.btnPlay.visibility = View.GONE
                binding.btnLibrary.visibility = View.VISIBLE
                binding.btnWatchlist.visibility = View.VISIBLE
            }
        }
    }

    private fun setupObservers() {
        // Director Chip
        viewModel.director.observe(this) { name ->
            binding.directorChipGroup.removeAllViews()
            if (!name.isNullOrEmpty()) {
                val chip = createStyledChip(name)
                binding.directorChipGroup.addView(chip)
            }
        }

        // Cast Chips
        viewModel.castList.observe(this) { cast ->
            binding.castChipGroup.removeAllViews()
            cast.forEach { c ->
                val chip = createStyledChip(c.name)
                binding.castChipGroup.addView(chip)
            }
        }

        // Streams
        viewModel.streams.observe(this) { streams ->
            if (binding.rvNavigation.adapter == streamAdapter) {
                // Ensure [..^..] is NOT in StreamAdapter (StreamAdapter is usually purely streams)
                // If you want [..^..] in streams, StreamAdapter needs modification.
                // For now, standard StreamAdapter logic:
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

                if (!meta.background.isNullOrEmpty()) {
                    Glide.with(this).load(meta.background).into(binding.imgBackground)
                }

                // Map seasons to TextItems
                val seasons = meta.videos?.map { vid ->
                    TextItem(
                        id = vid.season.toString(),
                        text = "Season ${vid.season}",
                        type = "season"
                    )
                } ?: emptyList()

                // At Top Level: No Back Button in list
                textListAdapter.submitList(seasons)
            }
        }

        // 2. Season Details Loaded -> Show Episodes
        viewModel.seasonEpisodes.observe(this) { episodes ->
            // Map episodes to TextItems
            val episodeItems = episodes.map { ep ->
                TextItem(
                    id = ep.id, // tmdb:id:season:episode
                    text = ep.name,
                    type = "episode",
                    data = ep.description // Stash description here
                )
            }.toMutableList()

            // Add UP NAVIGATION
            episodeItems.add(0, UP_NAV_TEXT_ITEM)

            binding.rvNavigation.adapter = textListAdapter
            textListAdapter.submitList(episodeItems)
        }

            // Library & Watchlist Status
            viewModel.isItemInLibrary.observe(this) { inLib ->
                // CHANGED: Text to "Library -" / "Library +"
                binding.btnLibrary.text = if (inLib) "Library -" else "Library +"
                val color = if (inLib) Color.parseColor("#006400") else Color.parseColor("#555555")
                binding.btnLibrary.background.setTint(color)
            }

            viewModel.isItemInWatchlist.observe(this) { inWatch ->
                // CHANGED: Text to "TMDB Watchlist -" / "TMDB Watchlist +"
                binding.btnWatchlist.text = if (inWatch) "TMDB Watchlist -" else "TMDB Watchlist +"
                val color = if (inWatch) Color.parseColor("#006400") else Color.parseColor("#555555")
                binding.btnWatchlist.background.setTint(color)
            }
        }

    private fun onTextItemClicked(item: TextItem) {
        // 1. Handle UP Navigation
        if (item.type == "up") {
            navigateUp()
            return
        }

        // 2. Handle Season Click -> Load Episodes
        if (item.type == "season") {
            val seasonNum = item.id.toIntOrNull() ?: return
            currentSeason = seasonNum
            updateButtonVisibility(isEpisodeLevel = true) // Now in episode list
            viewModel.loadSeasonEpisodes(currentMetaItem!!.id, seasonNum)
        }
        // 3. Handle Episode Click -> Load Streams
        else if (item.type == "episode") {
            val parts = item.id.split(":")
            if (parts.size >= 4) {
                val season = parts[2].toInt()
                val episode = parts[3].toInt()

                updateButtonVisibility(isEpisodeLevel = true) // Still deep level

                viewModel.loadEpisodeStreams(currentMetaItem!!.id, season, episode)
                binding.rvNavigation.adapter = streamAdapter

                binding.tvTitle.text = "${currentSeriesMeta?.name} - S${season} E${episode}"
                binding.tvDescription.text = item.data as? String ?: item.text
            }
        }
    }

    private fun navigateUp() {
        if (binding.rvNavigation.adapter == streamAdapter) {
            // Back from Streams -> Episode List
            binding.rvNavigation.adapter = textListAdapter
            if (currentSeason != null) {
                updateHeaderUI(currentSeriesMeta?.name ?: "", currentSeriesMeta?.description, currentSeriesMeta?.poster, currentSeriesMeta?.background)
                // Refresh episode list visibility (technically already loaded in adapter, but safe to ensure)
                updateButtonVisibility(isEpisodeLevel = true)
            }
        } else if (currentSeason != null) {
            // Back from Episodes -> Season List (Top Level)
            currentSeason = null
            updateButtonVisibility(isEpisodeLevel = false)
            updateHeaderUI(currentSeriesMeta?.name ?: "", currentSeriesMeta?.description, currentSeriesMeta?.poster, currentSeriesMeta?.background)

            // Reload/Refresh Season list
            viewModel.loadSeriesMeta(currentMetaItem!!.id)
        } else {
            // Back from Top Level -> Exit
            finish()
        }
    }

    private fun createStyledChip(text: String): Chip {
        return Chip(this).apply {
            this.text = text
            setChipBackgroundColorResource(R.color.chip_background_selector)
            setTextColor(getColor(R.color.chip_text_color))

            // Apply Red Border Stroke using the new Selector
            setChipStrokeColorResource(R.color.selector_focus_stroke)
            chipStrokeWidth = resources.getDimension(R.dimen.chip_stroke_width) // Define or hardcode 3dp

            isClickable = true
            isFocusable = true
            setOnClickListener { performSearch(text) }
        }
    }

    // Helper resource getter if dimen not defined
    private val Chip.resourcesStrokeWidth: Float
        get() = 3 * resources.displayMetrics.density

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
            // Logic: If streams visible, play first stream.
            // If episode list visible, focus list to pick episode.
            if (binding.rvNavigation.adapter == streamAdapter) {
                streamAdapter.currentList.firstOrNull()?.let { stream ->
                    val intent = Intent(this, PlayerActivity::class.java).apply {
                        putExtra("stream", stream)
                        putExtra("meta", currentMetaItem)
                    }
                    startActivity(intent)
                }
            } else {
                binding.rvNavigation.requestFocus()
            }
        }
    }
}