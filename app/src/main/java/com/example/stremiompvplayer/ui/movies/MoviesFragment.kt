package com.example.stremiompvplayer.ui.movies

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
// import androidx.recyclerview.widget.GridLayoutManager // No longer needed
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.example.stremiompvplayer.PlayerActivity
import com.example.stremiompvplayer.data.AppDatabase
import com.example.stremiompvplayer.data.ServiceLocator
import com.example.stremiompvplayer.databinding.FragmentMoviesBinding
import com.example.stremiompvplayer.models.Catalog
import com.example.stremiompvplayer.models.CollectedItem
import com.example.stremiompvplayer.models.MetaItem
import com.example.stremiompvplayer.models.Stream
import com.example.stremiompvplayer.models.UserCatalog
import com.example.stremiompvplayer.adapters.CatalogChipAdapter
import com.example.stremiompvplayer.adapters.StreamAdapter
import com.example.stremiompvplayer.adapters.PosterAdapter
import com.example.stremiompvplayer.utils.SharedPreferencesManager
import com.example.stremiompvplayer.viewmodels.CatalogViewModel
import com.example.stremiompvplayer.viewmodels.CatalogUiState
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

class MoviesFragment : Fragment() {

    private var _binding: FragmentMoviesBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CatalogViewModel by viewModels {
        ServiceLocator.provideCatalogViewModelFactory(requireContext())
    }

    private lateinit var catalogChipAdapter: CatalogChipAdapter
    private lateinit var streamAdapter: StreamAdapter
    private lateinit var posterAdapter: PosterAdapter
    private lateinit var prefsManager: SharedPreferencesManager
    private lateinit var db: AppDatabase

    private var currentMetaItem: MetaItem? = null
    private var currentUserId: String? = null
    private var userCatalogs: List<UserCatalog> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMoviesBinding.inflate(inflater, container, false)
        prefsManager = SharedPreferencesManager.getInstance(requireContext())
        db = AppDatabase.getInstance(requireContext())
        currentUserId = prefsManager.getCurrentUserId()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupCatalogChips()
        setupStreamsRecycler()
        setupMovieGrid()
        observeUserCatalogs()
        observeViewModel()
    }

    private fun setupCatalogChips() {
        catalogChipAdapter = CatalogChipAdapter(
            onClick = { catalog ->
                Log.d("MoviesFragment", "Catalog clicked: ${catalog.name}")
                viewModel.fetchCatalog(catalog.type, catalog.id)
            },
            onLongClick = { catalog ->
                showCatalogOptionsDialog(catalog)
            }
        )

        binding.catalogChipsRecycler.apply {
            layoutManager = LinearLayoutManager(context, RecyclerView.HORIZONTAL, false)
            adapter = catalogChipAdapter
        }
    }

    private fun setupStreamsRecycler() {
        streamAdapter = StreamAdapter { stream ->
            onStreamClick(stream)
        }

        binding.streamsRecycler.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = streamAdapter
        }
    }

    private fun setupMovieGrid() {
        posterAdapter = PosterAdapter(
            items = emptyList(),
            onClick = { metaItem ->
                displayMovieDetails(metaItem)
            },
            onLongClick = { metaItem ->
                showCollectionDialog(metaItem)
            }
        )

        binding.moviesGridRecycler.apply {
            // Horizontal scrolling list
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = posterAdapter
        }
    }

    private fun observeUserCatalogs() {
        val userId = currentUserId ?: return

        db.userCatalogDao().getCatalogsForPage(userId, "movies").observe(viewLifecycleOwner) { catalogs ->
            Log.d("MoviesFragment", "User catalogs updated: ${catalogs.size} catalogs")
            userCatalogs = catalogs

            if (catalogs.isEmpty()) {
                // Try to populate defaults if empty
                populateDefaultCatalogs(userId)
            } else {
                hideEmptyState()

                val chipCatalogs = catalogs.map { userCatalog ->
                    Catalog(
                        type = userCatalog.catalogType,
                        id = userCatalog.catalogId,
                        name = userCatalog.displayName,
                        extraProps = null
                    )
                }

                catalogChipAdapter.setCatalogs(chipCatalogs)

                if (currentMetaItem == null) {
                    catalogs.firstOrNull()?.let { firstCatalog ->
                        viewModel.fetchCatalog(firstCatalog.catalogType, firstCatalog.catalogId)
                    }
                }
            }
        }
    }

    // FIX: Corrected UserCatalog instantiation parameters
    private fun populateDefaultCatalogs(userId: String) {
        viewModel.loadedCatalogs.observe(viewLifecycleOwner) { allCatalogs ->
            val defaultMovieCatalogs = allCatalogs.filter { it.type == "movie" }

            if (defaultMovieCatalogs.isNotEmpty()) {
                lifecycleScope.launch {
                    val currentCount = db.userCatalogDao().getMaxDisplayOrder(userId, "movies") ?: 0

                    if (currentCount == 0) {
                        defaultMovieCatalogs.forEachIndexed { index, catalog ->
                            val userCatalog = UserCatalog(
                                userId = userId,
                                catalogId = catalog.id,
                                catalogType = catalog.type,
                                catalogName = catalog.name, // Corrected parameter
                                customName = null,          // Explicit null
                                pageType = "movies",
                                addonUrl = "",              // Empty string for now
                                manifestId = "default",
                                displayOrder = index
                            )
                            db.userCatalogDao().insert(userCatalog)
                        }
                        Toast.makeText(context, "Default catalogs added", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                showEmptyState()
            }
        }
    }

    private fun observeViewModel() {
        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is CatalogUiState.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                }
                is CatalogUiState.Error -> {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(context, "Error: ${state.message}", Toast.LENGTH_LONG).show()
                }
                is CatalogUiState.Success -> {
                    binding.progressBar.visibility = View.GONE

                    if (state.items.isNotEmpty()) {
                        posterAdapter.updateData(state.items)

                        if (currentMetaItem == null) {
                            state.items.firstOrNull()?.let { firstMovie ->
                                displayMovieDetails(firstMovie)
                            }
                        }
                    }
                }
            }
        }

        viewModel.streams.observe(viewLifecycleOwner) { streams ->
            displayStreams(streams)
        }
    }

    private fun showEmptyState() {
        binding.catalogChipsRecycler.visibility = View.GONE
        Toast.makeText(
            context,
            "No catalogs found. Check your connection.",
            Toast.LENGTH_LONG
        ).show()
    }

    private fun hideEmptyState() {
        binding.catalogChipsRecycler.visibility = View.VISIBLE
    }

    // ... (Dialog functions remain the same: showCatalogOptionsDialog, showMoveCatalogDialog, etc.) ...
    private fun showCatalogOptionsDialog(catalog: Catalog) {
        val userCatalog = userCatalogs.find {
            it.catalogId == catalog.id && it.catalogType == catalog.type
        } ?: return

        val options = arrayOf("Move", "Rename", "Remove")

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(catalog.name)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showMoveCatalogDialog(userCatalog)
                    1 -> showRenameCatalogDialog(userCatalog)
                    2 -> showRemoveCatalogDialog(userCatalog)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showMoveCatalogDialog(userCatalog: UserCatalog) {
        val currentPosition = userCatalogs.indexOf(userCatalog)
        if (currentPosition == -1) return

        val positions = userCatalogs.mapIndexed { index, catalog ->
            "${index + 1}. ${catalog.displayName}"
        }.toTypedArray()

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Move to Position")
            .setSingleChoiceItems(positions, currentPosition) { dialog, which ->
                if (which != currentPosition) {
                    moveCatalog(userCatalog, currentPosition, which)
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun moveCatalog(catalog: UserCatalog, fromPosition: Int, toPosition: Int) {
        lifecycleScope.launch {
            try {
                val reorderedList = userCatalogs.toMutableList()
                reorderedList.removeAt(fromPosition)
                reorderedList.add(toPosition, catalog)

                reorderedList.forEachIndexed { index, userCatalog ->
                    db.userCatalogDao().updateDisplayOrder(userCatalog.id, index)
                }
                Toast.makeText(context, "Catalog moved", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showRenameCatalogDialog(userCatalog: UserCatalog) {
        val input = EditText(requireContext()).apply {
            setText(userCatalog.displayName)
            inputType = InputType.TYPE_CLASS_TEXT
            setPadding(48, 32, 48, 32)
        }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Rename Catalog")
            .setView(input)
            .setPositiveButton("Rename") { _, _ ->
                val newName = input.text.toString().trim()
                if (newName.isNotEmpty()) {
                    renameCatalog(userCatalog, newName)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun renameCatalog(catalog: UserCatalog, newName: String) {
        lifecycleScope.launch {
            try {
                db.userCatalogDao().updateCustomName(catalog.id, newName)
                Toast.makeText(context, "Catalog renamed", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showRemoveCatalogDialog(userCatalog: UserCatalog) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Remove Catalog")
            .setMessage("Remove \"${userCatalog.displayName}\" from Movies page?")
            .setPositiveButton("Remove") { _, _ ->
                removeCatalog(userCatalog)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun removeCatalog(catalog: UserCatalog) {
        lifecycleScope.launch {
            try {
                db.userCatalogDao().delete(catalog)
                Toast.makeText(context, "Catalog removed", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showCollectionDialog(metaItem: MetaItem) {
        val userId = currentUserId ?: return
        lifecycleScope.launch {
            val collectedId = "${userId}_${metaItem.id}"
            val isCollected = db.collectedItemDao().isCollected(collectedId) > 0
            if (isCollected) {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Remove from Library")
                    .setMessage("Remove \"${metaItem.name}\" from your library?")
                    .setPositiveButton("Remove") { _, _ -> uncollectItem(collectedId) }
                    .setNegativeButton("Cancel", null)
                    .show()
            } else {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Add to Library")
                    .setMessage("Add \"${metaItem.name}\" to your library?")
                    .setPositiveButton("Collect") { _, _ -> collectItem(metaItem) }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        }
    }

    private fun collectItem(metaItem: MetaItem) {
        val userId = currentUserId ?: return
        lifecycleScope.launch {
            try {
                val collectedItem = CollectedItem.fromMetaItem(userId, metaItem)
                db.collectedItemDao().insert(collectedItem)
                Toast.makeText(context, "Added to library", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun uncollectItem(collectedId: String) {
        lifecycleScope.launch {
            try {
                db.collectedItemDao().deleteById(collectedId)
                Toast.makeText(context, "Removed from library", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun displayMovieDetails(metaItem: MetaItem) {
        currentMetaItem = metaItem

        // Load Poster (High quality)
        Glide.with(this)
            .load(metaItem.poster)
            .centerCrop()
            .into(binding.selectedPoster)

        // NEW: Load Background Image
        Glide.with(this)
            .load(metaItem.background ?: metaItem.poster) // Fallback to poster if no background
            .transition(DrawableTransitionOptions.withCrossFade())
            .into(binding.backgroundImage)

        binding.movieTitle.text = metaItem.name
        binding.movieDescription.text = metaItem.description ?: "No description"

        fetchStreamsForMovie(metaItem)
    }

    private fun fetchStreamsForMovie(metaItem: MetaItem) {
        binding.noStreamsText.visibility = View.VISIBLE
        binding.streamsRecycler.visibility = View.GONE
        binding.noStreamsText.text = "Loading streams..."

        viewModel.fetchStreams(metaItem.type, metaItem.id)
    }

    private fun displayStreams(streams: List<Stream>) {
        if (streams.isEmpty()) {
            binding.noStreamsText.visibility = View.VISIBLE
            binding.streamsRecycler.visibility = View.GONE
            binding.noStreamsText.text = "No streams available"
        } else {
            binding.noStreamsText.visibility = View.GONE
            binding.streamsRecycler.visibility = View.VISIBLE
            streamAdapter.submitList(streams)
        }
    }

    private fun onStreamClick(stream: Stream) {
        val url = stream.url
        if (url.isNullOrBlank()) {
            Toast.makeText(context, "Invalid stream URL", Toast.LENGTH_SHORT).show()
            return
        }
        val intent = Intent(requireContext(), PlayerActivity::class.java).apply {
            putExtra("stream", stream)
            putExtra("meta", currentMetaItem)
        }
        startActivity(intent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}