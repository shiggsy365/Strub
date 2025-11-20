package com.example.stremiompvplayer

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
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
import com.example.stremiompvplayer.models.Video
import com.example.stremiompvplayer.utils.SharedPreferencesManager
import com.example.stremiompvplayer.viewmodels.MainViewModel
import com.example.stremiompvplayer.viewmodels.MainViewModelFactory
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class DetailsActivity2 : AppCompatActivity() {

    private lateinit var binding: ActivityDetails2Binding

    private val viewModel: MainViewModel by viewModels {
        MainViewModelFactory(
            ServiceLocator.getInstance(applicationContext),
            SharedPreferencesManager.getInstance(this)
        )
    }

    private var currentMetaItem: MetaItem? = null
    private var seriesMeta: Meta? = null
    private var isSeries = false

    private enum class Level { TOP, EPISODES, STREAMS }
    private var currentLevel = Level.TOP
    private var currentSeasonNumber: Int = 0
    private var currentEpisodeNumber: Int = 0

    private lateinit var streamAdapter: StreamAdapter
    private lateinit var textListAdapter: TextListAdapter

    // Dark Green for Added state
    private val COLOR_ADDED = Color.parseColor("#006400")
    // Default transparent/black for Removed state
    private val COLOR_DEFAULT = Color.parseColor("#1A1A1A")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetails2Binding.inflate(layoutInflater)
        setContentView(binding.root)

        val metaId = intent.getStringExtra("metaId")
        val title = intent.getStringExtra("title")
        val poster = intent.getStringExtra("poster")
        val background = intent.getStringExtra("background")
        val description = intent.getStringExtra("description")
        val type = intent.getStringExtra("type") ?: "movie"

        if (metaId == null) { finish(); return }

        isSeries = (type == "series" || type == "tv")
        currentMetaItem = MetaItem(metaId, type, title ?: "", poster, background, description)

        setupUI()
        populateBasicInfo(title, poster, background, description)

        viewModel.fetchCast(metaId, type)

        // Check initial watchlist status
        viewModel.checkWatchlistStatus(metaId, type)

        if (isSeries) {
            viewModel.loadSeriesMeta(metaId)
            setupSeriesObservers()
            binding.rightPaneTitle.text = "Series"
        } else {
            viewModel.loadStreams(type, metaId)
            setupMovieObservers()
        }

        setupCreditsObservers()

        // Set default focus to Watchlist button
        binding.root.post {
            binding.btnTmdb.requestFocus()
        }
    }

    private fun setupUI() {
        binding.backButton.setOnClickListener { handleBackNavigation() }

        binding.btnTmdb.setOnClickListener {
            showWatchlistDialog()
        }

        streamAdapter = StreamAdapter { stream ->
            val intent = Intent(this, PlayerActivity::class.java).apply {
                putExtra("stream", stream)
                putExtra("meta", currentMetaItem)
            }
            startActivity(intent)
        }

        textListAdapter = TextListAdapter(
            items = emptyList(),
            onClick = { item -> handleListSelection(item) },
            onFocus = { item -> handleListFocus(item) }
        )

        binding.streamRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.streamRecyclerView.adapter = if (isSeries) textListAdapter else streamAdapter
    }

    private fun handleBackNavigation() {
        if (!isSeries || currentLevel == Level.TOP) {
            super.onBackPressed()
        } else if (currentLevel == Level.EPISODES) {
            showSeasonList()
        } else if (currentLevel == Level.STREAMS) {
            showEpisodeList(currentSeasonNumber)
        }
    }

    override fun onBackPressed() {
        handleBackNavigation()
    }

    private fun handleListSelection(item: TextItem) {
        when (item.type) {
            "back" -> handleBackNavigation()
            "season" -> {
                val seasonNum = item.id.toIntOrNull() ?: 1
                showEpisodeList(seasonNum)
            }
            "episode" -> {
                val epData = item.data as? Video
                if (epData != null) {
                    currentSeasonNumber = epData.season ?: 1
                    currentEpisodeNumber = epData.number ?: 1
                    loadEpisodeStreams(currentMetaItem!!.id, currentSeasonNumber, currentEpisodeNumber)
                }
            }
        }
    }

    private fun handleListFocus(item: TextItem) {
        if (item.type == "episode") {
            val video = item.data as? Video
            if (video != null) {
                binding.title.text = video.title ?: "Episode ${video.number}"
                binding.subTitle.visibility = View.VISIBLE
                binding.subTitle.text = "Series ${video.season}, Episode ${video.number}"
            }
        } else if (item.type == "season") {
            populateBasicInfo(currentMetaItem?.name, currentMetaItem?.poster, currentMetaItem?.background, currentMetaItem?.description)
        }

        // Request focus on the stream list item when focused.
        if (isSeries) {
            binding.streamRecyclerView.post {
                val currentItems = textListAdapter.currentList
                val position = currentItems.indexOf(item)
                val viewHolder = binding.streamRecyclerView.findViewHolderForAdapterPosition(position)
                viewHolder?.itemView?.requestFocus()
            }
        }
    }

    private fun setupSeriesObservers() {
        viewModel.metaDetails.observe(this) { meta ->
            if (meta != null) {
                seriesMeta = meta
                showSeasonList()
            }
        }

        viewModel.streams.observe(this) { streams ->
            if (currentLevel == Level.STREAMS) {
                binding.progressBar.visibility = View.GONE
                if (streams.isNotEmpty()) {
                    streamAdapter.submitList(streams)
                    binding.streamRecyclerView.adapter = streamAdapter
                    binding.textNoStreams.visibility = View.GONE
                    requestFocusOnList()
                } else {
                    binding.textNoStreams.visibility = View.VISIBLE
                    binding.textNoStreams.text = "No streams found for S${currentSeasonNumber}:E${currentEpisodeNumber}"
                }
            }
        }
    }

    private fun setupMovieObservers() {
        viewModel.streams.observe(this) { streams ->
            streamAdapter.submitList(streams)
            binding.progressBar.visibility = View.GONE
            binding.textNoStreams.visibility = if (streams.isEmpty()) View.VISIBLE else View.GONE

            if (streams.isNotEmpty()) {
                requestFocusOnList()
            }
        }
    }

    private fun setupWatchlistObserver() {
        viewModel.isItemInWatchlist.observe(this) { inWatchlist ->
            if (inWatchlist) {
                binding.btnTmdb.backgroundTintList = ColorStateList.valueOf(COLOR_ADDED)
                binding.btnTmdb.text = "Watchlist (Added)"
            } else {
                binding.btnTmdb.backgroundTintList = ColorStateList.valueOf(COLOR_DEFAULT)
                binding.btnTmdb.text = "Watchlist (Add)"
            }
        }
    }

    private fun setupCreditsObservers() {
        setupWatchlistObserver()

        viewModel.castList.observe(this) { castList ->
            binding.castGroup.removeAllViews()
            castList.forEach { actor ->
                // Chip styling is controlled by item_catalog_chip.xml which uses the fixed selectors
                val chip = LayoutInflater.from(this).inflate(R.layout.item_catalog_chip, binding.castGroup, false) as Chip
                chip.text = actor.name
                chip.setOnClickListener { launchSearch(actor.name) }
                binding.castGroup.addView(chip)
            }
        }
        viewModel.director.observe(this) { directorName ->
            binding.directorGroup.removeAllViews()
            if (directorName != null) {
                val chip = LayoutInflater.from(this).inflate(R.layout.item_catalog_chip, binding.directorGroup, false) as Chip
                chip.text = directorName
                chip.setOnClickListener { launchSearch(directorName) }
                binding.directorGroup.addView(chip)
            }
        }
    }

    private fun showSeasonList() {
        currentLevel = Level.TOP
        binding.rightPaneTitle.text = "Series"
        binding.btnTmdb.visibility = View.VISIBLE
        binding.subTitle.visibility = View.GONE
        populateBasicInfo(seriesMeta?.name, seriesMeta?.poster, seriesMeta?.background, seriesMeta?.description)

        val seasons = seriesMeta?.videos
            ?.mapNotNull { it.season }
            ?.distinct()
            ?.sorted()
            ?: emptyList()

        val items = seasons.map { seasonNum ->
            TextItem(id = seasonNum.toString(), text = "Series $seasonNum", type = "season")
        }

        textListAdapter.submitList(items)
        binding.streamRecyclerView.adapter = textListAdapter
        requestFocusOnList()
    }

    private fun showEpisodeList(season: Int) {
        currentLevel = Level.EPISODES
        currentSeasonNumber = season
        binding.rightPaneTitle.text = "Series $season"
        binding.btnTmdb.visibility = View.GONE

        val episodes = seriesMeta?.videos
            ?.filter { it.season == season }
            ?.sortedBy { it.number }
            ?: emptyList()

        val items = mutableListOf<TextItem>()
        items.add(TextItem("back", "[...]", "back"))

        episodes.map { video ->
            items.add(TextItem(id = video.id, text = "Ep ${video.number}: ${video.title}", type = "episode", data = video))
        }

        textListAdapter.submitList(items)
        binding.streamRecyclerView.adapter = textListAdapter
        requestFocusOnList()
    }

    private fun loadEpisodeStreams(metaId: String, season: Int, episode: Int) {
        currentLevel = Level.STREAMS
        binding.rightPaneTitle.text = "Streams S${season}:E${episode}"
        binding.progressBar.visibility = View.VISIBLE
        binding.streamRecyclerView.adapter = streamAdapter
        streamAdapter.submitList(emptyList())

        viewModel.loadEpisodeStreams(metaId, season, episode)
    }

    private fun requestFocusOnList() {
        binding.streamRecyclerView.post {
            // Request focus on the first item (or the back button if present, which is index 0)
            val viewHolder = binding.streamRecyclerView.findViewHolderForAdapterPosition(0)
            viewHolder?.itemView?.requestFocus()
        }
    }

    private fun populateBasicInfo(title: String?, poster: String?, background: String?, description: String?) {
        binding.title.text = title
        binding.description.text = description ?: "No description available."
        if (!poster.isNullOrEmpty()) Glide.with(this).load(poster).into(binding.poster)
        val backdropUrl = if (!background.isNullOrEmpty()) background else poster
        if (!backdropUrl.isNullOrEmpty()) Glide.with(this).load(backdropUrl).into(binding.backgroundImage)

        binding.subTitle.visibility = View.GONE
    }

    private fun showWatchlistDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("TMDB Watchlist")
            .setItems(arrayOf("Add to Watchlist", "Remove from Watchlist")) { _, which ->
                currentMetaItem?.let { item ->
                    // toggleWatchlist updates LiveData on success, which updates the button via observer
                    viewModel.toggleWatchlist(item, which == 0)
                }
            }
            .show()
    }

    private fun launchSearch(query: String) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("SEARCH_QUERY", query)
        }
        startActivity(intent)
    }
}