package com.example.stremiompvplayer.ui.search

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.GridLayoutManager
import com.example.stremiompvplayer.DetailsActivity2
import com.example.stremiompvplayer.R
import com.example.stremiompvplayer.adapters.PosterAdapter
import com.example.stremiompvplayer.data.ServiceLocator
import com.example.stremiompvplayer.databinding.FragmentSearchNewBinding
import com.example.stremiompvplayer.models.MetaItem
import com.example.stremiompvplayer.utils.SharedPreferencesManager
import com.example.stremiompvplayer.viewmodels.MainViewModel
import com.example.stremiompvplayer.viewmodels.MainViewModelFactory

class SearchFragment : Fragment() {

    private var _binding: FragmentSearchNewBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MainViewModel by activityViewModels {
        MainViewModelFactory(
            ServiceLocator.getInstance(requireContext()),
            SharedPreferencesManager.getInstance(requireContext())
        )
    }

    private lateinit var searchAdapter: PosterAdapter

    private enum class SearchType {
        MIXED, MOVIES, SERIES
    }

    private var currentSearchType = SearchType.MIXED

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSearchNewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupUI()
        setupObservers()

        // Default chip selection
        binding.chipMixed.isChecked = true
        binding.searchEditText.requestFocus()
    }

    private fun setupRecyclerView() {
        searchAdapter = PosterAdapter(
            items = emptyList(),
            onClick = { item ->
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
        )
        binding.resultsRecycler.apply {
            layoutManager = GridLayoutManager(context, 10)
            adapter = searchAdapter
        }
    }

    private fun setupUI() {
        binding.searchButton.setOnClickListener {
            performSearch(binding.searchEditText.text.toString())
            hideKeyboard()
        }

        binding.searchEditText.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)) {
                performSearch(binding.searchEditText.text.toString())
                hideKeyboard()
                true
            } else {
                false
            }
        }

        // Chip Logic
        binding.chipGroupSearchType.setOnCheckedChangeListener { group, checkedId ->
            if (checkedId == View.NO_ID) return@setOnCheckedChangeListener

            currentSearchType = when(checkedId) {
                R.id.chipMixed -> SearchType.MIXED
                R.id.chipMovies -> SearchType.MOVIES
                R.id.chipSeries -> SearchType.SERIES
                else -> SearchType.MIXED
            }

            // Re-trigger search if there is text
            val query = binding.searchEditText.text.toString()
            if (query.isNotBlank()) {
                performSearch(query)
            }
        }
    }

    private fun performSearch(query: String) {
        if (query.isBlank()) return

        when (currentSearchType) {
            SearchType.MIXED -> viewModel.searchTMDB(query)
            SearchType.MOVIES -> viewModel.searchMovies(query)
            SearchType.SERIES -> viewModel.searchSeries(query)
        }
    }

    private fun setupObservers() {
        viewModel.searchResults.observe(viewLifecycleOwner) { results ->
            // Supply simplified adapter update
            searchAdapter.updateData(results)

            // Update UI visibility
            if (results.isEmpty()) {
                binding.emptyState.visibility = View.GONE
                binding.noResultsState.visibility = View.VISIBLE
                binding.resultsRecycler.visibility = View.GONE
            } else {
                binding.emptyState.visibility = View.GONE
                binding.noResultsState.visibility = View.GONE
                binding.resultsRecycler.visibility = View.VISIBLE
            }

            binding.resultsRecycler.requestFocus()
        }

        viewModel.isSearching.observe(viewLifecycleOwner) { isSearching ->
            binding.progressBar.visibility = if (isSearching) View.VISIBLE else View.GONE
            binding.loadingCard.visibility = if (isSearching) View.VISIBLE else View.GONE

            if (isSearching) {
                binding.noResultsState.visibility = View.GONE
            }
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            // Optionally show error
        }
    }

    fun setSearchText(text: String) {
        binding.searchEditText.setText(text)
    }

    // NEW: Trigger Person ID search
    fun searchByPersonId(id: Int) {
        // Switch to Mixed or create a new logic, sticking to Mixed for general display
        binding.chipMixed.isChecked = true
        viewModel.loadPersonCredits(id)
    }

    private fun hideKeyboard() {
        val inputMethodManager = requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(binding.searchEditText.windowToken, 0)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        viewModel.clearSearchResults()
    }

    fun focusSearch() {
        binding.searchEditText.requestFocus()
    }
}
