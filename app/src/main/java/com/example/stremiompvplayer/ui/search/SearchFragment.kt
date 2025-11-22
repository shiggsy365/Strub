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
import android.widget.PopupMenu
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.GridLayoutManager
import com.bumptech.glide.Glide
import com.example.stremiompvplayer.DetailsActivity2
import com.example.stremiompvplayer.R
import com.example.stremiompvplayer.adapters.PosterAdapter
import com.example.stremiompvplayer.data.ServiceLocator
import com.example.stremiompvplayer.databinding.FragmentSearchNewBinding
import com.example.stremiompvplayer.models.MetaItem
import com.example.stremiompvplayer.utils.SharedPreferencesManager
import com.example.stremiompvplayer.viewmodels.MainViewModel
import com.example.stremiompvplayer.viewmodels.MainViewModelFactory
import java.text.SimpleDateFormat
import java.util.Locale

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
    private var currentSelectedItem: MetaItem? = null

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
            },
            onLongClick = { item ->
                val pos = searchAdapter.getItemPosition(item)
                val holder = binding.resultsRecycler.findViewHolderForAdapterPosition(pos)
                if (holder != null) showItemMenu(holder.itemView, item)
            }
        )
        binding.resultsRecycler.apply {
            layoutManager = GridLayoutManager(context, 10)
            adapter = searchAdapter
        }

        // Add focus listener for details pane
        binding.resultsRecycler.addOnChildAttachStateChangeListener(object : androidx.recyclerview.widget.RecyclerView.OnChildAttachStateChangeListener {
            override fun onChildViewAttachedToWindow(view: View) {
                view.setOnFocusChangeListener { v, hasFocus ->
                    if (hasFocus) {
                        val position = binding.resultsRecycler.getChildAdapterPosition(v)
                        if (position != androidx.recyclerview.widget.RecyclerView.NO_POSITION) {
                            val item = searchAdapter.getItem(position)
                            if (item != null) {
                                updateDetailsPane(item)
                            }
                        }
                    }
                }
            }

            override fun onChildViewDetachedFromWindow(view: View) {
                view.setOnFocusChangeListener(null)
            }
        })
    }

    private fun updateDetailsPane(item: MetaItem) {
        currentSelectedItem = item

        // Show hero card
        binding.heroCard.visibility = View.VISIBLE

        // Update background
        Glide.with(this)
            .load(item.background ?: item.poster)
            .into(binding.pageBackground)

        // Update metadata
        val formattedDate = try {
            item.releaseDate?.let { dateStr ->
                val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val outputFormat = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())
                val date = inputFormat.parse(dateStr)
                date?.let { outputFormat.format(it) }
            }
        } catch (e: Exception) {
            item.releaseDate
        }

        binding.detailDate.text = formattedDate ?: ""
        binding.detailRating.visibility = if (item.rating != null) {
            binding.detailRating.text = "★ ${item.rating}"
            View.VISIBLE
        } else {
            View.GONE
        }

        binding.detailDescription.text = item.description ?: "No description available."

        // Update title/logo
        binding.detailTitle.text = item.name
        binding.detailTitle.visibility = View.VISIBLE
        binding.detailLogo.visibility = View.GONE

        // Fetch logo if possible
        viewModel.fetchItemLogo(item)
    }

    private fun showItemMenu(view: View, item: MetaItem) {
        val wrapper = android.view.ContextThemeWrapper(requireContext(), android.R.style.Theme_DeviceDefault_Light_NoActionBar)
        val popup = PopupMenu(wrapper, view)

        popup.menu.add("Add to Library")
        popup.menu.add("Add to TMDB Watchlist")
        popup.menu.add("Mark as Watched")
        popup.menu.add("Clear Watched Status")

        popup.setOnMenuItemClickListener { menuItem ->
            when (menuItem.title) {
                "Add to Library" -> {
                    viewModel.addToLibrary(item)
                    true
                }
                "Add to TMDB Watchlist" -> {
                    viewModel.toggleWatchlist(item, force = true)
                    true
                }
                "Mark as Watched" -> {
                    viewModel.markAsWatched(item)
                    item.isWatched = true
                    item.progress = item.duration
                    val position = searchAdapter.getItemPosition(item)
                    if (position != -1) {
                        searchAdapter.notifyItemChanged(position)
                    }
                    true
                }
                "Clear Watched Status" -> {
                    viewModel.clearWatchedStatus(item)
                    item.isWatched = false
                    item.progress = 0
                    val position = searchAdapter.getItemPosition(item)
                    if (position != -1) {
                        searchAdapter.notifyItemChanged(position)
                    }
                    true
                }
                else -> false
            }
        }
        popup.show()
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
            searchAdapter.updateData(results)

            // Update UI visibility
            if (results.isEmpty()) {
                binding.emptyState.visibility = View.GONE
                binding.noResultsState.visibility = View.VISIBLE
                binding.contentGrid.visibility = View.GONE
                binding.heroCard.visibility = View.GONE
            } else {
                binding.emptyState.visibility = View.GONE
                binding.noResultsState.visibility = View.GONE
                binding.contentGrid.visibility = View.VISIBLE
                binding.heroCard.visibility = View.VISIBLE
                
                // Show first result in hero card
                if (results.isNotEmpty()) {
                    updateDetailsPane(results[0])
                }
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

        viewModel.currentLogo.observe(viewLifecycleOwner) { logoUrl ->
            if (logoUrl != null) {
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

        viewModel.error.observe(viewLifecycleOwner) { error ->
            // Optionally show error
        }
    }

    fun setSearchText(text: String) {
        binding.searchEditText.setText(text)
    }

    fun searchByPersonId(id: Int) {
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
