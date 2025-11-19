package com.example.stremiompvplayer.ui.series

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.stremiompvplayer.R
import com.example.stremiompvplayer.adapters.CatalogChipAdapter
import com.example.stremiompvplayer.adapters.PosterAdapter
import com.example.stremiompvplayer.adapters.StreamAdapter
import com.example.stremiompvplayer.data.ServiceLocator
import com.example.stremiompvplayer.databinding.FragmentSeriesBinding
import com.example.stremiompvplayer.models.Catalog
import com.example.stremiompvplayer.models.Meta
import com.example.stremiompvplayer.models.MetaItem
import com.example.stremiompvplayer.PlayerActivity
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

    // Standard TMDB list catalogs
    private val standardCatalogs = listOf(
        Catalog(type = "series", id = "popular", name = "Most Popular", extraProps = null),
        Catalog(type = "series", id = "latest", name = "Latest Releases", extraProps = null),
        Catalog(type = "series", id = "trending", name = "Trending", extraProps = null)
    )

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
        
        if (currentLevel == Level.CATALOG) {
            viewModel.loadSeriesLists()
        }
    }

    private fun setupRecyclerViews() {
        // Setup Catalog Chips
        catalogChipAdapter = CatalogChipAdapter(
            onClick = { catalog -> onCatalogSelected(catalog) },
            onLongClick = null
        )
        binding.catalogChipsRecycler.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = catalogChipAdapter
        }
        catalogChipAdapter.setCatalogs(standardCatalogs)

        // Setup Posters
        posterAdapter = PosterAdapter(
            items = emptyList(),
            onClick = { item -> onPosterItemClicked(item) }
        )
        binding.rvPosters.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = posterAdapter
        }

        // Setup Streams
        streamAdapter = StreamAdapter { stream ->
            val intent = Intent(requireContext(), PlayerActivity::class.java).apply {
                putExtra("streamUrl", stream.url)
                putExtra("streamTitle", stream.title ?: "Unknown Stream")
            }
            startActivity(intent)
        }
        binding.rvStreams.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = streamAdapter
        }
    }

    private fun setupObservers() {
        // Observe Popular Series (default)
        viewModel.popularSeries.observe(viewLifecycleOwner) { items ->
            if (currentLevel == Level.CATALOG) {
                posterAdapter.updateData(items)
            }
        }

        viewModel.latestSeries.observe(viewLifecycleOwner) { }
        viewModel.trendingSeries.observe(viewLifecycleOwner) { }

        viewModel.metaDetails.observe(viewLifecycleOwner) { meta ->
            if (meta != null && currentLevel == Level.CATALOG) {
                currentSeriesMeta = meta
                displaySeasons(meta)
            }
        }

        viewModel.streams.observe(viewLifecycleOwner) { streams ->
            if (currentLevel == Level.EPISODES) {
                streamAdapter.submitList(streams)
            } else {
                streamAdapter.submitList(emptyList())
            }
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            if (error != null) {
                Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun onCatalogSelected(catalog: Catalog) {
        if (currentLevel != Level.CATALOG) return
        
        when (catalog.id) {
            "popular" -> {
                viewModel.popularSeries.value?.let { posterAdapter.updateData(it) }
            }
            "latest" -> {
                viewModel.latestSeries.value?.let { posterAdapter.updateData(it) }
            }
            "trending" -> {
                viewModel.trendingSeries.value?.let { posterAdapter.updateData(it) }
            }
        }
        selectedShow = null
        currentSeriesMeta = null
        viewModel.clearStreams()
        updateHeaderUI("Select a Series", "Choose a series to explore", null, null)
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
                viewModel.loadStreams("series", item.id)
            }
        }
    }

    private fun displaySeasons(meta: Meta) {
        currentLevel = Level.SEASONS
        val seasons = meta.videos?.map { it.season }?.distinct()?.sorted() ?: emptyList()
        val seasonItems = ArrayList<MetaItem>()
        seasonItems.add(createBackItem())
        seasons.forEach { seasonNum ->
            if (seasonNum != null && seasonNum > 0) {
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
        binding.tvTitle.text = meta.name
        binding.tvDescription.text = "Select a Season"
        binding.tvSubHeader.visibility = View.GONE
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
        binding.tvDescription.text = "Select an Episode"
        binding.tvSubHeader.text = "Season $season"
        binding.tvSubHeader.visibility = View.VISIBLE
    }

    private fun navigateBack() {
        when (currentLevel) {
            Level.EPISODES -> currentSeriesMeta?.let { displaySeasons(it) }
            Level.SEASONS -> {
                currentLevel = Level.CATALOG
                selectedShow = null
                currentSeriesMeta = null
                viewModel.loadSeriesLists()
                viewModel.popularSeries.value?.let { posterAdapter.updateData(it) }
                updateUIForCatalogLevel()
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

    private fun updateUIForCatalogLevel() {
        binding.tvTitle.text = "TV Series"
        binding.tvSubHeader.visibility = View.GONE
        binding.tvDescription.text = "Browse available TV Series."
        binding.imgBackground.setImageResource(R.drawable.movie)
        viewModel.clearStreams()
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
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
