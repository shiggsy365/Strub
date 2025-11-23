package com.example.stremiompvplayer.ui.home

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.stremiompvplayer.DetailsActivity2
import com.example.stremiompvplayer.R
import com.example.stremiompvplayer.adapters.PosterAdapter
import com.example.stremiompvplayer.data.ServiceLocator
import com.example.stremiompvplayer.databinding.FragmentHomeBinding
import com.example.stremiompvplayer.models.MetaItem
import com.example.stremiompvplayer.utils.SharedPreferencesManager
import com.example.stremiompvplayer.viewmodels.MainViewModel
import com.example.stremiompvplayer.viewmodels.MainViewModelFactory

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var nextUpAdapter: PosterAdapter
    private lateinit var continueEpisodesAdapter: PosterAdapter
    private lateinit var continueMoviesAdapter: PosterAdapter

    private val viewModel: MainViewModel by activityViewModels {
        MainViewModelFactory(
            ServiceLocator.getInstance(requireContext()),
            SharedPreferencesManager.getInstance(requireContext())
        )
    }

    // Track currently focused items for each section
    private var currentNextUpItem: MetaItem? = null
    private var currentContinueEpisodeItem: MetaItem? = null
    private var currentContinueMovieItem: MetaItem? = null

    companion object {
        fun newInstance(): HomeFragment {
            return HomeFragment()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupNextUpSection()
        setupContinueEpisodesSection()
        setupContinueMoviesSection()
        setupObservers()

        // Load home content
        viewModel.loadHomeContent()
    }

    private fun setupNextUpSection() {
        nextUpAdapter = PosterAdapter(
            items = emptyList(),
            onClick = { item -> openDetails(item) }
        )

        binding.rvNextUp.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = nextUpAdapter
            setHasFixedSize(true)
        }
    }

    private fun setupContinueEpisodesSection() {
        continueEpisodesAdapter = PosterAdapter(
            items = emptyList(),
            onClick = { item -> openDetails(item) }
        )

        binding.rvContinueEpisodes.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = continueEpisodesAdapter
            setHasFixedSize(true)
        }
    }

    private fun setupContinueMoviesSection() {
        continueMoviesAdapter = PosterAdapter(
            items = emptyList(),
            onClick = { item -> openDetails(item) }
        )

        binding.rvContinueMovies.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = continueMoviesAdapter
            setHasFixedSize(true)
        }
    }

    private fun setupObservers() {
        viewModel.homeNextUp.observe(viewLifecycleOwner) { items ->
            nextUpAdapter.updateData(items)
            if (items.isNotEmpty() && currentNextUpItem == null) {
                updateNextUpSidecar(items[0])
            }
        }

        viewModel.homeContinueEpisodes.observe(viewLifecycleOwner) { items ->
            continueEpisodesAdapter.updateData(items)
            if (items.isNotEmpty() && currentContinueEpisodeItem == null) {
                updateContinueEpisodesSidecar(items[0])
            }
        }

        viewModel.homeContinueMovies.observe(viewLifecycleOwner) { items ->
            continueMoviesAdapter.updateData(items)
            if (items.isNotEmpty() && currentContinueMovieItem == null) {
                updateContinueMoviesSidecar(items[0])
            }
        }

        viewModel.currentLogo.observe(viewLifecycleOwner) { logoUrl ->
            // Update logos for the currently focused sections
            currentNextUpItem?.let { item ->
                if (logoUrl != null && logoUrl.isNotEmpty()) {
                    binding.nextUpLogo.visibility = View.VISIBLE
                    binding.nextUpTitle.visibility = View.GONE
                    Glide.with(this).load(logoUrl).into(binding.nextUpLogo)
                } else {
                    binding.nextUpLogo.visibility = View.GONE
                    binding.nextUpTitle.visibility = View.VISIBLE
                }
            }

            currentContinueEpisodeItem?.let { item ->
                if (logoUrl != null && logoUrl.isNotEmpty()) {
                    binding.continueEpisodesLogo.visibility = View.VISIBLE
                    binding.continueEpisodesTitle.visibility = View.GONE
                    Glide.with(this).load(logoUrl).into(binding.continueEpisodesLogo)
                } else {
                    binding.continueEpisodesLogo.visibility = View.GONE
                    binding.continueEpisodesTitle.visibility = View.VISIBLE
                }
            }

            currentContinueMovieItem?.let { item ->
                if (logoUrl != null && logoUrl.isNotEmpty()) {
                    binding.continueMoviesLogo.visibility = View.VISIBLE
                    binding.continueMoviesTitle.visibility = View.GONE
                    Glide.with(this).load(logoUrl).into(binding.continueMoviesLogo)
                } else {
                    binding.continueMoviesLogo.visibility = View.GONE
                    binding.continueMoviesTitle.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun updateNextUpSidecar(item: MetaItem) {
        // Update title/logo
        binding.nextUpTitle.text = item.name

        // Extract episode info if available
        if (item.type == "episode") {
            val parts = item.id.split(":")
            if (parts.size >= 4) {
                val season = parts[2]
                val episode = parts[3]
                binding.nextUpEpisode.text = "S${season.padStart(2, '0')}E${episode.padStart(2, '0')}"
                binding.nextUpEpisode.visibility = View.VISIBLE
            }
        } else {
            binding.nextUpEpisode.visibility = View.GONE
        }

        // Rating - would need to fetch from TMDB if not in MetaItem
        binding.nextUpRating.visibility = View.GONE

        // Fetch logo
        viewModel.fetchItemLogo(item)

        // Update background
        Glide.with(this)
            .load(item.background ?: item.poster)
            .into(binding.pageBackground)
    }

    private fun updateContinueEpisodesSidecar(item: MetaItem) {
        // Update title/logo
        binding.continueEpisodesTitle.text = item.name

        // Extract episode info if available
        if (item.type == "episode") {
            val parts = item.id.split(":")
            if (parts.size >= 4) {
                val season = parts[2]
                val episode = parts[3]
                binding.continueEpisodesEpisode.text = "S${season.padStart(2, '0')}E${episode.padStart(2, '0')}"
                binding.continueEpisodesEpisode.visibility = View.VISIBLE
            }
        } else {
            binding.continueEpisodesEpisode.visibility = View.GONE
        }

        // Rating
        binding.continueEpisodesRating.visibility = View.GONE

        // Fetch logo
        viewModel.fetchItemLogo(item)

        // Update background
        Glide.with(this)
            .load(item.background ?: item.poster)
            .into(binding.pageBackground)
    }

    private fun updateContinueMoviesSidecar(item: MetaItem) {
        // Update title/logo
        binding.continueMoviesTitle.text = item.name

        // Rating
        binding.continueMoviesRating.visibility = View.GONE

        // Fetch logo
        viewModel.fetchItemLogo(item)

        // Update background
        Glide.with(this)
            .load(item.background ?: item.poster)
            .into(binding.pageBackground)
    }

    private fun openDetails(item: MetaItem) {
        val type = when {
            item.type == "episode" -> {
                // Extract show ID and navigate to series details
                val parts = item.id.split(":")
                if (parts.size >= 2) "series" else item.type
            }
            else -> item.type
        }

        val intent = Intent(requireContext(), DetailsActivity2::class.java).apply {
            putExtra("metaId", item.id)
            putExtra("title", item.name)
            putExtra("poster", item.poster)
            putExtra("background", item.background)
            putExtra("description", item.description)
            putExtra("type", type)
        }
        startActivity(intent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
