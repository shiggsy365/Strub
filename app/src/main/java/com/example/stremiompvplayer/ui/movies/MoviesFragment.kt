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
import com.example.stremiompvplayer.PlayerActivity
import com.example.stremiompvplayer.R
import com.example.stremiompvplayer.adapters.CatalogChipAdapter
import com.example.stremiompvplayer.adapters.PosterAdapter
import com.example.stremiompvplayer.adapters.StreamAdapter
import com.example.stremiompvplayer.data.ServiceLocator
import com.example.stremiompvplayer.databinding.FragmentMoviesBinding
import com.example.stremiompvplayer.models.Catalog
import com.example.stremiompvplayer.models.MetaItem
import com.example.stremiompvplayer.models.UserCatalog
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

        // Initialize defaults if needed, though MainActivity usually handles this
        viewModel.initDefaultCatalogs()
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
        binding.moviesGridRecycler.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = posterAdapter
        }

        streamAdapter = StreamAdapter { stream ->
            val intent = Intent(requireContext(), PlayerActivity::class.java).apply {
                putExtra("stream", stream)
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
        // UPDATED: Observe the dynamic list of enabled movie catalogs
        viewModel.movieCatalogs.observe(viewLifecycleOwner) { userCatalogs ->
            // Convert UserCatalog to generic Catalog model for adapter
            val uiCatalogs = userCatalogs.map {
                Catalog(type = "movie", id = it.catalogId, name = it.displayName, extraProps = null)
            }
            catalogChipAdapter.setCatalogs(uiCatalogs)

            // Load the first catalog if nothing is loaded yet and list isn't empty
            if (uiCatalogs.isNotEmpty() && posterAdapter.itemCount == 0) {
                // Use the UserCatalog object for loading content as it has all necessary info
                viewModel.loadContentForCatalog(userCatalogs[0])
            }
        }

        // UPDATED: Observe the generic content list instead of specific ones like popularMovies
        viewModel.currentCatalogContent.observe(viewLifecycleOwner) { items ->
            posterAdapter.updateData(items)

            // If list refreshed, select first item or show empty state
            if (items.isNotEmpty()) {
                val firstItem = items[0]
                // Only auto-select if we don't have a user selection (optional logic)
                if (selectedMovie == null) {
                    updateHeaderUI(firstItem.name, firstItem.description ?: "No description available.", firstItem.poster)
                }
            } else {
                updateHeaderUI("No Movies", "Select a different catalog.", null)
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
        // Find the full UserCatalog object to pass to ViewModel
        val userCatalog = viewModel.movieCatalogs.value?.find { it.catalogId == catalog.id }

        if (userCatalog != null) {
            viewModel.loadContentForCatalog(userCatalog)
        }

        selectedMovie = null
        viewModel.clearStreams()
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