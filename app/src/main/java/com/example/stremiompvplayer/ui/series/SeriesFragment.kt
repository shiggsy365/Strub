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
import com.example.stremiompvplayer.databinding.FragmentSeriesBinding
import com.example.stremiompvplayer.models.Meta
import com.example.stremiompvplayer.models.MetaItem
import com.example.stremiompvplayer.models.Video
import com.example.stremiompvplayer.PlayerActivity
import com.example.stremiompvplayer.viewmodels.MainViewModel

class SeriesFragment : Fragment() {

    private var _binding: FragmentSeriesBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MainViewModel by activityViewModels()

    private lateinit var posterAdapter: PosterAdapter
    private lateinit var streamAdapter: StreamAdapter

    // Navigation State Management
    private enum class Level {
        CATALOG, // Top level: List of Shows
        SEASONS, // Second level: List of Seasons for a Show
        EPISODES // Third level: List of Episodes for a Season
    }

    private var currentLevel = Level.CATALOG
    
    // Data caching for navigation
    private var selectedShow: MetaItem? = null
    private var currentSeriesMeta: Meta? = null
    private var selectedSeasonNumber: Int? = null

    // Special ID for the back button
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
        
        // Initial Load
        if (currentLevel == Level.CATALOG) {
            viewModel.loadCatalogs("series")
        }
    }

    private fun setupRecyclerViews() {
        // Setup Posters (Horizontal list at bottom)
        posterAdapter = PosterAdapter { item ->
            onPosterItemClicked(item)
        }
        binding.rvPosters.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = posterAdapter
        }

        // Setup Streams (Vertical list in box)
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
        // Observe Catalogs (Level 0)
        viewModel.catalogs.observe(viewLifecycleOwner) { items ->
            if (currentLevel == Level.CATALOG) {
                posterAdapter.submitList(items)
                updateUIForCatalogLevel()
            }
        }

        // Observe Series Details (Loaded when clicking a show)
        viewModel.metaDetails.observe(viewLifecycleOwner) { meta ->
            if (meta != null && currentLevel == Level.CATALOG) {
                // Transition to Seasons View
                currentSeriesMeta = meta
                displaySeasons(meta)
            }
        }

        // Observe Streams (Only relevant in Episode Level)
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
        // Handle Back Button Click
        if (item.id == BACK_ITEM_ID) {
            navigateBack()
            return
        }

        when (currentLevel) {
            Level.CATALOG -> {
                // User clicked a Show -> Go to Seasons
                selectedShow = item
                viewModel.loadMeta("series", item.id)
                // UI update happens in observer when data arrives
                updateHeaderUI(item.name, item.description ?: "", item.poster, null)
            }
            Level.SEASONS -> {
                // User clicked a Season -> Go to Episodes
                // We stored season number in generic item.id or assume name parsing?
                // Better: The displaySeasons logic constructed MetaItems with ID = season number string
                val seasonNum = item.id.toIntOrNull()
                if (seasonNum != null) {
                    selectedSeasonNumber = seasonNum
                    displayEpisodes(seasonNum)
                }
            }
            Level.EPISODES -> {
                // User clicked an Episode -> Load Streams
                // item.id holds the video ID (e.g. tt12345:1:1)
                
                // Find the full video object to get details
                val video = currentSeriesMeta?.videos?.find { it.id == item.id }
                
                val episodeTitle = video?.name ?: item.name
                val episodeDesc = video?.overview ?: "No description available."
                val season = video?.season ?: 0
                val episode = video?.episode ?: 0
                val subHeader = "S$season:E$episode - $episodeTitle"

                updateHeaderUI(selectedShow?.name ?: "", episodeDesc, currentSeriesMeta?.poster, subHeader)
                
                // Load streams
                viewModel.loadStreams("series", item.id)
            }
        }
    }

    private fun displaySeasons(meta: Meta) {
        currentLevel = Level.SEASONS
        
        // Extract unique seasons from videos
        // Assuming meta.videos contains all episodes
        val seasons = meta.videos?.map { it.season }?.distinct()?.sorted() ?: emptyList()
        
        val seasonItems = ArrayList<MetaItem>()
        
        // Add Back Button
        seasonItems.add(createBackItem())

        // Create MetaItems for seasons
        seasons.forEach { seasonNum ->
            if (seasonNum > 0) { // Filter out specials if season is 0, unless desired
                seasonItems.add(
                    MetaItem(
                        id = seasonNum.toString(),
                        name = "Season $seasonNum",
                        poster = meta.poster, // Use series poster
                        type = "season",
                        description = ""
                    )
                )
            }
        }

        posterAdapter.submitList(seasonItems)
        
        // Clear streams when entering Season view
        viewModel.clearStreams()
        
        // Update text to prompt user
        binding.tvTitle.text = meta.name
        binding.tvDescription.text = "Select a Season"
        binding.tvSubHeader.visibility = View.GONE
    }

    private fun displayEpisodes(season: Int) {
        currentLevel = Level.EPISODES
        
        val episodes = currentSeriesMeta?.videos?.filter { it.season == season }?.sortedBy { it.episode } ?: emptyList()
        
        val episodeItems = ArrayList<MetaItem>()
        
        // Add Back Button
        episodeItems.add(createBackItem())

        episodes.forEach { vid ->
            // Use thumbnail if available, else series poster
            val image = if (!vid.thumbnail.isNullOrEmpty()) vid.thumbnail else currentSeriesMeta?.poster
            
            episodeItems.add(
                MetaItem(
                    id = vid.id, // Important: This ID is used for getStreams
                    name = "Ep ${vid.episode}: ${vid.name ?: "Episode ${vid.episode}"}",
                    poster = image,
                    type = "episode",
                    description = vid.overview
                )
            )
        }

        posterAdapter.submitList(episodeItems)
        
        // Clear streams until specific episode clicked
        viewModel.clearStreams()
        
        binding.tvDescription.text = "Select an Episode"
        binding.tvSubHeader.text = "Season $season"
        binding.tvSubHeader.visibility = View.VISIBLE
    }

    private fun navigateBack() {
        when (currentLevel) {
            Level.EPISODES -> {
                // Go back to Seasons
                currentSeriesMeta?.let { displaySeasons(it) }
            }
            Level.SEASONS -> {
                // Go back to Catalog
                currentLevel = Level.CATALOG
                selectedShow = null
                currentSeriesMeta = null
                viewModel.loadCatalogs("series") // Reload catalog or use cached if VM persists it
                updateUIForCatalogLevel()
                viewModel.clearStreams()
            }
            Level.CATALOG -> {
                // Already at top, maybe exit app or do nothing
            }
        }
    }
    
    private fun createBackItem(): MetaItem {
        // We use a resource URI for the poster so Glide can load the local drawable
        // Note: You need to ensure R.drawable.ic_arrow_back_white_24dp exists
        val uri = "android.resource://${requireContext().packageName}/${R.drawable.ic_arrow_back_white_24dp}"
        return MetaItem(
            id = BACK_ITEM_ID,
            name = "Back",
            poster = uri,
            type = "navigation",
            description = "Go Back"
        )
    }

    private fun updateUIForCatalogLevel() {
        binding.tvTitle.text = "TV Series"
        binding.tvSubHeader.visibility = View.GONE
        binding.tvDescription.text = "Browse available TV Series."
        binding.imgBackground.setImageResource(R.drawable.movie) // Reset background
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
