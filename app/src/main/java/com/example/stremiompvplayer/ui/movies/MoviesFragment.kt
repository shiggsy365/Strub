package com.example.stremiompvplayer.ui.movies

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
import com.example.stremiompvplayer.adapters.PosterAdapter
import com.example.stremiompvplayer.data.ServiceLocator
import com.example.stremiompvplayer.databinding.FragmentHomeBinding
import com.example.stremiompvplayer.models.MetaItem
import com.example.stremiompvplayer.ui.ResultsDisplayModule
import com.example.stremiompvplayer.utils.SharedPreferencesManager
import com.example.stremiompvplayer.viewmodels.HomeRow
import com.example.stremiompvplayer.viewmodels.MainViewModel
import com.example.stremiompvplayer.viewmodels.MainViewModelFactory
import com.example.stremiompvplayer.viewmodels.MoviesViewModel
import com.example.stremiompvplayer.viewmodels.MoviesViewModelFactory
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Fragment for the Movies page.
 * Displays movie rows based on the configuration from Settings.
 * Reuses the same layout as HomeFragment (fragment_home.xml).
 */
class MoviesFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MoviesViewModel by viewModels {
        MoviesViewModelFactory(
            ServiceLocator.getInstance(requireContext()),
            SharedPreferencesManager.getInstance(requireContext())
        )
    }

    // Access MainViewModel for shared logic (player, library actions)
    private val mainViewModel: MainViewModel by activityViewModels {
        MainViewModelFactory(
            ServiceLocator.getInstance(requireContext()),
            SharedPreferencesManager.getInstance(requireContext())
        )
    }

    private lateinit var rowAdapter: HomeRowAdapter
    private var heroUpdateJob: Job? = null

    // Helper module for dialogs (lazy initialized)
    private lateinit var displayModule: ResultsDisplayModule

    // Drill-down navigation state
    enum class DrillDownLevel { ROWS, GENRE_RESULTS, CAST_RESULTS }
    private var currentDrillDownLevel = DrillDownLevel.ROWS

    // Adapter for drill-down content (genre results/cast results)
    private var drillDownAdapter: PosterAdapter? = null

    // Store current movie rows for restoration
    private var cachedMovieRows: List<HomeRow> = emptyList()

    // Genre drill-down state
    private var currentGenreId: Int? = null
    private var currentGenreName: String? = null

    // Cast search results
    private var castMovieResults: List<MetaItem> = emptyList()
    private var currentCastPersonName: String? = null

    companion object {
        fun newInstance(): MoviesFragment {
            return MoviesFragment()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize display module for helper methods
        val config = ResultsDisplayModule.Configuration(
            pageBackground = binding.pageBackground,
            detailTitle = binding.detailTitle,
            detailLogo = binding.detailLogo,
            detailDescription = binding.detailDescription,
            detailDate = binding.detailDate,
            detailRating = binding.detailRating,
            detailEpisode = binding.detailEpisode,
            actorChips = binding.actorChips,
            btnPlay = null, btnTrailer = null, btnRelated = null
        )
        displayModule = ResultsDisplayModule(this, mainViewModel, config)

        setupRecyclerView()
        setupObservers()

        // Initial load
        viewModel.loadMovieContent()
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
            "movie" -> {
                // DIRECT PLAY: Launch PlayerActivity with MetaItem only.
                val intent = android.content.Intent(requireContext(), PlayerActivity::class.java).apply {
                    putExtra("meta", item)
                }
                startActivity(intent)
            }
            "genre" -> {
                // Drill down into genre to show movies in that genre
                item.genreId?.let { genreId ->
                    currentDrillDownLevel = DrillDownLevel.GENRE_RESULTS
                    currentGenreId = genreId
                    currentGenreName = item.name
                    loadGenreMovies(genreId, item.name)
                }
            }
            else -> {
                // Fallback for other types
                displayModule.showStreamDialog(item)
            }
        }
    }

    private fun loadGenreMovies(genreId: Int, genreName: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            binding.progressBar.visibility = View.VISIBLE
            try {
                val movies = viewModel.fetchMoviesByGenre(genreId)
                showDrillDownContent(movies, "$genreName Movies")
            } catch (e: Exception) {
                // Handle error
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    private fun showLongPressMenu(item: MetaItem) {
        val view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_content_options, null)
        val dialog = android.app.AlertDialog.Builder(requireContext(), android.R.style.Theme_DeviceDefault_Dialog_NoActionBar)
            .setView(view)
            .create()
        
        // Make dialog background transparent to show the card background
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val titleView = view.findViewById<TextView>(R.id.menuTitle)
        titleView.text = item.name

        // --- Actions ---

        // 1. Scrape Streams (Explicit)
        view.findViewById<View>(R.id.actionScrape).setOnClickListener {
            dialog.dismiss()
            displayModule.showStreamDialog(item)
        }

        // 2. Watch Trailer - Uses external YouTube player
        view.findViewById<View>(R.id.actionTrailer).setOnClickListener {
            dialog.dismiss()
            displayModule.playTrailer(item)
        }

        // 3. Watchlist Toggle - Uses toggleWatchlist for proper add/remove behavior
        val actionWatchlist = view.findViewById<TextView>(R.id.actionWatchlist)
        // Default text - actual toggle behavior will add if not present, remove if present
        actionWatchlist.text = "Toggle Watchlist"
        actionWatchlist.setOnClickListener {
            dialog.dismiss()
            mainViewModel.toggleWatchlist(item)
        }

        // 4. Library Toggle - Shows proper toggle state
        val actionLibrary = view.findViewById<TextView>(R.id.actionLibrary)
        lifecycleScope.launch {
            val inLib = mainViewModel.isItemInLibrarySync(item.id)
            actionLibrary.text = if (inLib) "Remove from Library" else "Add to Library"
        }
        actionLibrary.setOnClickListener {
            dialog.dismiss()
            mainViewModel.toggleLibrary(item)
        }

        // 5. Watched Toggle - Shows proper toggle state
        val actionWatched = view.findViewById<TextView>(R.id.actionWatched)
        actionWatched.text = if (item.isWatched) "Mark Unwatched" else "Mark Watched"
        actionWatched.setOnClickListener {
            dialog.dismiss()
            if (item.isWatched) mainViewModel.clearWatchedStatus(item) else mainViewModel.markAsWatched(item)
        }

        // 6. Not Watching - Complete removal from all lists
        view.findViewById<View>(R.id.actionNotWatching).setOnClickListener {
            dialog.dismiss()
            mainViewModel.markAsNotWatching(item)
        }

        // --- Cast Section ---
        val castGroup = view.findViewById<ChipGroup>(R.id.castChipGroup)

        val castObserver = androidx.lifecycle.Observer<List<MetaItem>> { castList ->
            if (dialog.isShowing) {
                castGroup.removeAllViews()
                castList.take(10).forEach { actor ->
                    val chip = Chip(requireContext())
                    chip.text = actor.name
                    chip.setOnClickListener {
                        dialog.dismiss()
                        val personId = actor.id.removePrefix("tmdb:").toIntOrNull()
                        if (personId != null) {
                            currentCastPersonName = actor.name
                            mainViewModel.loadPersonCredits(personId)
                        }
                    }
                    castGroup.addView(chip)
                }
            }
        }

        mainViewModel.castList.observe(viewLifecycleOwner, castObserver)
        mainViewModel.fetchCast(item.id, item.type)

        dialog.setOnDismissListener {
            mainViewModel.castList.removeObserver(castObserver)
        }

        dialog.show()
        dialog.window?.apply {
            val width = (resources.displayMetrics.widthPixels * 0.8).toInt()
            setLayout(width, android.view.WindowManager.LayoutParams.WRAP_CONTENT)
            setGravity(android.view.Gravity.CENTER)
        }
    }

    private fun updateHeroBanner(item: MetaItem) {
        heroUpdateJob?.cancel()
        heroUpdateJob = viewLifecycleOwner.lifecycleScope.launch {
            delay(200)
            if (!isAdded) return@launch

            Glide.with(this@MoviesFragment)
                .load(item.background ?: item.poster)
                .into(binding.pageBackground)

            binding.detailTitle.text = item.name
            binding.detailTitle.visibility = View.VISIBLE
            binding.detailLogo.visibility = View.GONE

            mainViewModel.fetchItemLogo(item)

            binding.detailDescription.text = item.description ?: "No description available."

            binding.detailRating.visibility = if (item.rating != null) View.VISIBLE else View.GONE
            binding.detailRating.text = "★ ${item.rating}"

            val dateText = try {
                item.releaseDate?.take(4) ?: ""
            } catch (e: Exception) { "" }
            binding.detailDate.text = dateText

            binding.detailEpisode.visibility = View.GONE

            binding.actorChips.removeAllViews()
        }
    }

    private fun setupObservers() {
        viewModel.movieRows.observe(viewLifecycleOwner) { rows ->
            cachedMovieRows = rows
            if (currentDrillDownLevel == DrillDownLevel.ROWS) {
                rowAdapter.updateData(rows, this::showLongPressMenu)

                if (rows.isNotEmpty() && rows[0].items.isNotEmpty()) {
                    updateHeroBanner(rows[0].items[0])
                }
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
                binding.detailTitle.visibility = View.VISIBLE
                binding.detailLogo.visibility = View.GONE
            }
        }

        // Observer for person credits (cast search results)
        mainViewModel.searchResults.observe(viewLifecycleOwner) { results ->
            if (currentCastPersonName != null && results.isNotEmpty()) {
                // Filter only movies for the Movies page
                castMovieResults = results.filter { it.type == "movie" }

                if (castMovieResults.isNotEmpty()) {
                    currentDrillDownLevel = DrillDownLevel.CAST_RESULTS
                    showDrillDownContent(
                        castMovieResults,
                        "$currentCastPersonName - Movies (${castMovieResults.size})"
                    )
                }
            }
        }
    }

    fun focusSidebar() {
        binding.rvHomeRows.post {
            binding.rvHomeRows.requestFocus()
        }
    }

    /**
     * Shows drill-down content (genre results or cast results) in place of movie rows
     */
    private fun showDrillDownContent(items: List<MetaItem>, label: String) {
        if (items.isEmpty()) return

        if (drillDownAdapter == null) {
            drillDownAdapter = PosterAdapter(
                items = items,
                onClick = { item -> handleSinglePress(item) },
                onLongClick = { item -> showLongPressMenu(item) }
            )
        } else {
            drillDownAdapter?.updateData(items)
        }

        binding.rvHomeRows.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        binding.rvHomeRows.adapter = drillDownAdapter

        if (items.isNotEmpty()) {
            updateHeroBanner(items[0])
        }

        binding.rvHomeRows.setOnHierarchyChangeListener(object : ViewGroup.OnHierarchyChangeListener {
            override fun onChildViewAdded(parent: View?, child: View?) {
                child?.setOnFocusChangeListener { v, hasFocus ->
                    if (hasFocus) {
                        val pos = binding.rvHomeRows.getChildAdapterPosition(v)
                        if (pos != androidx.recyclerview.widget.RecyclerView.NO_POSITION) {
                            val item = drillDownAdapter?.getItem(pos)
                            if (item != null) {
                                heroUpdateJob?.cancel()
                                heroUpdateJob = viewLifecycleOwner.lifecycleScope.launch {
                                    delay(200)
                                    if (isAdded) {
                                        updateHeroBanner(item)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            override fun onChildViewRemoved(parent: View?, child: View?) {
                child?.onFocusChangeListener = null
            }
        })

        binding.root.postDelayed({
            binding.rvHomeRows.scrollToPosition(0)
            binding.rvHomeRows.layoutManager?.findViewByPosition(0)?.requestFocus()
        }, 100)
    }

    /**
     * Restores the movie rows view from drill-down state
     */
    private fun restoreMovieRowsView() {
        binding.rvHomeRows.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        binding.rvHomeRows.adapter = rowAdapter

        if (cachedMovieRows.isNotEmpty()) {
            rowAdapter.updateData(cachedMovieRows, this::showLongPressMenu)
            if (cachedMovieRows[0].items.isNotEmpty()) {
                updateHeroBanner(cachedMovieRows[0].items[0])
            }
        }

        binding.root.postDelayed({
            binding.rvHomeRows.scrollToPosition(0)
            binding.rvHomeRows.requestFocus()
        }, 100)
    }

    /**
     * Handles back button press for drill-down navigation
     * Returns true if handled, false otherwise
     */
    fun handleBackPress(): Boolean {
        when (currentDrillDownLevel) {
            DrillDownLevel.GENRE_RESULTS, DrillDownLevel.CAST_RESULTS -> {
                currentDrillDownLevel = DrillDownLevel.ROWS
                currentGenreId = null
                currentGenreName = null
                currentCastPersonName = null
                castMovieResults = emptyList()
                restoreMovieRowsView()
                return true
            }
            DrillDownLevel.ROWS -> {
                return false
            }
        }
    }

    /**
     * Handles key events for navigation
     */
    fun handleKeyDown(keyCode: Int, event: android.view.KeyEvent?): Boolean {
        // No special key handling needed for Movies page currently
        return false
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        heroUpdateJob?.cancel()
    }
}
