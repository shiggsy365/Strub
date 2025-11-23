package com.example.stremiompvplayer.ui.home

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.stremiompvplayer.DetailsActivity2
import com.example.stremiompvplayer.R
import com.example.stremiompvplayer.adapters.PosterAdapter
import com.example.stremiompvplayer.data.ServiceLocator
import com.example.stremiompvplayer.databinding.FragmentHomeBinding
import com.example.stremiompvplayer.databinding.ItemHomeSectionBinding
import com.example.stremiompvplayer.models.MetaItem
import com.example.stremiompvplayer.utils.SharedPreferencesManager
import com.example.stremiompvplayer.viewmodels.MainViewModel
import com.example.stremiompvplayer.viewmodels.MainViewModelFactory

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MainViewModel by activityViewModels {
        MainViewModelFactory(
            ServiceLocator.getInstance(requireContext()),
            SharedPreferencesManager.getInstance(requireContext())
        )
    }

    private lateinit var nextUpAdapter: PosterAdapter
    private lateinit var continueEpAdapter: PosterAdapter
    private lateinit var continueMovAdapter: PosterAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupSection(binding.sectionNextUp, "Episodes - Next Up", "episode")
        setupSection(binding.sectionContinueEpisodes, "Episodes - Continue Watching", "episode")
        setupSection(binding.sectionContinueMovies, "Movies - Continue Watching", "movie")

        setupObservers()

        // Trigger load
        viewModel.loadHomeContent()
    }

    private fun setupSection(sectionBinding: ItemHomeSectionBinding, title: String, type: String) {
        sectionBinding.tvSectionTitle.text = title

        val adapter = PosterAdapter(
            items = emptyList(),
            onClick = { item -> onContentClicked(item) },
            onLongClick = null // simplified for home
        )

        when(title) {
            "Episodes - Next Up" -> nextUpAdapter = adapter
            "Episodes - Continue Watching" -> continueEpAdapter = adapter
            "Movies - Continue Watching" -> continueMovAdapter = adapter
        }

        val layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        sectionBinding.rvSectionContent.layoutManager = layoutManager
        sectionBinding.rvSectionContent.adapter = adapter

        // Snap to center/start to make sidecar updates cleaner
        val snapHelper = LinearSnapHelper()
        snapHelper.attachToRecyclerView(sectionBinding.rvSectionContent)

        // Scroll Listener to update Sidecar
        sectionBinding.rvSectionContent.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    val centerView = snapHelper.findSnapView(layoutManager)
                    if (centerView != null) {
                        val pos = layoutManager.getPosition(centerView)
                        val item = adapter.getItem(pos)
                        if (item != null) {
                            updateSidecar(sectionBinding, item)
                        }
                    }
                }
            }
        })
    }

    private fun updateSidecar(binding: ItemHomeSectionBinding, item: MetaItem) {
        // Title
        binding.tvSidecarTitle.text = item.name
        binding.tvSidecarTitle.visibility = View.VISIBLE
        binding.imgSidecarLogo.visibility = View.GONE // Default to text

        // Logo (Fetch async from VM logic or re-use if cached in item)
        // Note: item.poster is usually the poster image.
        // Sidecar asks for "Media Logo". The VM has a fetchItemLogo method but it targets a single LiveData.
        // For Home screen sidecars, fetching logos for every scroll might be heavy.
        // For now, let's use the name. If you really need logos, we need a better caching strategy.
        // However, we CAN try to fetch it if we want.
        // Let's stick to Text Title for performance unless logo is pre-loaded.

        // Rating
        if (item.rating != null) {
            binding.tvSidecarRating.text = "★ ${item.rating}"
            binding.tvSidecarRating.visibility = View.VISIBLE
        } else {
            binding.tvSidecarRating.visibility = View.GONE
        }

        // Episode Info
        if (item.type == "episode") {
            val parts = item.id.split(":")
            if (parts.size >= 4) {
                binding.tvSidecarEpisode.text = "S${parts[2]} E${parts[3]}"
            } else {
                // Fallback parsing from name "Show - 1x05"
                binding.tvSidecarEpisode.text = "Episode"
            }
            binding.tvSidecarEpisode.visibility = View.VISIBLE
        } else {
            binding.tvSidecarEpisode.visibility = View.GONE
        }

        // Description
        binding.tvSidecarDesc.text = item.description ?: "No description."
    }

    private fun setupObservers() {
        viewModel.homeNextUp.observe(viewLifecycleOwner) { items ->
            nextUpAdapter.updateData(items)
            if (items.isNotEmpty()) updateSidecar(binding.sectionNextUp, items[0])
            else binding.sectionNextUp.root.visibility = View.GONE
        }

        viewModel.homeContinueEpisodes.observe(viewLifecycleOwner) { items ->
            continueEpAdapter.updateData(items)
            if (items.isNotEmpty()) updateSidecar(binding.sectionContinueEpisodes, items[0])
            else binding.sectionContinueEpisodes.root.visibility = View.GONE
        }

        viewModel.homeContinueMovies.observe(viewLifecycleOwner) { items ->
            continueMovAdapter.updateData(items)
            if (items.isNotEmpty()) updateSidecar(binding.sectionContinueMovies, items[0])
            else binding.sectionContinueMovies.root.visibility = View.GONE
        }
    }

    private fun onContentClicked(item: MetaItem) {
        val intent = Intent(requireContext(), DetailsActivity2::class.java).apply {
            putExtra("metaId", item.id)
            putExtra("title", item.name)
            putExtra("poster", item.poster)
            putExtra("background", item.background)
            putExtra("description", item.description)
            putExtra("type", item.type)
        }
        startActivity(intent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}