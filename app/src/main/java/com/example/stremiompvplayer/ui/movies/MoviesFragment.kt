package com.example.stremiompvplayer.ui.movies

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.stremiompvplayer.DetailsActivity2
import com.example.stremiompvplayer.data.ServiceLocator
import com.example.stremiompvplayer.databinding.FragmentMoviesBinding
import com.example.stremiompvplayer.databinding.MetaDetailPaneBinding // NEW IMPORT
import com.example.stremiompvplayer.models.Catalog
import com.example.stremiompvplayer.models.FeedList
import com.example.stremiompvplayer.models.MetaItem
import com.example.stremiompvplayer.adapters.CatalogChipAdapter // NEW IMPORT
import com.example.stremiompvplayer.ui.discover.DiscoverSectionAdapter
import com.example.stremiompvplayer.viewmodels.CatalogViewModel
import com.example.stremiompvplayer.viewmodels.CatalogUiState
import android.util.Log

class MoviesFragment : Fragment() {

    // 1. BINDING AND VIEW MODEL SETUP
    private var _binding: FragmentMoviesBinding? = null
    private val binding get() = _binding!!

    // NEW: Binding for the details pane (will hold title, description, image)
    private lateinit var detailsPaneBinding: MetaDetailPaneBinding

    private val viewModel: CatalogViewModel by viewModels {
        ServiceLocator.provideCatalogViewModelFactory(requireContext())
    }

    private lateinit var contentAdapter: DiscoverSectionAdapter
    private lateinit var catalogChipAdapter: CatalogChipAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // 2. USE THE NEW DEDICATED LAYOUT FILE
        _binding = FragmentMoviesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 3. Setup and Inject the Details Pane
        val layoutInflater = LayoutInflater.from(requireContext())
        detailsPaneBinding = MetaDetailPaneBinding.inflate(layoutInflater)
        binding.detailsPane.addView(detailsPaneBinding.root)

        // 4. Initialize Adapters
        contentAdapter = DiscoverSectionAdapter { metaItem ->
            onPosterClick(metaItem)
        }

        catalogChipAdapter = CatalogChipAdapter { catalog ->
            // When a chip is clicked, fetch the content for that catalog
            viewModel.fetchCatalog(catalog.type, catalog.id)
        }

        // 5. Setup RecyclerViews
        binding.catalogChipsRecycler.apply {
            layoutManager = LinearLayoutManager(context, RecyclerView.HORIZONTAL, false)
            adapter = catalogChipAdapter
        }

        binding.moviesGridRecycler.apply {
            // Setup for 7-wide grid display to get narrow posters
            layoutManager = GridLayoutManager(context, 7)
            adapter = contentAdapter
        }

        // Start observing data flow
        observeViewModel()
    }

    private fun observeViewModel() {
        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is CatalogUiState.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                    // Clear previous content visual indicators
                    binding.moviesGridRecycler.visibility = View.GONE
                }
                is CatalogUiState.Error -> {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(context, "Error: ${state.message}", Toast.LENGTH_LONG).show()
                }
                is CatalogUiState.Success -> {
                    binding.progressBar.visibility = View.GONE

                    // 1. Filter and set data for the horizontal chip list
                    val movieCatalogs = state.catalogs.filter { it.type == "movie" }
                    setupCatalogChips(movieCatalogs)

                    // 2. Display the posters, wrapped in a FeedList section
                    val movieSection = FeedList(
                        id = "movie_page",
                        userId = "",
                        name = "Movies",
                        catalogUrl = "",
                        type = "movie",
                        catalogId = movieCatalogs.firstOrNull()?.id ?: "default_movie_catalog",
                        orderIndex = 0
                    ).apply {
                        content = state.items // List<MetaItem>
                    }

                    // Submission to the DiscoverSectionAdapter (List<FeedList>)
                    contentAdapter.submitList(listOf(movieSection))
                    binding.moviesGridRecycler.visibility = View.VISIBLE

                    // 3. Auto-select and display details of the first item
                    state.items.firstOrNull()?.let { firstItem ->
                        updateDetailsPane(firstItem)
                    }
                }
            }
        }
    }

    private fun setupCatalogChips(movieCatalogs: List<Catalog>) {
        if (movieCatalogs.isEmpty()) {
            Log.w("MoviesFragment", "No movie catalogs were received.")
        } else {
            // Update the Chip Adapter with the list of movie catalogs
            catalogChipAdapter.setCatalogs(movieCatalogs)
        }
    }

    // NEW: Function to update the details pane content
    private fun updateDetailsPane(metaItem: MetaItem) {
        detailsPaneBinding.detailTitle.text = metaItem.name
        detailsPaneBinding.detailDescription.text = metaItem.description

        // Use Glide to load the poster into the detail image view
        context?.let {
            Glide.with(it)
                .load(metaItem.poster)
                .centerCrop()
                .into(detailsPaneBinding.detailImage)
        }

        // NOTE: Stream dropdown population logic (fetchStreams) would go here.
    }

    private fun onPosterClick(metaItem: MetaItem) {
        // Update the detail pane when a poster in the grid is clicked
        updateDetailsPane(metaItem)

        // Navigate or load details streams
        /*
        val intent = Intent(activity, DetailsActivity2::class.java).apply {
            putExtra("id", metaItem.id)
            putExtra("type", metaItem.type)
        }
        startActivity(intent)
        */
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}