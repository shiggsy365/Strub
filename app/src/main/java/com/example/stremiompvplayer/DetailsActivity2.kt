package com.example.stremiompvplayer

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.PopupMenu
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
            val intent = Intent(this, PlayerActivity::class.java).apply {
                putExtra("stream", stream)
                putExtra("meta", currentMetaItem)
            }
            startActivity(intent)
        }

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

            binding.tvDescription.text = desc ?: "Resuming Episode..."
            if (!poster.isNullOrEmpty()) Glide.with(this).load(poster).into(binding.imgPoster)
            if (!bg.isNullOrEmpty()) Glide.with(this).load(bg).into(binding.imgBackground)

            viewModel.fetchCast(parentId, "series")

            focusFirstNavigationItem()

        } else {
            currentMetaItem = MetaItem(id, currentType, title, poster, bg, desc)
            updateHeaderUI(title, desc, poster, bg)
            viewModel.fetchCast(id, currentType)

            if (currentType == "movie") {
                // Movies are always at "play level"
                viewModel.loadStreams("movie", id)
                binding.rvNavigation.adapter = streamAdapter
                updateButtonVisibility()
            } else {
                // Series starts at top level
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
                focusFirstNavigationItem()
            }
        }

        viewModel.isLoading.observe(this) { loading ->
            binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
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

        // NEW: Observe logo changes and hide title when logo is available
        viewModel.currentLogo.observe(this) { logoUrl ->
            if (logoUrl != null) {
                binding.tvTitle.visibility = View.GONE
                binding.imgBackground.post {
                    // Create an ImageView for the logo overlay if it doesn't exist
                    val logoView = binding.imgBackground.findViewById<android.widget.ImageView>(R.id.titleLogo)
                    if (logoView == null) {
                        val newLogoView = android.widget.ImageView(this)
                        newLogoView.id = R.id.titleLogo
                        newLogoView.scaleType = android.widget.ImageView.ScaleType.FIT_CENTER
                        val params = android.widget.FrameLayout.LayoutParams(
                            resources.getDimensionPixelSize(R.dimen.spacing_xxxl) * 6,
                            resources.getDimensionPixelSize(R.dimen.spacing_xxxl) * 2
                        )
                        params.gravity = android.view.Gravity.TOP or android.view.Gravity.CENTER_HORIZONTAL
                        params.topMargin = resources.getDimensionPixelSize(R.dimen.spacing_xl)
                        
                        Glide.with(this)
                            .load(logoUrl)
                            .fitCenter()
                            .into(newLogoView)
                    }
                }
            } else {
                binding.tvTitle.visibility = View.VISIBLE
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

    private fun onTextItemLongClicked(view: View, item: TextItem) {
        val popup = PopupMenu(this, view)
        popup.menu.add("Mark as Watched")
        popup.menu.add("Clear Watched Status")

        popup.setOnMenuItemClickListener { menuItem ->
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
                    else -> false
                }
            } else {
                false
            }
        }
        popup.show()
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
        
        // Try to fetch and display logo instead of title
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
            val isWatched = binding.btnPlay.text == "WATCHED"
            currentMetaItem?.let { item ->
                if (isWatched) {
                    viewModel.clearWatchedStatus(item)
                } else {
                    viewModel.markAsWatched(item)
                }
            }
        }
    }
}
