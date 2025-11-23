package com.example.stremiompvplayer.ui.movies

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ContextThemeWrapper
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.GridLayoutManager
import com.bumptech.glide.Glide
import com.example.stremiompvplayer.DetailsActivity2
import com.example.stremiompvplayer.R
import com.example.stremiompvplayer.adapters.PosterAdapter
import com.example.stremiompvplayer.data.ServiceLocator
import com.example.stremiompvplayer.databinding.FragmentMoviesBinding
import com.example.stremiompvplayer.models.MetaItem
import com.example.stremiompvplayer.utils.SharedPreferencesManager
import com.example.stremiompvplayer.viewmodels.MainViewModel
import com.example.stremiompvplayer.viewmodels.MainViewModelFactory

class MoviesFragment : Fragment() {

    private var _binding: FragmentMoviesBinding? = null
    private val binding get() = _binding!!

    private lateinit var contentAdapter: PosterAdapter

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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentMoviesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adapter = LibraryAdapter(
            onClick = { item ->
                val intent = Intent(requireContext(), DetailsActivity2::class.java).apply {
                    putExtra("metaId", item.id)
                    putExtra("title", item.name)
                    putExtra("poster", item.poster)
                    putExtra("background", item.background)
                    putExtra("description", item.description)
                    putExtra("type", "movie")
                }
                startActivity(intent)
            },
            onFocus = { item -> updateDetails(item) }
        )

        binding.rvContent.layoutManager = GridLayoutManager(context, 5)
        binding.rvContent.adapter = adapter

        viewModel.libraryMovies.observe(viewLifecycleOwner) { items ->
            adapter.submitList(items)
            if (items.isNotEmpty()) {
                updateDetails(items[0])
            }
        }
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

        popup.menu.add("Add to TMDB Watchlist")
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
                "Add to TMDB Watchlist" -> {
                    viewModel.toggleWatchlist(item, force = true)
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
    private fun updateDetails(item: MetaItem) {
        binding.detailTitle.text = item.name
        binding.detailDescription.text = item.description ?: "No description available."
        Glide.with(this).load(item.background ?: item.poster).into(binding.backgroundImage)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    inner class LibraryAdapter(
        private val onClick: (MetaItem) -> Unit,
        private val onFocus: (MetaItem) -> Unit
    ) : androidx.recyclerview.widget.RecyclerView.Adapter<LibraryAdapter.ViewHolder>() {
        private var items = listOf<MetaItem>()
        inner class ViewHolder(val view: View) : androidx.recyclerview.widget.RecyclerView.ViewHolder(view) {
            val poster: android.widget.ImageView = view.findViewById(R.id.poster)
        }
        fun submitList(newItems: List<MetaItem>) {
            items = newItems
            notifyDataSetChanged()
        }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_poster, parent, false)
            return ViewHolder(view)
        }
        override fun getItemCount() = items.size
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            holder.poster.scaleType = android.widget.ImageView.ScaleType.CENTER_CROP
            Glide.with(holder.view).load(item.poster).placeholder(R.drawable.movie).into(holder.poster)
            holder.view.setOnClickListener { onClick(item) }
            holder.view.setOnFocusChangeListener { _, hasFocus -> if (hasFocus) onFocus(item) }
            holder.view.isFocusable = true
            holder.view.isFocusableInTouchMode = true
        }
    }
}