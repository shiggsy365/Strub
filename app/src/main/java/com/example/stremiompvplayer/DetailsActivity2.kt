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

    private lateinit var textListAdapter: TextListAdapter
    private lateinit var streamAdapter: StreamAdapter

    private var currentMetaItem: MetaItem? = null
    private var currentSeriesMeta: Meta? = null
    private var currentType = "movie"
    private var currentSeason: Int? = null

    private val UP_NAV_TEXT_ITEM = TextItem("UP_NAV", "[..^..]", "up")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetails2Binding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.rvNavigation.layoutManager = LinearLayoutManager(this)

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
            onFocus = {}
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

        // Populate initial UI
        updateHeaderUI(title, desc, poster, bg, null, null)

        // 1. Fetch Content & Credits
        viewModel.fetchCast(id, currentType)
        viewModel.fetchItemLogo(currentMetaItem!!) // FETCH LOGO

        if (currentType == "movie") {
            updateButtonVisibility(true)
            viewModel.loadStreams("movie", id)
            binding.rvNavigation.adapter = streamAdapter
        } else {
            updateButtonVisibility(false)
            viewModel.loadSeriesMeta(id)
            binding.rvNavigation.adapter = textListAdapter
        }

        // Check Status
        viewModel.checkLibraryStatus(id)
        viewModel.checkWatchlistStatus(id, currentType)
    }

    private fun setupObservers() {
        // LOGO OBSERVER
        viewModel.currentLogo.observe(this) { logoUrl ->
            if (logoUrl != null) {
                binding.tvTitle.visibility = View.GONE
                binding.logoImage.visibility = View.VISIBLE
                Glide.with(this).load(logoUrl).fitCenter().into(binding.logoImage)
            } else {
                binding.tvTitle.visibility = View.VISIBLE
                binding.logoImage.visibility = View.GONE
            }
        }

        viewModel.director.observe(this) { name ->
            binding.directorChipGroup.removeAllViews()
            if (!name.isNullOrEmpty()) {
                binding.directorChipGroup.addView(createStyledChip(name))
            }
        }

        viewModel.castList.observe(this) { cast ->
            binding.castChipGroup.removeAllViews()
            cast.forEach { c -> binding.castChipGroup.addView(createStyledChip(c.name)) }
        }

        viewModel.streams.observe(this) { streams ->
            if (binding.rvNavigation.adapter == streamAdapter) streamAdapter.submitList(streams)
        }

        viewModel.metaDetails.observe(this) { meta ->
            if (meta != null && currentType == "series" && currentSeason == null) {
                currentSeriesMeta = meta
                if (!meta.background.isNullOrEmpty()) Glide.with(this).load(meta.background).into(binding.imgBackground)

                val seasons = meta.videos?.map { vid ->
                    TextItem(vid.season.toString(), "Season ${vid.season}", "season")
                } ?: emptyList()
                textListAdapter.submitList(seasons)
            }
        }

        viewModel.seasonEpisodes.observe(this) { episodes ->
            val listWithUp = mutableListOf(UP_NAV_TEXT_ITEM)
            listWithUp.addAll(episodes.map {
                TextItem(it.id, it.name, "episode", it.description)
            })
            textListAdapter.submitList(listWithUp)
        }

        viewModel.isLoading.observe(this) { loading ->
            binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        }

        // Buttons
        viewModel.isItemInLibrary.observe(this) { inLib ->
            binding.btnLibrary.text = if (inLib) "Library -" else "Library +"
            val color = if (inLib) Color.parseColor("#006400") else Color.parseColor("#555555")
            binding.btnLibrary.background.setTint(color)
        }
        viewModel.isItemInWatchlist.observe(this) { inWatch ->
            binding.btnWatchlist.text = if (inWatch) "TMDB Watchlist -" else "TMDB Watchlist +"
            val color = if (inWatch) Color.parseColor("#006400") else Color.parseColor("#555555")
            binding.btnWatchlist.background.setTint(color)
        }
    }

    private fun updateHeaderUI(title: String, desc: String?, poster: String?, bg: String?, date: String?, rating: String?) {
        // Text fallback
        binding.tvTitle.text = title
        binding.tvDescription.text = desc ?: "No description available."

        // Load images
        if (!poster.isNullOrEmpty()) Glide.with(this).load(poster).into(binding.imgPoster)
        if (!bg.isNullOrEmpty()) Glide.with(this).load(bg).into(binding.imgBackground)

        // Metadata
        var metaText = ""
        if (!date.isNullOrEmpty()) metaText += date
        binding.tvMeta.text = metaText

        if (!rating.isNullOrEmpty()) {
            binding.tvRating.text = "★ $rating"
            binding.tvRating.visibility = View.VISIBLE
        } else {
            binding.tvRating.visibility = View.GONE
        }
    }

    // ... (Rest of navigation logic: onTextItemClicked, navigateUp, onBackPressed) ...
    private fun onTextItemClicked(item: TextItem) {
        if (item.type == "up") { navigateUp(); return }

        if (item.type == "season") {
            val seasonNum = item.id.toIntOrNull() ?: return
            currentSeason = seasonNum
            updateButtonVisibility(true)
            viewModel.loadSeasonEpisodes(currentMetaItem!!.id, seasonNum)
        }
        else if (item.type == "episode") {
            val parts = item.id.split(":")
            if (parts.size >= 4) {
                val season = parts[2].toInt()
                val episode = parts[3].toInt()

                updateButtonVisibility(true)
                viewModel.loadEpisodeStreams(currentMetaItem!!.id, season, episode)
                binding.rvNavigation.adapter = streamAdapter

                binding.tvTitle.text = "${currentSeriesMeta?.name} - S${season} E${episode}"
                binding.tvDescription.text = item.data as? String ?: item.text
            }
        }
    }

    private fun navigateUp() {
        if (binding.rvNavigation.adapter == streamAdapter) {
            binding.rvNavigation.adapter = textListAdapter
            if (currentSeason != null) viewModel.loadSeasonEpisodes(currentMetaItem!!.id, currentSeason!!)
        } else if (currentSeason != null) {
            currentSeason = null
            updateButtonVisibility(false)
            viewModel.loadSeriesMeta(currentMetaItem!!.id)
        } else {
            finish()
        }
    }

    private fun updateButtonVisibility(isEpisodeLevel: Boolean) {
        if (currentType == "movie") {
            binding.btnPlay.visibility = View.VISIBLE
            binding.btnLibrary.visibility = View.VISIBLE
            binding.btnWatchlist.visibility = View.VISIBLE
        } else {
            if (isEpisodeLevel) {
                binding.btnPlay.visibility = View.VISIBLE
                binding.btnLibrary.visibility = View.GONE
                binding.btnWatchlist.visibility = View.GONE
            } else {
                binding.btnPlay.visibility = View.GONE
                binding.btnLibrary.visibility = View.VISIBLE
                binding.btnWatchlist.visibility = View.VISIBLE
            }
        }
    }

    private fun createStyledChip(text: String): Chip {
        return Chip(this).apply {
            this.text = text
            setChipBackgroundColorResource(R.color.chip_background_selector)
            setTextColor(getColor(R.color.chip_text_color))
            setChipStrokeColorResource(R.color.selector_focus_stroke)
            chipStrokeWidth = 3 * resources.displayMetrics.density
            isClickable = true
            isFocusable = true
            setOnClickListener { performSearch(text) }
        }
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

    override fun onBackPressed() {
        if (currentType == "series" && (binding.rvNavigation.adapter == streamAdapter || currentSeason != null)) {
            navigateUp()
            return
        }
        super.onBackPressed()
    }
}