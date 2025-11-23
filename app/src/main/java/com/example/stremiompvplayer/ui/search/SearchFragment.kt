package com.example.stremiompvplayer.ui.search

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.ContextThemeWrapper

import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.PopupMenu
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.bumptech.glide.Glide
import com.example.stremiompvplayer.DetailsActivity2
import com.example.stremiompvplayer.R
import com.example.stremiompvplayer.UserSelectionActivity
import com.example.stremiompvplayer.adapters.PosterAdapter
import com.example.stremiompvplayer.data.ServiceLocator
import com.example.stremiompvplayer.databinding.FragmentSearchNewBinding
import com.example.stremiompvplayer.models.MetaItem
import com.example.stremiompvplayer.utils.SharedPreferencesManager
import com.example.stremiompvplayer.viewmodels.MainViewModel
import com.example.stremiompvplayer.viewmodels.MainViewModelFactory
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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

    private fun updateItemUI(item: MetaItem, isWatched: Boolean) {
        item.isWatched = isWatched
        item.progress = if (isWatched) item.duration else 0

        val position = contentAdapter.getItemPosition(item)
        if (position != -1) {
            contentAdapter.notifyItemChanged(position)
        }
    }

    private lateinit var contentAdapter: PosterAdapter
    private lateinit var searchAdapter: PosterAdapter
    private var currentSelectedItem: MetaItem? = null

    private var detailsUpdateJob: Job? = null

    private enum class SearchType { MIXED, MOVIES, SERIES }
    private var currentSearchType = SearchType.MIXED

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSearchNewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupObservers()
        binding.chipMixed.isChecked = true
        binding.searchEditText.requestFocus()
    }

    private fun setupRecyclerView() {
        searchAdapter = PosterAdapter(
            items = emptyList(),
            onClick = { item ->
                updateDetailsPane(item)
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

        binding.resultsRecycler.addOnChildAttachStateChangeListener(object : androidx.recyclerview.widget.RecyclerView.OnChildAttachStateChangeListener {
            override fun onChildViewAttachedToWindow(view: View) {
                view.setOnFocusChangeListener { v, hasFocus ->
                    if (hasFocus) {
                        val position = binding.resultsRecycler.getChildAdapterPosition(v)
                        if (position != androidx.recyclerview.widget.RecyclerView.NO_POSITION) {
                            val item = searchAdapter.getItem(position)
                            if (item != null) {
                                detailsUpdateJob?.cancel()
                                detailsUpdateJob = viewLifecycleOwner.lifecycleScope.launch {
                                    delay(1000)
                                    if (isAdded) {
                                        updateDetailsPane(item)
                                    }
                                }
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
        binding.heroCard.visibility = View.VISIBLE

        Glide.with(this)
            .load(item.background ?: item.poster)
            .into(binding.pageBackground)

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

        binding.detailTitle.text = item.name
        // [CHANGE] Initial state hidden
        binding.detailTitle.visibility = View.GONE
        binding.detailLogo.visibility = View.GONE

        viewModel.fetchItemLogo(item)
    }

    private fun showItemMenu(view: View, item: MetaItem) {
        val wrapper = ContextThemeWrapper(requireContext(),
            android.R.style.Theme_DeviceDefault_Light_NoActionBar)
        val popup = PopupMenu(wrapper, view)

        // NEW: Check if item is in library
        viewModel.checkLibraryStatus(item.id)

        val isInLibrary = viewModel.isItemInLibrary.value ?: false

        if (isInLibrary) {
            popup.menu.add("Remove from Library")
        } else {
            popup.menu.add("Add to Library")
        }

        popup.menu.add("Mark as Watched")
        popup.menu.add("Clear Watched Status")

        popup.setOnMenuItemClickListener { menuItem ->
            when (menuItem.title) {
                "Add to Library" -> {
                    viewModel.addToLibrary(item)
                    true
                }
                "Remove from Library" -> {
                    viewModel.removeFromLibrary(item.id)
                    true
                }
                "Mark as Watched" -> {
                    viewModel.markAsWatched(item)
                    item.isWatched = true
                    item.progress = item.duration
                    refreshItem(item)
                    true
                }
                "Clear Watched Status" -> {
                    viewModel.clearWatchedStatus(item)
                    item.isWatched = false
                    item.progress = 0
                    refreshItem(item)
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

    private fun refreshItem(item: MetaItem) {
        val position = contentAdapter.getItemPosition(item)
        if (position != -1) {
            contentAdapter.notifyItemChanged(position)
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
                if (results.isNotEmpty()) updateDetailsPane(results[0])
            }
            binding.resultsRecycler.requestFocus()
        }
        viewModel.isSearching.observe(viewLifecycleOwner) { isSearching ->
            binding.progressBar.visibility = if (isSearching) View.VISIBLE else View.GONE
            binding.loadingCard.visibility = if (isSearching) View.VISIBLE else View.GONE
            if (isSearching) binding.noResultsState.visibility = View.GONE
        }
        viewModel.actionResult.observe(viewLifecycleOwner) { result ->
            when (result) {
                is MainViewModel.ActionResult.Success -> {
                    Toast.makeText(requireContext(), result.message,
                        Toast.LENGTH_SHORT).show()
                }
                is MainViewModel.ActionResult.Error -> {
                    Toast.makeText(requireContext(), result.message,
                        Toast.LENGTH_LONG).show()
                }
            }
        }

        // [CHANGE] Updated observer
        viewModel.currentLogo.observe(viewLifecycleOwner) { logoUrl ->
            when (logoUrl) {
                "" -> {
                    binding.detailTitle.visibility = View.GONE
                    binding.detailLogo.visibility = View.GONE
                }
                null -> {
                    binding.detailTitle.visibility = View.VISIBLE
                    binding.detailLogo.visibility = View.GONE
                }
                else -> {
                    binding.detailTitle.visibility = View.GONE
                    binding.detailLogo.visibility = View.VISIBLE
                    Glide.with(this).load(logoUrl).fitCenter().into(binding.detailLogo)
                }
            }
        }
    }

    fun setSearchText(text: String) { binding.searchEditText.setText(text) }
    fun searchByPersonId(id: Int) { binding.chipMixed.isChecked = true; viewModel.loadPersonCredits(id) }
    private fun hideKeyboard() { (requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).hideSoftInputFromWindow(binding.searchEditText.windowToken, 0) }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        viewModel.clearSearchResults()
        detailsUpdateJob?.cancel()
    }

    fun focusSearch() { binding.searchEditText.requestFocus() }
}