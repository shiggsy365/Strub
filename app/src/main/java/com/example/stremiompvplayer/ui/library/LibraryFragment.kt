package com.example.stremiompvplayer.ui.library

import android.content.Intent
import android.os.Bundle
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.stremiompvplayer.DetailsActivity2
import com.example.stremiompvplayer.R
import com.example.stremiompvplayer.data.ServiceLocator
import com.example.stremiompvplayer.databinding.FragmentLibraryNewBinding
import com.example.stremiompvplayer.models.MetaItem
import com.example.stremiompvplayer.utils.SharedPreferencesManager
import com.example.stremiompvplayer.viewmodels.MainViewModel
import com.example.stremiompvplayer.viewmodels.MainViewModelFactory
import com.example.stremiompvplayer.adapters.PosterAdapter
import java.text.SimpleDateFormat
import java.util.Locale

class LibraryFragment : Fragment() {

    private var _binding: FragmentLibraryNewBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MainViewModel by activityViewModels {
        MainViewModelFactory(
            ServiceLocator.getInstance(requireContext()),
            SharedPreferencesManager.getInstance(requireContext())
        )
    }

    private lateinit var contentAdapter: PosterAdapter
    private var currentSelectedItem: MetaItem? = null
    private var currentType = "movie" // "movie" or "series"

    // Sort state
    private var currentSortBy = "dateAdded" // "dateAdded", "releaseDate", "title"
    private var sortAscending = false

    companion object {
        private const val ARG_TYPE = "media_type"
        fun newInstance(type: String): LibraryFragment {
            val fragment = LibraryFragment()
            val args = Bundle()
            args.putString(ARG_TYPE, type)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentLibraryNewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        currentType = arguments?.getString(ARG_TYPE) ?: "movie"
        setupAdapters()
        setupObservers()
        setupFilterButtons()
        
        // Initial load
        applyFiltersAndSort()
    }

    override fun onResume() {
        super.onResume()
        currentSelectedItem?.let { updateDetailsPane(it) }
    }

    private fun setupAdapters() {
        contentAdapter = PosterAdapter(
            items = emptyList(),
            onClick = { item -> onContentClicked(item) },
            onLongClick = { item ->
                val pos = contentAdapter.getItemPosition(item)
                val holder = binding.rvContent.findViewHolderForAdapterPosition(pos)
                if (holder != null) showItemMenu(holder.itemView, item)
            }
        )

        binding.rvContent.layoutManager = GridLayoutManager(context, 10)
        binding.rvContent.adapter = contentAdapter

        binding.rvContent.addOnChildAttachStateChangeListener(object : RecyclerView.OnChildAttachStateChangeListener {
            override fun onChildViewAttachedToWindow(view: View) {
                view.setOnFocusChangeListener { v, hasFocus ->
                    if (hasFocus) {
                        val position = binding.rvContent.getChildAdapterPosition(v)
                        if (position != RecyclerView.NO_POSITION) {
                            val item = contentAdapter.getItem(position)
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

    private fun setupFilterButtons() {
        binding.btnSortAddedDate.setOnClickListener {
            if (currentSortBy == "dateAdded") {
                sortAscending = !sortAscending
            } else {
                currentSortBy = "dateAdded"
                sortAscending = false
            }
            updateSortButtonText()
            applyFiltersAndSort()
        }

        binding.btnSortReleaseDate.setOnClickListener {
            if (currentSortBy == "releaseDate") {
                sortAscending = !sortAscending
            } else {
                currentSortBy = "releaseDate"
                sortAscending = false
            }
            updateSortButtonText()
            applyFiltersAndSort()
        }

        binding.btnSortTitle.setOnClickListener {
            if (currentSortBy == "title") {
                sortAscending = !sortAscending
            } else {
                currentSortBy = "title"
                sortAscending = false
            }
            updateSortButtonText()
            applyFiltersAndSort()
        }

        binding.btnGenreAll.setOnClickListener {
            // Placeholder for genre filtering - not yet implemented
            applyFiltersAndSort()
        }
    }

    private fun updateSortButtonText() {
        val arrow = if (sortAscending) "↑" else "↓"
        
        binding.btnSortAddedDate.text = if (currentSortBy == "dateAdded") 
            "Sort: Date Added $arrow" else "Sort: Date Added"
        
        binding.btnSortReleaseDate.text = if (currentSortBy == "releaseDate")
            "Sort: Release Date $arrow" else "Sort: Release Date"
        
        binding.btnSortTitle.text = if (currentSortBy == "title")
            "Sort: Title $arrow" else "Sort: Title"
    }

    private fun applyFiltersAndSort() {
        viewModel.filterAndSortLibrary(currentType, null, currentSortBy, sortAscending)
    }

    private fun updateDetailsPane(item: MetaItem) {
        currentSelectedItem = item

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
        binding.detailTitle.visibility = View.VISIBLE
        binding.detailLogo.visibility = View.GONE

        viewModel.fetchItemLogo(item)
    }

    private fun showItemMenu(view: View, item: MetaItem) {
        val wrapper = ContextThemeWrapper(requireContext(), android.R.style.Theme_DeviceDefault_Light_NoActionBar)
        val popup = PopupMenu(wrapper, view)

        popup.menu.add("Remove from Library")
        popup.menu.add("Mark as Watched")
        popup.menu.add("Clear Watched Status")

        popup.setOnMenuItemClickListener { menuItem ->
            when (menuItem.title) {
                "Remove from Library" -> {
                    viewModel.removeFromLibrary(item.id)
                    true
                }
                "Mark as Watched" -> {
                    viewModel.markAsWatched(item)
                    item.isWatched = true
                    item.progress = item.duration
                    val position = contentAdapter.getItemPosition(item)
                    if (position != -1) {
                        contentAdapter.notifyItemChanged(position)
                    }
                    true
                }
                "Clear Watched Status" -> {
                    viewModel.clearWatchedStatus(item)
                    item.isWatched = false
                    item.progress = 0
                    val position = contentAdapter.getItemPosition(item)
                    if (position != -1) {
                        contentAdapter.notifyItemChanged(position)
                    }
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

    private fun onContentClicked(item: MetaItem) {
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
    }

    private fun setupObservers() {
        // Observe filtered library content
        val liveData = if (currentType == "movie") {
            viewModel.filteredLibraryMovies
        } else {
            viewModel.filteredLibrarySeries
        }

        liveData.observe(viewLifecycleOwner) { items ->
            contentAdapter.updateData(items)

            if (items.isEmpty()) {
                binding.emptyText.visibility = View.VISIBLE
                binding.rvContent.visibility = View.GONE
            } else {
                binding.emptyText.visibility = View.GONE
                binding.rvContent.visibility = View.VISIBLE
                if (items.isNotEmpty()) {
                    updateDetailsPane(items[0])
                }
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

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
