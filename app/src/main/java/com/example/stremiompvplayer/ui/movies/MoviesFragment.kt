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
import com.example.stremiompvplayer.adapters.PosterAdapter
import com.example.stremiompvplayer.adapters.StreamAdapter
import com.example.stremiompvplayer.data.ServiceLocator
import com.example.stremiompvplayer.databinding.FragmentMoviesBinding
import com.example.stremiompvplayer.models.MetaItem
import com.example.stremiompvplayer.PlayerActivity
import com.example.stremiompvplayer.viewmodels.MainViewModel
import com.example.stremiompvplayer.viewmodels.MainViewModelFactory

class MoviesFragment : Fragment() {

    private var _binding: FragmentMoviesBinding? = null
    private val binding get() = _binding!!

    // Update ViewModel initialization to use Factory
    private val viewModel: MainViewModel by activityViewModels {
        MainViewModelFactory(ServiceLocator.getInstance(requireContext()))
    }

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

        // Load movies based on user settings (User Lists)
        viewModel.loadUserEnabledCatalogs("movie")
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
        // Observe combined list of movies from enabled catalogs
        viewModel.catalogs.observe(viewLifecycleOwner) { items ->
            posterAdapter.submitList(items)
            
            // Optional: Select first item by default if nothing selected
            if (selectedMovie == null && items.isNotEmpty()) {
                // onPosterItemClicked(items[0]) // Uncomment if auto-selection is desired
            }
        }

        viewModel.streams.observe(viewLifecycleOwner) { streams ->
            streamAdapter.submitList(streams)
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
        selectedMovie = item
        
        // Update Header UI
        binding.tvTitle.text = item.name
        binding.tvDescription.text = item.description ?: "No description available."
        
        if (!item.poster.isNullOrEmpty()) {
            Glide.with(this).load(item.poster).into(binding.imgBackground)
        }

        // Load streams for the selected movie
        viewModel.loadStreams("movie", item.id)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
