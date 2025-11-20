package com.example.stremiompvplayer.ui.series

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.stremiompvplayer.PlayerActivity
import com.example.stremiompvplayer.R
import com.example.stremiompvplayer.adapters.CatalogChipAdapter
import com.example.stremiompvplayer.adapters.PosterAdapter
import com.example.stremiompvplayer.adapters.StreamAdapter
import com.example.stremiompvplayer.data.ServiceLocator
import com.example.stremiompvplayer.databinding.FragmentSeriesBinding
import com.example.stremiompvplayer.models.Catalog
import com.example.stremiompvplayer.models.Meta
import com.example.stremiompvplayer.models.MetaItem
import com.example.stremiompvplayer.utils.SharedPreferencesManager
import com.example.stremiompvplayer.viewmodels.MainViewModel
import com.example.stremiompvplayer.viewmodels.MainViewModelFactory

class SeriesFragment : Fragment() {

    private var _binding: FragmentSeriesBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MainViewModel by activityViewModels {
        MainViewModelFactory(
            ServiceLocator.getInstance(requireContext()),
            SharedPreferencesManager.getInstance(requireContext())
        )
    }

    private lateinit var catalogChipAdapter: CatalogChipAdapter
    private lateinit var posterAdapter: PosterAdapter
    private lateinit var streamAdapter: StreamAdapter

    private enum class Level {
        CATALOG, SEASONS, EPISODES
    }

    private var currentLevel = Level.CATALOG
    private var selectedShow: MetaItem? = null
    private var currentSeriesMeta: Meta? = null
    private val BACK_ITEM_ID = "ACTION_BACK"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSeriesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerViews()
        setupObservers()

        viewModel.initDefaultCatalogs()
    }

    fun handleBackPress(): Boolean {
        if (currentLevel != Level.CATALOG) {
            navigateBack()
            return true
        }
        return false
    }

    private fun setupRecyclerViews() {
        catalogChipAdapter = CatalogChipAdapter(
            onClick = { catalog -> onCatalogSelected(catalog) },
            onLongClick = null
        )
        binding.catalogChipsRecycler.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = catalogChipAdapter
        }

        posterAdapter = PosterAdapter(
            items = emptyList(),
            onClick = { item -> onPosterItemClicked(item) }
        )
        binding.rvPosters.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = posterAdapter
        }

        streamAdapter = StreamAdapter { stream ->
            val intent = Intent(requireContext(), PlayerActivity::class.java).apply {
                putExtra("stream", stream)
                putExtra("meta", selectedShow)
            }
            startActivity(intent)
        }
        binding.rvStreams.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = streamAdapter
        }
    }

    private fun setupObservers() {
        // 1. Observe Enabled Series Catalogs
        viewModel.seriesCatalogs.observe(viewLifecycleOwner) { userCatalogs ->
            val uiCatalogs = userCatalogs.map {
                Catalog(type = "series", id = it.catalogId, name = it.displayName, extraProps = null)
            }
            catalogChipAdapter.setCatalogs(uiCatalogs)

            if (uiCatalogs.isNotEmpty() && posterAdapter.itemCount == 0) {
                viewModel.loadContentForCatalog(userCatalogs[0])
            }
        }

        // 2. Observe Content (Shared with MoviesFragment logic, but works because fragments usually aren't visible simultaneously in this way or data is refreshed)
        // NOTE: Since MainViewModel is shared, switching tabs refreshes data.
        viewModel.currentCatalogContent.observe(viewLifecycleOwner) { items ->
            if (currentLevel == Level.CATALOG) {
                posterAdapter.updateData(items)
                if (items.isNotEmpty()) {
                    val firstItem = items[0]
                    updateHeaderUI(firstItem.name, firstItem.description ?: "", firstItem.poster, null)
                    selectedShow = null
                } else {
                    updateHeaderUI("TV Series", "Browse popular series", null, null)
                }
            }
        }

        viewModel.metaDetails.observe(viewLifecycleOwner) { meta ->
            if (meta != null && currentLevel == Level.CATALOG) {
                currentSeriesMeta = meta
                displaySeasons(meta)
            }
        }

        viewModel.streams.observe(viewLifecycleOwner) { streams ->
            if (currentLevel == Level.EPISODES) {
                if (streams.isNotEmpty()) {
                    streamAdapter.submitList(streams)
                    binding.rvStreams.visibility = View.VISIBLE
                    binding.noStreamsText.visibility = View.GONE
                } else {
                    binding.rvStreams.visibility = View.GONE
                    binding.noStreamsText.visibility = View.VISIBLE
                }
            } else {
                streamAdapter.submitList(emptyList())
                binding.rvStreams.visibility = View.GONE
                binding.noStreamsText.visibility = View.VISIBLE
            }
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
    }

    private fun onCatalogSelected(catalog: Catalog) {
        currentLevel = Level.CATALOG
        binding.catalogChipsRecycler.visibility = View.VISIBLE

        // Find the user catalog config to load
        val userCatalog = viewModel.seriesCatalogs.value?.find { it.catalogId == catalog.id }
        userCatalog?.let { viewModel.loadContentForCatalog(it) }
    }

    private fun onPosterItemClicked(item: MetaItem) {
        if (item.id == BACK_ITEM_ID) {
            navigateBack()
            return
        }

        when (currentLevel) {
            Level.CATALOG -> {
                selectedShow = item
                viewModel.loadSeriesMeta(item.id)
                updateHeaderUI(item.name, item.description ?: "", item.poster, null)
            }
            Level.SEASONS -> {
                val seasonNum = item.id.toIntOrNull()
                if (seasonNum != null) {
                    displayEpisodes(seasonNum)
                }
            }
            Level.EPISODES -> {
                val video = currentSeriesMeta?.videos?.find { it.id == item.id }
                val episodeTitle = video?.title ?: item.name
                val episodeDesc = item.description ?: "No description available."
                val season = video?.season ?: 0
                val episode = video?.number ?: 0
                val subHeader = "S$season:E$episode - $episodeTitle"

                updateHeaderUI(selectedShow?.name ?: "", episodeDesc, currentSeriesMeta?.poster, subHeader)

                if (season > 0 && episode > 0 && selectedShow != null) {
                    viewModel.loadEpisodeStreams(selectedShow!!.id, season, episode)
                }
            }
        }
    }

    private fun displaySeasons(meta: Meta) {
        currentLevel = Level.SEASONS
        binding.catalogChipsRecycler.visibility = View.GONE

        val seasons = meta.videos
            ?.mapNotNull { it.season }
            ?.distinct()
            ?.sorted()
            ?: emptyList()
        val seasonItems = ArrayList<MetaItem>()
        seasonItems.add(createBackItem())
        seasons.forEach { seasonNum: Int ->
            if (seasonNum > 0) {
                seasonItems.add(MetaItem(
                    id = seasonNum.toString(),
                    name = "Season $seasonNum",
                    poster = meta.poster,
                    type = "season",
                    description = "",
                    background = null
                ))
            }
        }
        posterAdapter.updateData(seasonItems)
        viewModel.clearStreams()
        updateHeaderUI(meta.name, "Select a Season", meta.poster, null)
    }

    private fun displayEpisodes(season: Int) {
        currentLevel = Level.EPISODES
        val episodes = currentSeriesMeta?.videos?.filter { it.season == season }?.sortedBy { it.number } ?: emptyList()
        val episodeItems = ArrayList<MetaItem>()
        episodeItems.add(createBackItem())
        episodes.forEach { vid ->
            val image = vid.thumbnail ?: currentSeriesMeta?.poster
            episodeItems.add(MetaItem(
                id = vid.id,
                name = "Ep ${vid.number}: ${vid.title ?: "Episode ${vid.number}"}",
                poster = image,
                type = "episode",
                description = null,
                background = null
            ))
        }
        posterAdapter.updateData(episodeItems)
        viewModel.clearStreams()
        updateHeaderUI(selectedShow?.name ?: "", "Select an Episode", currentSeriesMeta?.poster, "Season $season")
    }

    private fun navigateBack() {
        when (currentLevel) {
            Level.EPISODES -> currentSeriesMeta?.let { displaySeasons(it) }
            Level.SEASONS -> {
                currentLevel = Level.CATALOG
                binding.catalogChipsRecycler.visibility = View.VISIBLE
                selectedShow = null
                currentSeriesMeta = null

                // Reload current catalog content if needed, or just use what's in viewmodel
                viewModel.currentCatalogContent.value?.let {
                    posterAdapter.updateData(it)
                    if (it.isNotEmpty()) {
                        val firstItem = it[0]
                        updateHeaderUI(firstItem.name, firstItem.description ?: "", firstItem.poster, null)
                    }
                }
                viewModel.clearStreams()
            }
            Level.CATALOG -> {}
        }
    }

    private fun createBackItem(): MetaItem {
        val uri = "android.resource://${requireContext().packageName}/${R.drawable.ic_arrow_back_white_24dp}"
        return MetaItem(
            id = BACK_ITEM_ID,
            name = "Back",
            poster = uri,
            type = "navigation",
            description = "Go Back",
            background = null
        )
    }

    private fun updateHeaderUI(title: String, description: String, posterUrl: String?, subHeader: String?) {
        binding.tvTitle.text = title
        binding.tvDescription.text = description
        if (subHeader != null) {
            binding.tvSubHeader.text = subHeader
            binding.tvSubHeader.visibility = View.VISIBLE
        } else {
            binding.tvSubHeader.visibility = View.GONE
        }

        if (!posterUrl.isNullOrEmpty()) {
            Glide.with(this).load(posterUrl).into(binding.imgBackground)
            Glide.with(this).load(posterUrl).into(binding.selectedPoster)
        } else {
            binding.imgBackground.setImageResource(R.drawable.movie)
            binding.selectedPoster.setImageResource(R.drawable.movie)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}