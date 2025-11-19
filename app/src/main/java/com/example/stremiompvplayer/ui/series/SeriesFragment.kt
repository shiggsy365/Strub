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
import com.example.stremiompvplayer.adapters.PosterAdapter
import com.example.stremiompvplayer.adapters.StreamAdapter
import com.example.stremiompvplayer.data.ServiceLocator
import com.example.stremiompvplayer.databinding.FragmentSeriesBinding
import com.example.stremiompvplayer.models.Meta
import com.example.stremiompvplayer.models.MetaItem
import com.example.stremiompvplayer.PlayerActivity
import com.example.stremiompvplayer.viewmodels.MainViewModel
import com.example.stremiompvplayer.viewmodels.MainViewModelFactory

class SeriesFragment : Fragment() {

    private var _binding: FragmentSeriesBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: MainViewModel by activityViewModels {
        MainViewModelFactory(ServiceLocator.getInstance(requireContext()))
    }

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
        
        if (currentLevel == Level.CATALOG) {
            viewModel.loadUserEnabledCatalogs("series")
        }
    }

    private fun setupRecyclerViews() {
        posterAdapter = PosterAdapter { item -> onPosterItemClicked(item) }
        binding.rvPosters.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = posterAdapter
        }

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
        viewModel.catalogs.observe(viewLifecycleOwner) { items ->
            if (currentLevel == Level.CATALOG) {
                posterAdapter.submitList(items)
                updateUIForCatalogLevel()
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
    
    private fun onPosterItemClicked(item: MetaItem) {
        if (item.id == BACK_ITEM_ID) {
            navigateBack()
            return
        }

        when (currentLevel) {
            Level.CATALOG -> {
                selectedShow = item
                viewModel.loadMeta("series", item.id)
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
                val episodeTitle = video?.name ?: item.name
                val episodeDesc = video?.overview ?: "No description available."
                val season = video?.season ?: 0
                val episode = video?.episode ?: 0
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
            if (seasonNum > 0) {
                seasonItems.add(MetaItem(id = seasonNum.toString(), name = "Season $seasonNum", poster = meta.poster, type = "season", description = ""))
            }
        }
        posterAdapter.submitList(seasonItems)
        viewModel.clearStreams()
        binding.tvTitle.text = meta.name
        binding.tvDescription.text = "Select a Season"
        binding.tvSubHeader.visibility = View.GONE
    }

    private fun displayEpisodes(season: Int) {
        currentLevel = Level.EPISODES
        val episodes = currentSeriesMeta?.videos?.filter { it.season == season }?.sortedBy { it.episode } ?: emptyList()
        val episodeItems = ArrayList<MetaItem>()
        episodeItems.add(createBackItem())
        episodes.forEach { vid ->
            val image = if (!vid.thumbnail.isNullOrEmpty()) vid.thumbnail else currentSeriesMeta?.poster
            episodeItems.add(MetaItem(id = vid.id, name = "Ep ${vid.episode}: ${vid.name ?: "Episode ${vid.episode}"}", poster = image, type = "episode", description = vid.overview))
        }
        posterAdapter.submitList(episodeItems)
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
                viewModel.loadUserEnabledCatalogs("series") 
                updateUIForCatalogLevel()
                viewModel.clearStreams()
            }
            Level.CATALOG -> {}
        }
    }

    private fun createBackItem(): MetaItem {
        val uri = "android.resource://${requireContext().packageName}/${R.drawable.ic_arrow_back_white_24dp}"
        return MetaItem(id = BACK_ITEM_ID, name = "Back", poster = uri, type = "navigation", description = "Go Back")
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
