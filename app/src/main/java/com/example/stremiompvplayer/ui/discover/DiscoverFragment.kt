package com.example.stremiompvplayer.ui.discover

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.stremiompvplayer.DetailsActivity2
import com.example.stremiompvplayer.R
import com.example.stremiompvplayer.data.ServiceLocator
import com.example.stremiompvplayer.databinding.FragmentDiscoverBinding
import com.example.stremiompvplayer.models.MetaItem
import com.example.stremiompvplayer.models.UserCatalog
import com.example.stremiompvplayer.viewmodels.MainViewModel
import com.example.stremiompvplayer.viewmodels.MainViewModelFactory

class DiscoverFragment : Fragment() {

    fun handleBackPress(): Boolean {
        // Return true if you handle the back press internally (e.g., closing a menu)
        // Return false to let the Activity handle it (exit app)
        return true}
    private var _binding: FragmentDiscoverBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MainViewModel by activityViewModels {
        MainViewModelFactory(
            ServiceLocator.getInstance(requireContext()),
            com.example.stremiompvplayer.utils.SharedPreferencesManager.getInstance(requireContext())
        )
    }

    private lateinit var sidebarAdapter: SidebarAdapter
    private lateinit var contentAdapter: DiscoverContentAdapter
    private var currentType = "movie"

    companion object {
        private const val ARG_TYPE = "media_type"
        fun newInstance(type: String): DiscoverFragment {
            val fragment = DiscoverFragment()
            val args = Bundle()
            args.putString(ARG_TYPE, type)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentDiscoverBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        currentType = arguments?.getString(ARG_TYPE) ?: "movie"
        setupAdapters()
        setupObservers()
        loadCatalogs()
    }

    private fun setupAdapters() {
        sidebarAdapter = SidebarAdapter { catalog -> viewModel.loadContentForCatalog(catalog) }
        binding.rvSidebar.layoutManager = LinearLayoutManager(context)
        binding.rvSidebar.adapter = sidebarAdapter

        contentAdapter = DiscoverContentAdapter(
            items = emptyList(),
            onClick = { item -> onContentClicked(item) },
            onLongClick = { view, item -> showItemMenu(view, item) },
            onFocus = { item -> updateDetailsPane(item) }
        )

        // CHANGED: 10 Columns Wide
        binding.rvContent.layoutManager = GridLayoutManager(context, 10)
        binding.rvContent.adapter = contentAdapter
    }

    private fun updateDetailsPane(item: MetaItem) {
        Glide.with(this).load(item.background ?: item.poster).into(binding.pageBackground)

        // BIND DATA: Release Date & Rating
        binding.detailDate.text = item.releaseDate ?: ""
        if (item.rating != null) {
            binding.detailRating.text = "★ ${item.rating}"
            binding.detailRating.visibility = View.VISIBLE
        } else {
            binding.detailRating.visibility = View.GONE
        }

        binding.detailDescription.text = item.description ?: "No description available."

        binding.detailTitle.text = item.name
        binding.detailTitle.visibility = View.VISIBLE
        binding.detailLogo.visibility = View.GONE

        viewModel.fetchItemLogo(item)
    }

    // ... (setupObservers, loadCatalogs, etc remain the same) ...

    inner class DiscoverContentAdapter(
        private var items: List<MetaItem>,
        private val onClick: (MetaItem) -> Unit,
        private val onLongClick: (View, MetaItem) -> Unit,
        private val onFocus: (MetaItem) -> Unit
    ) : RecyclerView.Adapter<DiscoverContentAdapter.ViewHolder>() {

        inner class ViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
            val poster: android.widget.ImageView = view.findViewById(R.id.poster)
        }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_poster, parent, false)
            return ViewHolder(view)
        }
        override fun getItemCount() = items.size
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            holder.poster.scaleType = android.widget.ImageView.ScaleType.CENTER_CROP
            Glide.with(holder.view.context).load(item.poster).placeholder(R.drawable.movie).into(holder.poster)

            holder.view.setOnClickListener { onClick(item) }
            holder.view.setOnLongClickListener { onLongClick(holder.view, item); true }

            holder.view.setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) {
                    onFocus(item)
                    // REMOVED SCALING ANIMATION as requested
                }
            }
            holder.view.isFocusable = true
            holder.view.isFocusableInTouchMode = true
        }
        fun updateData(newItems: List<MetaItem>) {
            items = newItems
            notifyDataSetChanged()
        }
    }


    private fun loadCatalogs() {
        viewModel.getDiscoverCatalogs(currentType).observe(viewLifecycleOwner) { catalogs ->
            sidebarAdapter.submitList(catalogs)
            if (catalogs.isNotEmpty()) {
                viewModel.loadContentForCatalog(catalogs[0])
                binding.rvSidebar.post {
                    binding.rvSidebar.findViewHolderForAdapterPosition(0)?.itemView?.requestFocus()
                }
            }
        }
    }

    private fun showItemMenu(view: View, item: MetaItem) {
        val popup = PopupMenu(requireContext(), view)
        popup.menu.add("Add to Library")
        popup.menu.add("Add to TMDB Watchlist")
        popup.setOnMenuItemClickListener { menuItem ->
            when (menuItem.title) {
                "Add to Library" -> { viewModel.addToLibrary(item); true }
                "Add to TMDB Watchlist" -> { viewModel.toggleWatchlist(item, true); true }
                else -> false
            }
        }
        popup.show()
    }



    private fun onContentClicked(item: MetaItem) {
        val intent = Intent(requireContext(), DetailsActivity2::class.java).apply {
            putExtra("metaId", item.id)
            putExtra("title", item.name)
            putExtra("poster", item.poster)
            putExtra("background", item.background)
            putExtra("description", item.description)
            putExtra("type", currentType)
        }
        startActivity(intent)
    }



    private fun setupObservers() {
        viewModel.currentCatalogContent.observe(viewLifecycleOwner) { items ->
            contentAdapter.updateData(items)
            if (items.isNotEmpty()) {
                updateDetailsPane(items[0])
            }
        }

        // LOGO OBSERVER
        viewModel.currentLogo.observe(viewLifecycleOwner) { logoUrl ->
            if (logoUrl != null) {
                // Logo found: Hide text, show image
                binding.detailTitle.visibility = View.GONE
                binding.detailLogo.visibility = View.VISIBLE
                Glide.with(this)
                    .load(logoUrl)
                    .fitCenter()
                    .into(binding.detailLogo)
            } else {
                // No logo: Show text, hide image
                binding.detailTitle.visibility = View.VISIBLE
                binding.detailLogo.visibility = View.GONE
            }
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
    }

    // ... Adapters (Keep existing SidebarAdapter and DiscoverContentAdapter) ...
    // (Included in previous response, just ensure they are present)


    inner class SidebarAdapter(private val onClick: (UserCatalog) -> Unit) :
        androidx.recyclerview.widget.ListAdapter<UserCatalog, SidebarAdapter.ViewHolder>(com.example.stremiompvplayer.adapters.CatalogConfigAdapter.DiffCallback()) {
        inner class ViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
            val name: TextView = view.findViewById(R.id.catalogName)
        }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_discover_sidebar, parent, false)
            return ViewHolder(view)
        }
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = getItem(position)
            holder.name.text = item.displayName
            holder.view.setOnClickListener { onClick(item) }
            holder.view.setOnFocusChangeListener { _, hasFocus -> if(hasFocus) onClick(item) }
        }
    }
}