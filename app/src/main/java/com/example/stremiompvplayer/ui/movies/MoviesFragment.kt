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

        viewModel.loadMovieLists()
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
        catalogChipAdapter.setCatalogs(standardCatalogs)

        posterAdapter = PosterAdapter(
            items = emptyList(),
            onClick = { item -> onPosterItemClicked(item) }
        )
        binding.moviesGridRecycler.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = posterAdapter
        }

        // FIXED CLICK LISTENER
        streamAdapter = StreamAdapter { stream ->
            val intent = Intent(requireContext(), PlayerActivity::class.java).apply {
                // Passing Full Stream Object
                putExtra("stream", stream)
                // Passing MetaItem for context
                putExtra("meta", selectedMovie)
            }
            startActivity(intent)
        }
        binding.streamsRecycler.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = streamAdapter
        }
    }

    private fun setupObservers() {
        viewModel.popularMovies.observe(viewLifecycleOwner) { items ->
            posterAdapter.updateData(items)
            if (selectedMovie == null && items.isNotEmpty()) {
                val firstItem = items[0]
                updateHeaderUI(firstItem.name, firstItem.description ?: "No description available.", firstItem.poster)
            }
        }

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
        var itemsToSelect: List<MetaItem>? = null

        when (catalog.id) {
            "popular" -> itemsToSelect = viewModel.popularMovies.value
            "latest" -> itemsToSelect = viewModel.latestMovies.value
            "trending" -> itemsToSelect = viewModel.trendingMovies.value
        }

        itemsToSelect?.let {
            posterAdapter.updateData(it)
            if (it.isNotEmpty()) {
                val firstItem = it[0]
                updateHeaderUI(firstItem.name, firstItem.description ?: "No description available.", firstItem.poster)
            }
        }

        selectedMovie = null
        viewModel.clearStreams()

        if (itemsToSelect.isNullOrEmpty()) {
            updateHeaderUI("Select a Movie", "Choose a movie to see available streams", null)
        }
    }

    private fun onPosterItemClicked(item: MetaItem) {
        selectedMovie = item
        updateHeaderUI(item.name, item.description ?: "No description available.", item.poster)
        viewModel.loadStreams("movie", item.id)
    }

    private fun updateHeaderUI(title: String, description: String, posterUrl: String?) {
        binding.movieTitle.text = title
        binding.movieDescription.text = description

        if (!posterUrl.isNullOrEmpty()) {
            Glide.with(this).load(posterUrl).into(binding.selectedPoster)
            Glide.with(this).load(posterUrl).into(binding.backgroundImage)
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