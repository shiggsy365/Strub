package com.example.stremiompvplayer.ui.library

import android.content.Intent
import android.os.Bundle
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
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
    private var currentSelectedItem: MetaItem? = null
    private var currentType = "movie"

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
        setupKeyHandling()
        loadLibrary()
    }

    override fun onResume() {
        super.onResume()
        currentSelectedItem?.let { updateDetailsPane(it) }
    }

    private fun setupKeyHandling() {
        // Make posterCarousel focusable to receive key events
        binding.posterCarousel.isFocusable = true
        binding.posterCarousel.isFocusableInTouchMode = true

        // Set key listener for up navigation
        binding.root.setOnKeyListener { _, keyCode, event ->
            if (event.action == android.view.KeyEvent.ACTION_DOWN) {
                when (keyCode) {
                    android.view.KeyEvent.KEYCODE_DPAD_UP -> {
                        val focusedChild = binding.rvContent.focusedChild
                        if (focusedChild != null) {
                            binding.root.findViewById<View>(R.id.btnPlay)?.requestFocus()
                            return@setOnKeyListener true
                        }
                        false
                    }
                    else -> false
                }
            } else {
                false
            }
        }
    }

    private fun setupAdapters() {
        contentAdapter = PosterAdapter(
            items = emptyList(),
            onClick = { item ->
                // For playable items, focus play button; for non-playable, open details
                if (item.type == "movie" || item.type == "episode") {
                    updateDetailsPane(item)
                    binding.root.findViewById<View>(R.id.btnPlay)?.requestFocus()
                } else {
                    onContentClicked(item)
                }
            },
            onLongClick = { item ->
                val pos = contentAdapter.getItemPosition(item)
                val holder = binding.rvContent.findViewHolderForAdapterPosition(pos)
                if (holder != null) showItemMenu(holder.itemView, item)
            }
        )

        // Horizontal scrolling for Netflix-style poster carousel
        binding.rvContent.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
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
                                    delay(300) // PERFORMANCE: Reduced delay for snappier UX
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

        // Setup Play button
        binding.root.findViewById<View>(R.id.btnPlay)?.setOnClickListener {
            currentSelectedItem?.let { item ->
                showStreamDialog(item)
            }
        }

        // Setup Trailer button
        binding.root.findViewById<View>(R.id.btnTrailer)?.setOnClickListener {
            currentSelectedItem?.let { item ->
                playTrailer(item)
            }
        }

        // Setup Related button
        binding.root.findViewById<View>(R.id.btnRelated)?.setOnClickListener {
            currentSelectedItem?.let { item ->
                showRelatedContent(item)
            }
        }
    }

    private fun loadLibrary() {
        viewModel.filterAndSortLibrary(currentType)
        updateCurrentListLabel("My Library")
    }

    private fun updateCurrentListLabel(labelText: String) {
        val label = binding.root.findViewById<android.widget.TextView>(R.id.currentListLabel)
        label?.text = labelText
    }

    private fun updateItemUI(item: MetaItem, isWatched: Boolean) {
        item.isWatched = isWatched
        item.progress = if (isWatched) item.duration else 0

        val position = contentAdapter.getItemPosition(item)
        if (position != -1) {
            contentAdapter.notifyItemChanged(position)
        }
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

        // Episode Info (Specific to episodes)
        if (item.type == "episode") {
            val parts = item.id.split(":")
            if (parts.size >= 4) {
                val season = parts[2]
                val episode = parts[3]
                binding.detailEpisode.text = "S${season.padStart(2, '0')}E${episode.padStart(2, '0')}"
                binding.detailEpisode.visibility = View.VISIBLE
            } else {
                binding.detailEpisode.visibility = View.GONE
            }
        } else {
            binding.detailEpisode.visibility = View.GONE
        }

        binding.detailTitle.text = item.name
        binding.detailTitle.visibility = View.GONE
        binding.detailLogo.visibility = View.GONE

        viewModel.fetchItemLogo(item)

        // Update actor chips
        updateActorChips(item)
    }

    private fun showItemMenu(view: View, item: MetaItem) {
        val wrapper = ContextThemeWrapper(requireContext(),
            android.R.style.Theme_DeviceDefault_Light_NoActionBar)
        val popup = PopupMenu(wrapper, view)

        // Use lifecycleScope to get library status synchronously before showing menu
        viewLifecycleOwner.lifecycleScope.launch {
            val isInLibrary = viewModel.isItemInLibrarySync(item.id)

            if (isInLibrary) {
                popup.menu.add("Remove from Library")
            } else {
                popup.menu.add("Add to Library")
            }

            popup.menu.add("Mark as Watched")
            popup.menu.add("Clear Progress")

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
                    "Clear Progress" -> {
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
    }

    private fun refreshItem(item: MetaItem) {
        val position = contentAdapter.getItemPosition(item)
        if (position != -1) {
            contentAdapter.notifyItemChanged(position)
        }
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

    private fun showStreamDialog(item: MetaItem) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_stream_selection, null)
        val dialog = android.app.AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        val rvStreams = dialogView.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.rvStreams)
        val progressBar = dialogView.findViewById<android.widget.ProgressBar>(R.id.progressBar)
        val btnCancel = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnCancel)
        val dialogTitle = dialogView.findViewById<android.widget.TextView>(R.id.dialogTitle)

        dialogTitle.text = "Select Stream - ${item.name}"

        val streamAdapter = com.example.stremiompvplayer.adapters.StreamAdapter { stream ->
            dialog.dismiss()
            playStream(stream)
        }
        rvStreams.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(requireContext())
        rvStreams.adapter = streamAdapter

        btnCancel.setOnClickListener { dialog.dismiss() }

        progressBar.visibility = View.VISIBLE
        rvStreams.visibility = View.GONE

        viewModel.loadStreams(item.type, item.id)

        val streamObserver = androidx.lifecycle.Observer<List<com.example.stremiompvplayer.models.Stream>> { streams ->
            progressBar.visibility = View.GONE
            rvStreams.visibility = View.VISIBLE
            if (streams.isEmpty()) {
                Toast.makeText(requireContext(), "No streams available", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            } else {
                streamAdapter.submitList(streams)
            }
        }
        viewModel.streams.observe(viewLifecycleOwner, streamObserver)

        dialog.setOnDismissListener {
            viewModel.streams.removeObserver(streamObserver)
        }

        dialog.show()
    }

    private fun playStream(stream: com.example.stremiompvplayer.models.Stream) {
        val intent = Intent(requireContext(), com.example.stremiompvplayer.PlayerActivity::class.java).apply {
            putExtra("stream", stream)
            putExtra("title", currentSelectedItem?.name ?: "Unknown")
            putExtra("metaId", currentSelectedItem?.id)
        }
        startActivity(intent)
    }

    private fun playTrailer(item: MetaItem) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val trailerUrl = viewModel.fetchTrailer(item.id, item.type)
                if (trailerUrl != null) {
                    val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(trailerUrl))
                    startActivity(intent)
                } else {
                    Toast.makeText(requireContext(), "No trailer available", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error loading trailer", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showRelatedContent(item: MetaItem) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                viewModel.fetchSimilarContent(item.id, item.type)
                Toast.makeText(requireContext(), "Loading related content...", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error loading related content", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateActorChips(item: MetaItem) {
        val actorChipGroup = binding.root.findViewById<com.google.android.material.chip.ChipGroup>(R.id.actorChips)
        actorChipGroup?.removeAllViews()

        // Fetch cast information from TMDB
        viewModel.fetchCast(item.id, item.type)
    }

    private fun setupObservers() {
        viewModel.libraryMovies.observe(viewLifecycleOwner, androidx.lifecycle.Observer {})
        viewModel.librarySeries.observe(viewLifecycleOwner, androidx.lifecycle.Observer {})

        viewModel.actionResult.observe(viewLifecycleOwner) { result ->
            when (result) {
                is MainViewModel.ActionResult.Success -> {
                    Toast.makeText(requireContext(), result.message, Toast.LENGTH_SHORT).show()
                }
                is MainViewModel.ActionResult.Error -> {
                    Toast.makeText(requireContext(), result.message, Toast.LENGTH_LONG).show()
                }
            }
        }

        val liveData = if (currentType == "movie") viewModel.filteredLibraryMovies else viewModel.filteredLibrarySeries

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

        // Observe cast list and update actor chips
        viewModel.castList.observe(viewLifecycleOwner) { castList ->
            val actorChipGroup = binding.root.findViewById<com.google.android.material.chip.ChipGroup>(R.id.actorChips)
            actorChipGroup?.removeAllViews()

            castList.take(5).forEach { actor ->
                val chip = com.google.android.material.chip.Chip(requireContext())
                chip.text = actor.name
                chip.isClickable = true
                chip.isFocusable = true
                chip.setChipBackgroundColorResource(R.color.md_theme_surfaceContainer)
                chip.setTextColor(resources.getColor(R.color.text_primary, null))

                chip.setOnClickListener {
                    val personId = actor.id.removePrefix("tmdb:").toIntOrNull()
                    if (personId != null) {
                        // Navigate to search with person
                        val intent = Intent(requireContext(), com.example.stremiompvplayer.MainActivity::class.java).apply {
                            putExtra("SEARCH_PERSON_ID", personId)
                            putExtra("SEARCH_QUERY", actor.name)
                        }
                        startActivity(intent)
                    }
                }

                actorChipGroup?.addView(chip)
            }
        }
    }

    fun focusSidebar(): Boolean {
        binding.root.post {
            // Focus on Play button or poster carousel
            binding.root.findViewById<View>(R.id.btnPlay)?.requestFocus()
                ?: binding.rvContent.requestFocus()
        }
        return true
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        detailsUpdateJob?.cancel()
    }
}
