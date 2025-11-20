package com.example.stremiompvplayer.ui.discover

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.stremiompvplayer.DetailsActivity2
import com.example.stremiompvplayer.R
import com.example.stremiompvplayer.adapters.PosterAdapter
import com.example.stremiompvplayer.data.ServiceLocator
import com.example.stremiompvplayer.databinding.FragmentDiscoverBinding
import com.example.stremiompvplayer.models.MetaItem
import com.example.stremiompvplayer.models.UserCatalog
import com.example.stremiompvplayer.utils.SharedPreferencesManager
import com.example.stremiompvplayer.viewmodels.MainViewModel
import com.example.stremiompvplayer.viewmodels.MainViewModelFactory

class DiscoverFragment : Fragment() {

    private var _binding: FragmentDiscoverBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MainViewModel by activityViewModels {
        MainViewModelFactory(
            ServiceLocator.getInstance(requireContext()),
            SharedPreferencesManager.getInstance(requireContext())
        )
    }

    private lateinit var sidebarAdapter: SidebarAdapter
    // We use a custom adapter instead of PosterAdapter to handle focus better if needed,
    // but for now using the internal class which mimics PosterAdapter logic
    private lateinit var contentAdapter: DiscoverContentAdapter

    private var currentType = "movie"
    private var currentLevel = Level.CATALOG_CONTENT
    private var selectedSeries: MetaItem? = null

    // IDs for back navigation items
    private val BACK_ITEM_ID = "ACTION_BACK"

    private enum class Level {
        CATALOG_CONTENT, SEASONS, EPISODES, PERSON_CREDITS
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDiscoverBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupAdapters()
        setupUI()
        setupObservers()
        switchType("movie")
    }

    private fun setupUI() {
        binding.btnMovies.setOnClickListener { switchType("movie") }
        binding.btnSeries.setOnClickListener { switchType("series") }
    }

    private fun setupAdapters() {
        sidebarAdapter = SidebarAdapter { catalog ->
            viewModel.loadContentForCatalog(catalog)
            updateLevel(Level.CATALOG_CONTENT)
        }
        binding.rvSidebar.layoutManager = LinearLayoutManager(context)
        binding.rvSidebar.adapter = sidebarAdapter

        contentAdapter = DiscoverContentAdapter(
            items = emptyList(),
            onClick = { item -> onContentClicked(item) },
            onFocus = { item -> updateDetailsPane(item) }
        )
        binding.rvContent.layoutManager = GridLayoutManager(context, 3)
        binding.rvContent.adapter = contentAdapter
    }

    private fun updateLevel(level: Level) {
        currentLevel = level
        // Hide sidebar if drilled down
        if (level == Level.CATALOG_CONTENT) {
            binding.rvSidebar.visibility = View.VISIBLE
            // Restore sidebar focus if needed
        } else {
            binding.rvSidebar.visibility = View.GONE
        }
    }

    inner class DiscoverContentAdapter(
        private var items: List<MetaItem>,
        private val onClick: (MetaItem) -> Unit,
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

            // Handle "Back" item display
            if (item.id == BACK_ITEM_ID) {
                holder.poster.setImageResource(R.drawable.ic_arrow_back_white_24dp) // Ensure this drawable exists
                holder.poster.scaleType = android.widget.ImageView.ScaleType.CENTER_INSIDE
            } else {
                holder.poster.scaleType = android.widget.ImageView.ScaleType.CENTER_CROP
                Glide.with(holder.view.context)
                    .load(item.poster)
                    .placeholder(R.drawable.movie)
                    .into(holder.poster)
            }

            holder.view.setOnClickListener { onClick(item) }
            holder.view.setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) onFocus(item)
            }
        }

        fun updateData(newItems: List<MetaItem>) {
            items = newItems
            notifyDataSetChanged()
        }
    }

    private fun setupObservers() {
        viewModel.currentCatalogContent.observe(viewLifecycleOwner) { items ->
            if (currentLevel == Level.CATALOG_CONTENT || currentLevel == Level.PERSON_CREDITS) {
                // If Person Credits, we might need to prepend a back button
                // But logic is handled in loadPersonCredits mostly.
                // Wait, loadPersonCredits updates _currentCatalogContent directly.
                // So we need to check currentLevel to decide if we add back button.

                val displayList = if (currentLevel == Level.PERSON_CREDITS) {
                    mutableListOf(createBackItem()).apply { addAll(items) }
                } else {
                    items
                }

                contentAdapter.updateData(displayList)
                if (displayList.isNotEmpty()) updateDetailsPane(displayList[0])
            }
        }

        viewModel.castList.observe(viewLifecycleOwner) { castList ->
            val container = binding.castContainer // Now a LinearLayout
            container.removeAllViews()

            castList.forEach { castMember ->
                val chip = LayoutInflater.from(context)
                    .inflate(R.layout.item_cast_chip, container, false) as TextView
                chip.text = castMember.name

                chip.setOnClickListener {
                    // Trigger Cast Search
                    viewModel.loadPersonCredits(castMember.id)
                    updateLevel(Level.PERSON_CREDITS)
                }

                container.addView(chip)
            }
        }

        viewModel.metaDetails.observe(viewLifecycleOwner) { meta ->
            if (meta != null && currentLevel == Level.SEASONS) {
                val seasons = meta.videos?.mapNotNull { it.season }?.distinct()?.sorted()
                    ?.map { seasonNum ->
                        MetaItem(
                            id = seasonNum.toString(),
                            name = "Season $seasonNum",
                            poster = meta.poster,
                            type = "season",
                            description = "Season $seasonNum",
                            background = meta.background
                        )
                    } ?: emptyList()

                val displayList = mutableListOf(createBackItem()).apply { addAll(seasons) }
                contentAdapter.updateData(displayList)
                if (displayList.isNotEmpty()) updateDetailsPane(displayList[0]) // Update details to Back item or first season
            }
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
    }

    private fun createBackItem(): MetaItem {
        return MetaItem(
            id = BACK_ITEM_ID,
            type = "navigation",
            name = "Go Back",
            poster = null,
            background = null,
            description = "Return to previous level"
        )
    }

    private fun switchType(type: String) {
        currentType = type
        updateLevel(Level.CATALOG_CONTENT)

        if (type == "movie") {
            binding.btnMovies.setTextColor(Color.WHITE)
            binding.btnSeries.setTextColor(Color.parseColor("#AAAAAA"))
        } else {
            binding.btnMovies.setTextColor(Color.parseColor("#AAAAAA"))
            binding.btnSeries.setTextColor(Color.WHITE)
        }

        viewModel.getDiscoverCatalogs(type).observe(viewLifecycleOwner) { catalogs ->
            sidebarAdapter.submitList(catalogs)
            if (catalogs.isNotEmpty()) {
                viewModel.loadContentForCatalog(catalogs[0])
            }
        }
    }

    private fun onContentClicked(item: MetaItem) {
        if (item.id == BACK_ITEM_ID) {
            handleBackPress()
            return
        }

        if (currentType == "movie" && currentLevel != Level.PERSON_CREDITS) {
            // Movie click
            val intent = Intent(requireContext(), DetailsActivity2::class.java).apply {
                putExtra("metaId", item.id)
                putExtra("title", item.name)
                putExtra("poster", item.poster)
                putExtra("background", item.background)
                putExtra("description", item.description)
                putExtra("type", "movie")
            }
            startActivity(intent)
        } else if (currentLevel == Level.PERSON_CREDITS) {
            // Clicked a movie/show from person credits
            // Treat as standard item click but respect its type
            if (item.type == "movie") {
                val intent = Intent(requireContext(), DetailsActivity2::class.java).apply {
                    putExtra("metaId", item.id)
                    putExtra("title", item.name)
                    putExtra("poster", item.poster)
                    putExtra("background", item.background)
                    putExtra("description", item.description)
                    putExtra("type", "movie")
                }
                startActivity(intent)
            } else {
                // Switch to series drill down?
                // It's complicated to switch context inside Person Credits.
                // For now, let's assume we just launch it if it's a movie,
                // or enter series flow if series
                selectedSeries = item
                currentLevel = Level.SEASONS
                viewModel.loadSeriesMeta(item.id)
            }
        } else {
            // Series Drill Down
            when (currentLevel) {
                Level.CATALOG_CONTENT -> {
                    selectedSeries = item
                    updateLevel(Level.SEASONS)
                    viewModel.loadSeriesMeta(item.id)
                }
                Level.SEASONS -> {
                    val seasonNum = item.id.toIntOrNull() ?: 1
                    updateLevel(Level.EPISODES)
                    displayEpisodes(seasonNum)
                }
                Level.EPISODES -> {
                    if (selectedSeries != null) {
                        // Play Episode logic
                        // viewModel.loadEpisodeStreams(...)
                    }
                }
                else -> {}
            }
        }
    }

    private fun displayEpisodes(season: Int) {
        val meta = viewModel.metaDetails.value ?: return
        val episodes = meta.videos?.filter { it.season == season }?.sortedBy { it.number } ?: emptyList()
        val items = episodes.map { vid ->
            MetaItem(
                id = vid.id,
                name = "Ep ${vid.number}: ${vid.title}",
                poster = vid.thumbnail ?: meta.poster,
                background = meta.background,
                description = "Episode ${vid.number}",
                type = "episode"
            )
        }
        val displayList = mutableListOf(createBackItem()).apply { addAll(items) }
        contentAdapter.updateData(displayList)
    }

    private fun updateDetailsPane(item: MetaItem) {
        if (item.id == BACK_ITEM_ID) {
            binding.detailTitle.text = "Go Back"
            binding.detailDescription.text = "Return to previous list"
            binding.castContainer.removeAllViews()
            return
        }

        Glide.with(this)
            .load(item.background ?: item.poster)
            .into(binding.pageBackground)

        binding.detailTitle.text = item.name
        binding.detailRating.text = ""
        binding.detailDate.text = ""

        when(currentLevel) {
            Level.CATALOG_CONTENT, Level.PERSON_CREDITS -> {
                binding.detailDescription.text = item.description
                viewModel.fetchCast(item.id, if (item.type == "movie") "movie" else "tv")
            }
            Level.SEASONS -> {
                binding.detailDescription.text = item.description
                binding.castContainer.removeAllViews()
            }
            Level.EPISODES -> {
                binding.detailDescription.text = item.description
                binding.castContainer.removeAllViews()
            }
        }
    }

    // Sidebar Adapter (Simple ListAdapter)
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

    fun handleBackPress(): Boolean {
        if (currentLevel != Level.CATALOG_CONTENT) {
            if (currentLevel == Level.PERSON_CREDITS) {
                // Return to Catalog Content
                // We need to reload the catalog content that was there before?
                // Ideally we cached it, but for simplicity we reload the *current selected catalog*.
                // We can just trigger sidebar click on current item if we track it, or just reload via VM
                // Actually VM might still hold the data if we didn't overwrite it.
                // loadPersonCredits overwrites _currentCatalogContent.
                // So we need to reload catalog.
                // Let's assume sidebar has selection or we just default to first?
                // Better: just switchType(currentType) to reset.
                switchType(currentType)
            } else if (currentLevel == Level.EPISODES) {
                updateLevel(Level.SEASONS)
                viewModel.metaDetails.value?.let {
                    // Re-trigger observer logic or manually call display
                    // Just force notify observer by re-posting value? No, simple call:
                    // Logic duplicated from observer:
                    val seasons = it.videos?.mapNotNull { v -> v.season }?.distinct()?.sorted()?.map { s ->
                        MetaItem(id = s.toString(), name = "Season $s", poster = it.poster, type="season", description="Season $s", background=it.background)
                    } ?: emptyList()
                    contentAdapter.updateData(mutableListOf(createBackItem()).apply{addAll(seasons)})
                }
            } else if (currentLevel == Level.SEASONS) {
                switchType(currentType) // Resets to Catalog
            }
            return true
        }
        return false
    }
}