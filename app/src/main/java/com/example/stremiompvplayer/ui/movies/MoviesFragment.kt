package com.example.stremiompvplayer.ui.movies

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.stremiompvplayer.DetailsActivity2
import com.example.stremiompvplayer.data.ServiceLocator
import com.example.stremiompvplayer.models.Catalog
import com.example.stremiompvplayer.databinding.FragmentDiscoverBinding
import com.example.stremiompvplayer.models.MetaItem
import com.example.stremiompvplayer.models.FeedList
import com.example.stremiompvplayer.ui.discover.DiscoverSectionAdapter
import com.example.stremiompvplayer.viewmodels.CatalogViewModel
import com.example.stremiompvplayer.viewmodels.CatalogUiState
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import android.util.Log

class MoviesFragment : Fragment() {

    private var _binding: FragmentDiscoverBinding? = null
    private val binding get() = _binding!!

    // NEW: Inject the CatalogViewModel using the factory
    private val viewModel: CatalogViewModel by viewModels {
        ServiceLocator.provideCatalogViewModelFactory()
    }

    // NEW: Adapter to display the content posters (MetaItems)
    private lateinit var contentAdapter: DiscoverSectionAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Assuming you have separate layout IDs for the RecyclerView and ChipGroup in this fragment
        _binding = FragmentDiscoverBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize the RecyclerView adapter
        contentAdapter = DiscoverSectionAdapter { metaItem ->
            onPosterClick(metaItem)
        }

        binding.discoverRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = contentAdapter
        }

        // Start observing data flow
        observeViewModel()
    }

    // MoviesFragment.kt

// ... (existing code and imports) ...

    private fun observeViewModel() {
        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is CatalogUiState.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.discoverRecyclerView.visibility = View.GONE
                }
                is CatalogUiState.Error -> {
                    binding.progressBar.visibility = View.GONE
                    Log.e("MoviesFragment", "Error loading catalog: ${state.message}")
                    Toast.makeText(context, "Error: ${state.message}", Toast.LENGTH_LONG).show()
                }
                is CatalogUiState.Success -> {
                    binding.progressBar.visibility = View.GONE
                    binding.discoverRecyclerView.visibility = View.VISIBLE

                    // 1. Setup the UI for the Movie Catalogs
                    setupCatalogChips(state.catalogs)

                    // --- FIX: CONVERT FLAT LIST OF MetaItem TO LIST OF FeedList ---
                    val movieCatalogs = state.catalogs.filter { it.type == "movie" }
                    val items = state.items // List<MetaItem>

                    // Create a single FeedList object (section) to wrap the movies
                    val movieSection = FeedList(
                        id = "movie_page",
                        userId = "", // Placeholder
                        name = "Movies",
                        catalogUrl = "",
                        type = "movie",
                        catalogId = movieCatalogs.firstOrNull()?.id ?: "default_movie_catalog",
                        orderIndex = 0
                    ).apply {
                        content = items // Inject the List<MetaItem> into the content property
                    }

                    // 2. Display the Items (Submitting List<FeedList>)
                    // This resolves the Type Mismatch error.
                    contentAdapter.submitList(listOf(movieSection))

                    // --- END FIX ---
                }
            }
        }
    }

    private fun setupCatalogChips(allCatalogs: List<Catalog>) {
        val movieCatalogs = allCatalogs.filter { it.type == "movie" }

        if (movieCatalogs.isEmpty()) {
            Log.w("MoviesFragment", "No movie catalogs were received.")
        } else {
            // Automatically fetch the content of the FIRST movie catalog
            val firstCatalog = movieCatalogs.first()

            // NOTE: The ViewModel fetchCatalog is usually called ONCE in init or when a chip is clicked.
            // Since this function is called inside the Success branch of the observer,
            // you should check if the currently displayed data matches the requested catalog
            // to avoid infinite loops or unnecessary network calls.

            // For now, we assume the initial call in the ViewModel's init block worked,
            // and we do NOT call fetchCatalog again here to avoid a loop.
            // We just ensure the right posters are displayed.

            // REMOVED: viewModel.fetchCatalog(firstCatalog.type, firstCatalog.id)
        }
    }

// ... (rest of the class)

    private fun onPosterClick(metaItem: MetaItem) {
        val intent = Intent(activity, DetailsActivity2::class.java).apply {
            putExtra("id", metaItem.id)
            putExtra("type", metaItem.type)
        }
        startActivity(intent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}