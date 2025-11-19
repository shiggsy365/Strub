package com.example.stremiompvplayer.ui.search

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.GridLayoutManager
import com.example.stremiompvplayer.DetailsActivity2
import com.example.stremiompvplayer.databinding.FragmentSearchBinding
import com.example.stremiompvplayer.adapters.PosterAdapter
import com.example.stremiompvplayer.data.ServiceLocator
import com.example.stremiompvplayer.models.MetaItem
import com.example.stremiompvplayer.utils.SharedPreferencesManager
import com.example.stremiompvplayer.viewmodels.MainViewModel
import com.example.stremiompvplayer.viewmodels.MainViewModelFactory

class SearchFragment : Fragment() {

    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MainViewModel by activityViewModels {
        MainViewModelFactory(
            ServiceLocator.getInstance(requireContext()),
            SharedPreferencesManager.getInstance(requireContext())
        )
    }

    private lateinit var searchAdapter: PosterAdapter
    private var currentSearchQuery: String = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupSearchView()
        setupObservers()
        setupFilterChips()
    }

    private fun setupRecyclerView() {
        searchAdapter = PosterAdapter(
            items = emptyList(),
            onClick = { item -> onItemClick(item) }
        )
        
        binding.searchRecyclerView.apply {
            layoutManager = GridLayoutManager(context, 3) // 3 columns for posters
            adapter = searchAdapter
        }
    }

    private fun setupSearchView() {
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.let {
                    if (it.isNotBlank()) {
                        currentSearchQuery = it
                        performSearch(it)
                    }
                }
                // Hide keyboard
                binding.searchView.clearFocus()
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                // Optional: Implement real-time search with debouncing
                if (newText.isNullOrBlank()) {
                    showEmptyState()
                }
                return true
            }
        })
    }

    private fun setupFilterChips() {
        // Set default selection to "All"
        binding.chipAll.isChecked = true

        binding.chipAll.setOnClickListener {
            if (currentSearchQuery.isNotBlank()) {
                performSearch(currentSearchQuery, SearchType.ALL)
            }
        }

        binding.chipMovies.setOnClickListener {
            if (currentSearchQuery.isNotBlank()) {
                performSearch(currentSearchQuery, SearchType.MOVIES)
            }
        }

        binding.chipSeries.setOnClickListener {
            if (currentSearchQuery.isNotBlank()) {
                performSearch(currentSearchQuery, SearchType.SERIES)
            }
        }
    }

    private fun setupObservers() {
        // Observe search results
        viewModel.searchResults.observe(viewLifecycleOwner) { results ->
            if (results.isEmpty()) {
                showNoResults()
            } else {
                showResults(results)
            }
        }

        // Observe loading state
        viewModel.isSearching.observe(viewLifecycleOwner) { isSearching ->
            binding.loadingProgress.visibility = if (isSearching) View.VISIBLE else View.GONE
        }

        // Observe errors
        viewModel.error.observe(viewLifecycleOwner) { error ->
            if (error != null) {
                Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun performSearch(query: String, type: SearchType = SearchType.ALL) {
        Log.d("SearchFragment", "Searching for: $query (type: $type)")
        
        when (type) {
            SearchType.ALL -> viewModel.searchTMDB(query)
            SearchType.MOVIES -> viewModel.searchMovies(query)
            SearchType.SERIES -> viewModel.searchSeries(query)
        }

        // Hide empty state, show loading
        binding.emptyText.visibility = View.GONE
        binding.searchRecyclerView.visibility = View.GONE
    }

    private fun showResults(results: List<MetaItem>) {
        Log.d("SearchFragment", "Showing ${results.size} results")
        
        binding.emptyText.visibility = View.GONE
        binding.searchRecyclerView.visibility = View.VISIBLE
        searchAdapter.updateData(results)
        
        // Update result count
        binding.resultCount.visibility = View.VISIBLE
        binding.resultCount.text = "${results.size} results found"
    }

    private fun showNoResults() {
        binding.searchRecyclerView.visibility = View.GONE
        binding.resultCount.visibility = View.GONE
        binding.emptyText.visibility = View.VISIBLE
        binding.emptyText.text = "No results found for \"$currentSearchQuery\""
    }

    private fun showEmptyState() {
        binding.searchRecyclerView.visibility = View.GONE
        binding.resultCount.visibility = View.GONE
        binding.emptyText.visibility = View.VISIBLE
        binding.emptyText.text = "Enter a search term to find content"
        viewModel.clearSearchResults()
    }

    private fun onItemClick(item: MetaItem) {
        Log.d("SearchFragment", "Clicked: ${item.name} (${item.type})")
        
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

    private enum class SearchType {
        ALL, MOVIES, SERIES
    }
}
