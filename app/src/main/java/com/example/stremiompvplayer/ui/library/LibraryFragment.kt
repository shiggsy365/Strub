package com.example.stremiompvplayer.ui.library

import android.content.Intent
import android.os.Bundle
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.stremiompvplayer.DetailsActivity2
import com.example.stremiompvplayer.R
import com.example.stremiompvplayer.UserSelectionActivity
import com.example.stremiompvplayer.data.ServiceLocator
import com.example.stremiompvplayer.databinding.FragmentLibraryNewBinding
import com.example.stremiompvplayer.models.MetaItem
import com.example.stremiompvplayer.utils.SharedPreferencesManager
import com.example.stremiompvplayer.viewmodels.MainViewModel
import com.example.stremiompvplayer.viewmodels.MainViewModelFactory
import com.example.stremiompvplayer.adapters.PosterAdapter
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
    private lateinit var sidebarAdapter: LibrarySidebarAdapter
    private var currentSelectedItem: MetaItem? = null
    private var currentType = "movie"
    private var currentSortBy = "dateAdded"
    private var sortAscending = false

    private var detailsUpdateJob: Job? = null

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
        setupSidebar()
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

    private fun setupSidebar() {
        sidebarAdapter = LibrarySidebarAdapter { item ->
            handleFilterClick(item)
        }
        binding.rvLibrarySidebar.layoutManager = LinearLayoutManager(context)
        binding.rvLibrarySidebar.adapter = sidebarAdapter
        updateSidebarItems()
    }

    private fun updateSidebarItems() {
        val arrow = if (sortAscending) "↑" else "↓"
        val items = listOf(
            LibraryFilterOption("dateAdded", "Sort: Date Added" + if (currentSortBy == "dateAdded") " $arrow" else "", currentSortBy == "dateAdded"),
            LibraryFilterOption("releaseDate", "Sort: Release Date" + if (currentSortBy == "releaseDate") " $arrow" else "", currentSortBy == "releaseDate"),
            LibraryFilterOption("title", "Sort: Title" + if (currentSortBy == "title") " $arrow" else "", currentSortBy == "title"),
            LibraryFilterOption("genre", "All Genres", false)
        )
        sidebarAdapter.submitList(items)
    }

    private fun handleFilterClick(item: LibraryFilterOption) {
        if (item.id == "genre") return

        if (currentSortBy == item.id) {
            sortAscending = !sortAscending
        } else {
            currentSortBy = item.id
            sortAscending = false
        }
        updateSidebarItems()
        applyFiltersAndSort()
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
        // [CHANGE] Initial state hidden
        binding.detailTitle.visibility = View.GONE
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
        viewModel.libraryMovies.observe(viewLifecycleOwner) {}
        viewModel.librarySeries.observe(viewLifecycleOwner) {}
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
                    Glide.with(this)
                        .load(logoUrl)
                        .fitCenter()
                        .into(binding.detailLogo)
                }
            }
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        detailsUpdateJob?.cancel()
    }

    data class LibraryFilterOption(val id: String, val label: String, val isSelected: Boolean)

    inner class LibrarySidebarAdapter(private val onClick: (LibraryFilterOption) -> Unit) :
        androidx.recyclerview.widget.ListAdapter<LibraryFilterOption, LibrarySidebarAdapter.ViewHolder>(DiffCallback()) {

        inner class ViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
            val name: TextView = view.findViewById(R.id.catalogName)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_discover_sidebar, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = getItem(position)
            holder.name.text = item.label
            holder.view.isSelected = item.isSelected
            holder.view.setOnClickListener { onClick(item) }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<LibraryFilterOption>() {
        override fun areItemsTheSame(oldItem: LibraryFilterOption, newItem: LibraryFilterOption) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: LibraryFilterOption, newItem: LibraryFilterOption) = oldItem == newItem
    }
}