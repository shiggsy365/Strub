package com.example.stremiompvplayer.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.stremiompvplayer.PlayerActivity
import com.example.stremiompvplayer.R
import com.example.stremiompvplayer.adapters.HomeRowAdapter
import com.example.stremiompvplayer.data.ServiceLocator
import com.example.stremiompvplayer.databinding.FragmentHomeBinding
import com.example.stremiompvplayer.models.MetaItem
import com.example.stremiompvplayer.ui.ResultsDisplayModule
import com.example.stremiompvplayer.utils.SharedPreferencesManager
import com.example.stremiompvplayer.viewmodels.HomeViewModel
import com.example.stremiompvplayer.viewmodels.HomeViewModelFactory
import com.example.stremiompvplayer.viewmodels.MainViewModel
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by viewModels {
        HomeViewModelFactory(
            ServiceLocator.getInstance(requireContext()),
            SharedPreferencesManager.getInstance(requireContext())
        )
    }

    // Access MainViewModel for shared logic (player, library actions)
    private val mainViewModel: MainViewModel by activityViewModels()

    private lateinit var rowAdapter: HomeRowAdapter
    private var heroUpdateJob: Job? = null

    // Helper module for dialogs (lazy initialized)
    private lateinit var displayModule: ResultsDisplayModule

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

        // Initialize display module for helper methods
        // Note: We pass null/dummies for views because we are handling the Hero banner manually in this fragment
        val config = ResultsDisplayModule.Configuration(
            pageBackground = binding.pageBackground,
            detailTitle = binding.detailTitle,
            detailLogo = binding.detailLogo,
            detailDescription = binding.detailDescription,
            detailDate = binding.detailDate,
            detailRating = binding.detailRating,
            detailEpisode = binding.detailTitle, // Placeholder
            actorChips = binding.actorChips,
            btnPlay = null, btnTrailer = null, btnRelated = null
        )
        displayModule = ResultsDisplayModule(this, mainViewModel, config)

        setupRecyclerView()
        setupObservers()

        // Initial load
        viewModel.loadHomeContent()
    }

    private fun setupRecyclerView() {
        rowAdapter = HomeRowAdapter(
            onContentClick = { item ->
                handleSinglePress(item)
            },
            onContentFocused = { item ->
                updateHeroBanner(item)
            }
        )

        binding.rvHomeRows.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
            adapter = rowAdapter
            setItemViewCacheSize(10)
        }
    }

    private fun handleSinglePress(item: MetaItem) {
        when (item.type) {
            "episode", "movie" -> {
                // DIRECT PLAY: Launch PlayerActivity with MetaItem only.
                // PlayerActivity will handle scraping and auto-play.
                val intent = android.content.Intent(requireContext(), PlayerActivity::class.java).apply {
                    putExtra("meta", item)
                }
                startActivity(intent)
            }
            "series" -> {
                // Drill down into series
                val discoverFragment = com.example.stremiompvplayer.ui.discover.DiscoverFragment.newInstance("series", item.id)
                (activity as? com.example.stremiompvplayer.MainActivity)?.loadFragment(discoverFragment)
            }
            else -> {
                // Fallback for other types (e.g. Person?)
                displayModule.showStreamDialog(item)
            }
        }
    }

    private fun showLongPressMenu(item: MetaItem) {
        val dialog = BottomSheetDialog(requireContext())
        val view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_content_options, null)
        dialog.setContentView(view)

        // We only set the title here since the complex description pane was NOT added to the dialog XML
        val titleView = view.findViewById<TextView>(R.id.menuTitle)
        titleView.text = item.name

        // --- Actions ---

        // 1. Scrape Streams (Explicit)
        view.findViewById<View>(R.id.actionScrape).setOnClickListener {
            dialog.dismiss()
            displayModule.showStreamDialog(item)
        }

        // 2. Watch Trailer
        view.findViewById<View>(R.id.actionTrailer).setOnClickListener {
            dialog.dismiss()
            displayModule.playTrailer(item)
        }

        // 3. Watchlist Toggle
        val actionWatchlist = view.findViewById<TextView>(R.id.actionWatchlist)
        actionWatchlist.setOnClickListener {
            dialog.dismiss()
            mainViewModel.toggleWatchlist(item)
        }

        // 4. Library Toggle
        val actionLibrary = view.findViewById<TextView>(R.id.actionLibrary)
        lifecycleScope.launch {
            val inLib = mainViewModel.isItemInLibrarySync(item.id)
            actionLibrary.text = if (inLib) "Remove from Library" else "Add to Library"
        }
        actionLibrary.setOnClickListener {
            dialog.dismiss()
            mainViewModel.toggleLibrary(item)
        }

        // 5. Watched Toggle
        val actionWatched = view.findViewById<TextView>(R.id.actionWatched)
        actionWatched.text = if (item.isWatched) "Mark as Not Watched" else "Mark as Watched"
        actionWatched.setOnClickListener {
            dialog.dismiss()
            if (item.isWatched) mainViewModel.clearWatchedStatus(item) else mainViewModel.markAsWatched(item)
        }

        // 6. Not Watching
        view.findViewById<View>(R.id.actionNotWatching).setOnClickListener {
            dialog.dismiss()
            mainViewModel.markAsNotWatching(item)
        }

        // --- Cast Section ---
        // Fetch cast and populate chips
        val castGroup = view.findViewById<ChipGroup>(R.id.castChipGroup)

        // Create a temporary observer for this dialog
        val castObserver = androidx.lifecycle.Observer<List<MetaItem>> { castList ->
            if (dialog.isShowing) {
                castGroup.removeAllViews()
                castList.take(10).forEach { actor ->
                    val chip = Chip(requireContext())
                    chip.text = actor.name
                    chip.setOnClickListener {
                        dialog.dismiss()
                        // Trigger actor search
                        val personId = actor.id.removePrefix("tmdb:").toIntOrNull()
                        if (personId != null) {
                            mainViewModel.loadPersonCredits(personId)
                            val intent = android.content.Intent(requireContext(), com.example.stremiompvplayer.MainActivity::class.java).apply {
                                putExtra("SEARCH_PERSON_ID", personId)
                                putExtra("SEARCH_QUERY", actor.name)
                            }
                            startActivity(intent)
                        }
                    }
                    castGroup.addView(chip)
                }
            }
        }

        mainViewModel.castList.observe(viewLifecycleOwner, castObserver)
        // Trigger fetch
        mainViewModel.fetchCast(item.id, item.type)

        dialog.setOnDismissListener {
            mainViewModel.castList.removeObserver(castObserver)
        }

        dialog.show()
    }

    private fun updateHeroBanner(item: MetaItem) {
        heroUpdateJob?.cancel()
        heroUpdateJob = viewLifecycleOwner.lifecycleScope.launch {
            delay(200)
            if (!isAdded) return@launch

            Glide.with(this@HomeFragment)
                .load(item.background ?: item.poster)
                .into(binding.pageBackground)

            // --- Logo Handling ---
            // We check if we have a cached logo URL in MainViewModel, or trigger a fetch.
            // Since MainViewModel handles one logo at a time, fast scrolling might get tricky.
            // For now, we'll use text title and try to fetch logo.

            binding.detailTitle.text = item.name
            binding.detailTitle.visibility = View.VISIBLE
            binding.detailLogo.visibility = View.GONE

            // Trigger logo fetch (updates mainViewModel.currentLogo)
            mainViewModel.fetchItemLogo(item)

            binding.detailDescription.text = item.description ?: "No description available."

            binding.detailRating.visibility = if (item.rating != null) View.VISIBLE else View.GONE
            binding.detailRating.text = "★ ${item.rating}"

            // Format date
            val dateText = try {
                item.releaseDate?.take(4) ?: ""
            } catch (e: Exception) { "" }
            binding.detailDate.text = dateText

            // Episode Info
            if (item.type == "episode") {
                binding.detailEpisode.visibility = View.VISIBLE
                // Try to parse SxxExx from ID or Name if not explicit
                if (item.id.split(":").size >= 4) {
                    val parts = item.id.split(":")
                    binding.detailEpisode.text = "S${parts[2]}E${parts[3]}"
                } else {
                    binding.detailEpisode.text = "Episode"
                }
            } else {
                binding.detailEpisode.visibility = View.GONE
            }

            // --- Cast Chips in Hero ---
            // Clear existing
            binding.actorChips.removeAllViews()
            // We won't trigger a network call for cast on every scroll to avoid rate limiting.
            // Only update if we already have data or maybe for the very first item.
        }
    }

    private fun setupObservers() {
        viewModel.homeRows.observe(viewLifecycleOwner) { rows ->
            rowAdapter.updateData(rows, this::showLongPressMenu)

            if (rows.isNotEmpty() && rows[0].items.isNotEmpty()) {
                updateHeroBanner(rows[0].items[0])
            }
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        // Observe Logo updates for the Hero Banner
        mainViewModel.currentLogo.observe(viewLifecycleOwner) { logoUrl ->
            if (!logoUrl.isNullOrEmpty()) {
                binding.detailTitle.visibility = View.GONE
                binding.detailLogo.visibility = View.VISIBLE
                Glide.with(this)
                    .load(logoUrl)
                    .fitCenter()
                    .into(binding.detailLogo)
            } else {
                // Keep title visible if no logo found
                binding.detailTitle.visibility = View.VISIBLE
                binding.detailLogo.visibility = View.GONE
            }
        }
    }

    fun focusSidebar() {
        binding.rvHomeRows.post {
            binding.rvHomeRows.requestFocus()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        heroUpdateJob?.cancel()
    }
}