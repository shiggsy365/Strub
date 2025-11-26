package com.example.stremiompvplayer

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.PopupMenu
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.stremiompvplayer.adapters.StreamAdapter
import com.example.stremiompvplayer.adapters.TextItem
import com.example.stremiompvplayer.adapters.TextListAdapter
import com.example.stremiompvplayer.MainActivity
import com.example.stremiompvplayer.data.ServiceLocator
import com.example.stremiompvplayer.databinding.ActivityDetails2Binding
import com.example.stremiompvplayer.models.Meta
import com.example.stremiompvplayer.models.MetaItem
import com.example.stremiompvplayer.utils.SharedPreferencesManager
import com.example.stremiompvplayer.viewmodels.MainViewModel
import com.example.stremiompvplayer.viewmodels.MainViewModelFactory
import com.google.android.material.chip.Chip
import kotlinx.coroutines.launch

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

    private val UP_NAV_TEXT_ITEM = TextItem("UP_NAV", "Back to Season List", "up", "android.resource://com.example.stremiompvplayer/drawable/ic_arrow_up")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetails2Binding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.rvNavigation.layoutManager = LinearLayoutManager(this)

        streamAdapter = StreamAdapter { stream ->
            // Check for watch progress and show resume/restart dialog if needed
            lifecycleScope.launch {
                val watchProgress = currentMetaItem?.id?.let { viewModel.getWatchProgressSync(it) }

                if (watchProgress != null &&
                    watchProgress.progress > 0 &&
                    !watchProgress.isWatched &&
                    watchProgress.duration > 0) {
                    // Show resume/restart dialog
                    com.example.stremiompvplayer.utils.ResumeRestartDialog.show(
                        this@DetailsActivity2,
                        itemTitle = currentMetaItem?.name ?: "Content",
                        progress = watchProgress.progress,
                        duration = watchProgress.duration,
                        onResume = {
                            playStream(stream)
                        },
                        onRestart = {
                            // Clear progress and play from beginning
                            currentMetaItem?.let { viewModel.clearWatchedStatus(it, syncToTrakt = false) }
                            playStream(stream)
                        }
                    )
                } else {
                    // No progress or already watched - play directly
                    playStream(stream)
                }
            }
        }

        setupAdapters()
    }

    private fun playStream(stream: com.example.stremiompvplayer.models.Stream) {
        val intent = Intent(this, PlayerActivity::class.java).apply {
            putExtra("stream", stream)
            putExtra("meta", currentMetaItem)
        }
        startActivity(intent)
    }

    private fun setupAdapters() {
        textListAdapter = TextListAdapter(
            items = emptyList(),
            onClick = { item -> onTextItemClicked(item) },
            onFocus = { },
            onLongClick = { view, item -> onTextItemLongClicked(view, item) }
        )

        handleIntent()
        setupUI()
        setupObservers()
    }

    override fun onResume() {
        super.onResume()
        if (binding.btnLibrary.visibility == View.VISIBLE) {
            binding.btnLibrary.requestFocus()
        }
        currentMetaItem?.id?.let { viewModel.checkWatchedStatus(it) }
    }

    private fun handleIntent() {
        val id = intent.getStringExtra("metaId") ?: return
        var title = intent.getStringExtra("title") ?: ""
        val poster = intent.getStringExtra("poster")
        val bg = intent.getStringExtra("background")
        var desc = intent.getStringExtra("description")
        currentType = intent.getStringExtra("type") ?: "movie"

        val idParts = id.split(":")
        val isSpecificEpisode = idParts.size >= 4

        if (isSpecificEpisode) {
            val parentId = "${idParts[0]}:${idParts[1]}"
            val season = idParts[2].toIntOrNull() ?: 1
            val episode = idParts[3].toIntOrNull() ?: 1

            currentMetaItem = MetaItem(id, "episode", title, poster, bg, desc)
            viewModel.loadSeriesMeta(parentId)
            viewModel.loadEpisodeStreams(parentId, season, episode)

            binding.rvNavigation.adapter = streamAdapter
            currentSeason = season
            updateButtonVisibility()

            if (!title.contains("S$season")) binding.tvTitle.text = "$title (S$season E$episode)"
            else binding.tvTitle.text = title

            // Initialize logo/title visibility to prevent flash
            binding.tvTitle.visibility = View.GONE
            binding.imgLogo.visibility = View.GONE

            binding.tvDescription.text = desc ?: "Resuming Episode..."
            if (!poster.isNullOrEmpty()) Glide.with(this).load(poster).into(binding.imgPoster)
            if (!bg.isNullOrEmpty()) Glide.with(this).load(bg).into(binding.imgBackground)

            viewModel.fetchCast(parentId, "series")

            // Fetch logo for the parent series
            val parentSeriesMeta = MetaItem(
                id = parentId,
                type = "series",
                name = title,
                poster = poster,
                background = bg,
                description = desc
            )
            viewModel.fetchItemLogo(parentSeriesMeta)

            focusFirstNavigationItem()

        } else {
            currentMetaItem = MetaItem(id, currentType, title, poster, bg, desc)
            updateHeaderUI(title, desc, poster, bg)
            viewModel.fetchCast(id, currentType)

            if (currentType == "movie") {
                viewModel.loadStreams("movie", id)
                binding.rvNavigation.adapter = streamAdapter
                updateButtonVisibility()
            } else {
                currentSeason = null
                updateButtonVisibility()
                viewModel.loadSeriesMeta(id)
                binding.rvNavigation.adapter = textListAdapter
            }
        }

        val checkId = if(isSpecificEpisode) "${idParts[0]}:${idParts[1]}" else id
        viewModel.checkLibraryStatus(checkId)
        viewModel.checkWatchlistStatus(checkId, currentType)
        viewModel.checkWatchedStatus(id)
    }

    private fun updateButtonVisibility() {
        val isPlayableLevel = currentType == "movie" ||
                (currentType == "series" && binding.rvNavigation.adapter == streamAdapter) ||
                (currentType == "episode")

        if (isPlayableLevel) {
            binding.btnPlay.visibility = View.VISIBLE
            if (currentType == "movie") {
                binding.btnLibrary.visibility = View.VISIBLE
                binding.btnWatchlist.visibility = View.VISIBLE
            } else {
                binding.btnLibrary.visibility = View.GONE
                binding.btnWatchlist.visibility = View.GONE
            }
        } else {
            binding.btnPlay.visibility = View.GONE
            binding.btnLibrary.visibility = View.VISIBLE
            binding.btnWatchlist.visibility = View.VISIBLE
        }
    }

    private fun setupObservers() {
        viewModel.director.observe(this) { item ->
            binding.directorChipGroup.removeAllViews()
            if (item != null) {
                val chip = createStyledChip(item)
                binding.directorChipGroup.addView(chip)
            }
        }

        viewModel.castList.observe(this) { cast ->
            binding.castChipGroup.removeAllViews()
            cast.forEach { c ->
                val chip = createStyledChip(c)
                binding.castChipGroup.addView(chip)
            }
        }

        viewModel.streams.observe(this) { streams ->
            if (binding.rvNavigation.adapter == streamAdapter) {
                streamAdapter.submitList(streams)
                // Focus on the stream list when streams are loaded
                binding.rvNavigation.post {
                    focusFirstNavigationItem()
                }
            }
        }

        viewModel.isLoading.observe(this) { loading ->
            // Show spinner in streams pane when loading streams, otherwise show center spinner
            if (binding.rvNavigation.adapter == streamAdapter) {
                binding.progressBarStreams.visibility = if (loading) View.VISIBLE else View.GONE
                binding.progressBar.visibility = View.GONE
            } else {
                binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
                binding.progressBarStreams.visibility = View.GONE
            }
        }

        viewModel.metaDetails.observe(this) { meta ->
            if (meta != null && currentType == "series") {
                currentSeriesMeta = meta
                if (currentSeason == null && binding.rvNavigation.adapter == textListAdapter) {
                    if (!meta.background.isNullOrEmpty()) {
                        Glide.with(this).load(meta.background).into(binding.imgBackground)
                    }

                    val seasons = meta.videos?.map { vid ->
                        TextItem(
                            id = vid.season.toString(),
                            text = "Season ${vid.season}",
                            type = "season",
                            image = meta.poster
                        )
                    } ?: emptyList()
                    textListAdapter.submitList(seasons)
                }
            }
        }

        viewModel.seasonEpisodes.observe(this) { episodes ->
            val episodeItems = episodes.map { ep ->
                TextItem(
                    id = ep.id,
                    text = ep.name,
                    type = "episode",
                    data = ep.description,
                    image = ep.poster ?: currentSeriesMeta?.background
                )
            }.toMutableList()
            episodeItems.add(0, UP_NAV_TEXT_ITEM)
            binding.rvNavigation.adapter = textListAdapter
            textListAdapter.submitList(episodeItems)

            focusFirstNavigationItem()
        }

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

        viewModel.isItemWatched.observe(this) { isWatched ->
            if (isWatched) {
                binding.btnPlay.text = "WATCHED"
                binding.btnPlay.background.setTint(Color.parseColor("#006400"))
            } else {
                binding.btnPlay.text = "NOT WATCHED"
                binding.btnPlay.background.setTint(Color.parseColor("#E50914"))
            }
        }

        // [CHANGE] Updated observer logic for flash prevention
        viewModel.currentLogo.observe(this) { logoUrl ->
            when (logoUrl) {
                "" -> { // Loading
                    binding.tvTitle.visibility = View.GONE
                    binding.imgLogo.visibility = View.GONE
                }
                null -> { // No Logo
                    binding.tvTitle.visibility = View.VISIBLE
                    binding.imgLogo.visibility = View.GONE
                }
                else -> { // Has Logo
                    binding.tvTitle.visibility = View.GONE
                    binding.imgLogo.visibility = View.VISIBLE
                    Glide.with(this)
                        .load(logoUrl)
                        .fitCenter()
                        .into(binding.imgLogo)
                }
            }
        }
    }

    private fun onTextItemClicked(item: TextItem) {
        if (item.type == "up") {
            navigateUp()
            return
        }

        if (item.type == "season") {
            val seasonNum = item.id.toIntOrNull() ?: return
            currentSeason = seasonNum
            viewModel.loadSeasonEpisodes(currentMetaItem!!.id, seasonNum)
        } else if (item.type == "episode") {
            val parts = item.id.split(":")
            if (parts.size >= 4) {
                val season = parts[2].toInt()
                val episode = parts[3].toInt()

                viewModel.loadEpisodeStreams(currentMetaItem!!.id, season, episode)
                binding.rvNavigation.adapter = streamAdapter
                updateButtonVisibility()

                currentMetaItem = MetaItem(
                    id = item.id,
                    type = "episode",
                    name = item.text,
                    poster = currentSeriesMeta?.poster,
                    background = currentSeriesMeta?.background,
                    description = item.data as? String
                )
                viewModel.checkWatchedStatus(currentMetaItem!!.id)

                binding.tvTitle.text = "${currentSeriesMeta?.name} - S${season} E${episode}"
                binding.tvDescription.text = item.data as? String ?: item.text
            }
        }
    }

    private fun focusFirstNavigationItem() {
        binding.rvNavigation.post {
            val holder = binding.rvNavigation.findViewHolderForAdapterPosition(0)
            holder?.itemView?.requestFocus()
        }
    }

    private fun navigateUp() {
        if (binding.rvNavigation.adapter == streamAdapter) {
            binding.rvNavigation.adapter = textListAdapter
            updateButtonVisibility()
            if (currentSeason != null) {
                updateHeaderUI(
                    currentSeriesMeta?.name ?: "",
                    currentSeriesMeta?.description,
                    currentSeriesMeta?.poster,
                    currentSeriesMeta?.background
                )
                focusFirstNavigationItem()
            }
        } else if (currentSeason != null) {
            currentSeason = null
            updateButtonVisibility()
            updateHeaderUI(
                currentSeriesMeta?.name ?: "",
                currentSeriesMeta?.description,
                currentSeriesMeta?.poster,
                currentSeriesMeta?.background
            )
            viewModel.loadSeriesMeta(currentMetaItem!!.id.split(":")[0] + ":" + currentMetaItem!!.id.split(":")[1])
        } else {
            finish()
        }
    }

    private fun createStyledChip(item: MetaItem): Chip {
        return Chip(this).apply {
            text = item.name
            setChipBackgroundColorResource(R.color.chip_background_selector)
            setTextColor(getColor(R.color.chip_text_color))
            setChipStrokeColorResource(R.color.selector_focus_stroke)
            chipStrokeWidth = 3 * resources.displayMetrics.density
            isClickable = true
            isFocusable = true
            setOnClickListener {
                val personId = item.id.removePrefix("tmdb:").toIntOrNull()
                if (personId != null) {
                    val intent = Intent(this@DetailsActivity2, MainActivity::class.java).apply {
                        putExtra("SEARCH_PERSON_ID", personId)
                        putExtra("SEARCH_QUERY", item.name)
                        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    }
                    startActivity(intent)
                }
            }
        }
    }

    private fun onTextItemLongClicked(view: View, item: TextItem) {
        val popup = PopupMenu(this, view)

        // Create temp meta object to represent this episode
        val tempMeta = if (item.type == "episode") {
            MetaItem(
                id = item.id,
                type = "episode",
                name = item.text,
                poster = item.image,
                background = null,
                description = item.data as? String
            )
        } else {
            null
        }

        // [FIX] Use lifecycleScope for synchronous library check
        lifecycleScope.launch {
            if (tempMeta != null) {
                val isInLibrary = viewModel.isItemInLibrarySync(tempMeta.id)
                if (isInLibrary) {
                    popup.menu.add("Remove from Library")
                } else {
                    popup.menu.add("Add to Library")
                }
            }

            popup.menu.add("Mark as Watched")
            popup.menu.add("Clear Watched Status")

            popup.setOnMenuItemClickListener { menuItem ->
                if (tempMeta != null) {
                    when (menuItem.title) {
                        "Mark as Watched" -> {
                            viewModel.markAsWatched(tempMeta)
                            true
                        }
                        "Clear Watched Status" -> {
                            viewModel.clearWatchedStatus(tempMeta)
                            true
                        }
                        "Add to Library" -> {
                            viewModel.addToLibrary(tempMeta)
                            true
                        }
                        "Remove from Library" -> {
                            viewModel.removeFromLibrary(tempMeta.id)
                            true
                        }
                        else -> false
                    }
                } else {
                    false
                }
            }
            popup.show()
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
        // [CHANGE] Initial state is hidden for both title and logo to prevent flash
        binding.tvTitle.visibility = View.GONE
        binding.imgLogo.visibility = View.GONE

        binding.tvDescription.text = desc ?: ""
        if (!poster.isNullOrEmpty()) Glide.with(this).load(poster).into(binding.imgPoster)
        if (!bg.isNullOrEmpty()) Glide.with(this).load(bg).into(binding.imgBackground)

        currentMetaItem?.let { meta ->
            viewModel.fetchItemLogo(meta)
        }
    }

    private fun setupUI() {
        binding.btnLibrary.setOnClickListener {
            val idToCheck = if(currentType=="series") {
                currentMetaItem!!.id.split(":").take(2).joinToString(":")
            } else {
                currentMetaItem!!.id
            }
            val tempMeta = currentMetaItem!!.copy(id = idToCheck)
            viewModel.toggleLibrary(tempMeta)
        }

        binding.btnWatchlist.setOnClickListener {
            val idToCheck = if(currentType=="series") {
                currentMetaItem!!.id.split(":").take(2).joinToString(":")
            } else {
                currentMetaItem!!.id
            }
            val tempMeta = currentMetaItem!!.copy(id = idToCheck)
            viewModel.toggleWatchlist(tempMeta, force = false)
        }

        binding.btnPlay.setOnClickListener {
            // Search for streams for the currently displayed item
            currentMetaItem?.let { item ->
                when (item.type) {
                    "movie" -> {
                        viewModel.loadStreams("movie", item.id)
                        binding.rvNavigation.adapter = streamAdapter
                    }
                    "episode" -> {
                        val parts = item.id.split(":")
                        if (parts.size >= 4) {
                            val parentId = "${parts[0]}:${parts[1]}"
                            val season = parts[2].toIntOrNull() ?: 1
                            val episode = parts[3].toIntOrNull() ?: 1
                            viewModel.loadEpisodeStreams(parentId, season, episode)
                            binding.rvNavigation.adapter = streamAdapter
                        }
                    }
                    "series" -> {
                        // For series, we need to load season list first
                        // This shouldn't normally happen as Play button is hidden for series
                    }
                }
            }
        }

        binding.btnTrailer.setOnClickListener {
            currentMetaItem?.let { item ->
                val idToCheck = if(currentType=="series") {
                    item.id.split(":").take(2).joinToString(":")
                } else {
                    item.id
                }
                val typeToCheck = if(currentType == "episode") "series" else currentType

                lifecycleScope.launch {
                    val trailerUrl = viewModel.fetchTrailer(idToCheck, typeToCheck)
                    if (trailerUrl != null) {
                        // Open YouTube URL in an external app
                        val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(trailerUrl))
                        startActivity(intent)
                    } else {
                        // Show a toast or error message
                        android.widget.Toast.makeText(
                            this@DetailsActivity2,
                            "No trailer available",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }

        binding.btnMoreLikeThis.setOnClickListener {
            currentMetaItem?.let { item ->
                val idToCheck = if(currentType=="series") {
                    item.id.split(":").take(2).joinToString(":")
                } else {
                    item.id
                }
                val typeToCheck = if(currentType == "episode") "series" else currentType

                val intent = Intent(this, SimilarActivity::class.java).apply {
                    putExtra("metaId", idToCheck)
                    putExtra("type", typeToCheck)
                    putExtra("title", item.name)
                    putExtra("poster", item.poster)
                    putExtra("background", item.background)
                }
                startActivity(intent)
            }
        }
    }
}