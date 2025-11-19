package com.example.stremiompvplayer.ui.movies

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
import com.example.stremiompvplayer.databinding.FragmentMoviesBinding
import com.example.stremiompvplayer.models.Catalog
import com.example.stremiompvplayer.models.MetaItem
import com.example.stremiompvplayer.PlayerActivity
import com.example.stremiompvplayer.utils.SharedPreferencesManager
import com.example.stremiompvplayer.viewmodels.MainViewModel
import com.example.stremiompvplayer.viewmodels.MainViewModelFactory

class MoviesFragment : Fragment() {

    private var _binding: FragmentMoviesBinding? = null
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

    private var selectedMovie: MetaItem? = null

    // Standard TMDB list catalogs
    private val standardCatalogs = listOf(
        Catalog(type = "movie", id = "popular", name = "Most Popular", extraProps = null),
        Catalog(type = "movie", id = "latest", name = "Latest Releases", extraProps = null),
        Catalog(type = "movie", id = "trending", name = "Trending", extraProps = null)
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMoviesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerViews()
        setupObservers()

        // Load all movie lists on startup
        viewModel.loadMovieLists()
    }

    private fun setupRecyclerViews() {
        // Setup Catalog Chips (Popular, Latest, Trending)
        catalogChipAdapter = CatalogChipAdapter(
            onClick = { catalog -> onCatalogSelected(catalog) },
            onLongClick = null
        )
        binding.catalogChipsRecycler.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = catalogChipAdapter
        }
        catalogChipAdapter.setCatalogs(standardCatalogs)

        // Setup Posters (Horizontal grid at bottom)
        posterAdapter = PosterAdapter(
            items = emptyList(),
            onClick = { item -> onPosterItemClicked(item) }
        )
        binding.moviesGridRecycler.apply {
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
        binding.streamsRecycler.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = streamAdapter
        }
    }

    private fun setupObservers() {
        // Observe Popular Movies (default)
        viewModel.popularMovies.observe(viewLifecycleOwner) { items ->
            posterAdapter.updateData(items)
            if (selectedMovie == null && items.isNotEmpty()) {
                // Optional: Auto-select first movie
                // onPosterItemClicked(items[0])
            }
        }

        // Observe Latest Movies
        viewModel.latestMovies.observe(viewLifecycleOwner) { }

        // Observe Trending Movies
        viewModel.trendingMovies.observe(viewLifecycleOwner) { }

        viewModel.streams.observe(viewLifecycleOwner) { streams ->
            if (streams.isNotEmpty()) {
                binding.streamsRecycler.visibility = View.VISIBLE
                binding.noStreamsText.visibility = View.GONE
                streamAdapter.submitList(streams)
            } else {
                binding.streamsRecycler.visibility = View.GONE
                binding.noStreamsText.visibility = View.VISIBLE
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
        when (catalog.id) {
            "popular" -> {
                viewModel.popularMovies.value?.let { posterAdapter.updateData(it) }
            }
            "latest" -> {
                viewModel.latestMovies.value?.let { posterAdapter.updateData(it) }
            }
            "trending" -> {
                viewModel.trendingMovies.value?.let { posterAdapter.updateData(it) }
            }
        }
        // Clear current selection and streams
        selectedMovie = null
        viewModel.clearStreams()
        updateHeaderUI("Select a Movie", "Choose a movie to see available streams", null)
    }

    private fun onPosterItemClicked(item: MetaItem) {
        selectedMovie = item
        
        // Update Header UI
        updateHeaderUI(item.name, item.description ?: "No description available.", item.poster)

        // Load streams for the selected movie
        viewModel.loadStreams("movie", item.id)
    }

    private fun updateHeaderUI(title: String, description: String, posterUrl: String?) {
        binding.movieTitle.text = title
        binding.movieDescription.text = description
        
        if (!posterUrl.isNullOrEmpty()) {
            Glide.with(this)
                .load(posterUrl)
                .into(binding.selectedPoster)
            
            Glide.with(this)
                .load(posterUrl)
                .into(binding.backgroundImage)
        } else {
            binding.selectedPoster.setImageResource(R.drawable.movie)
            binding.backgroundImage.setImageResource(R.drawable.movie)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
