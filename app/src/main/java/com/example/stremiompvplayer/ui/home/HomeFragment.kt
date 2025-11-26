package com.example.stremiompvplayer.ui.home

import android.content.Intent
import android.os.Bundle
import android.view.ContextThemeWrapper
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.stremiompvplayer.DetailsActivity2
import com.example.stremiompvplayer.R
import com.example.stremiompvplayer.adapters.PosterAdapter
import com.example.stremiompvplayer.data.ServiceLocator
import com.example.stremiompvplayer.databinding.FragmentHomeBinding
import com.example.stremiompvplayer.models.MetaItem
import com.example.stremiompvplayer.models.UserCatalog
import com.example.stremiompvplayer.utils.SharedPreferencesManager
import com.example.stremiompvplayer.utils.FocusMemoryManager
import com.example.stremiompvplayer.viewmodels.MainViewModel
import com.example.stremiompvplayer.viewmodels.MainViewModelFactory
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MainViewModel by activityViewModels {
        MainViewModelFactory(
            ServiceLocator.getInstance(requireContext()),
            SharedPreferencesManager.getInstance(requireContext())
        )
    }

    private lateinit var contentAdapter: PosterAdapter
    private var currentSelectedItem: MetaItem? = null
    private var detailsUpdateJob: Job? = null

    // Track current catalogs and index for cycling
    private var currentCatalogs = listOf<UserCatalog>()
    private var currentCatalogIndex = 0

    private val focusMemoryManager = FocusMemoryManager.getInstance()
    private val fragmentKey = "home"

    companion object {
        fun newInstance(): HomeFragment {
            return HomeFragment()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
        setupAdapters()
        setupObservers()
        loadHomeCatalogs()
    }

    override fun onResume() {
        super.onResume()
        currentSelectedItem?.let { updateDetailsPane(it) }
        if (currentCatalogs.isNotEmpty() && currentCatalogIndex < currentCatalogs.size) {
            viewModel.loadContentForCatalog(currentCatalogs[currentCatalogIndex], isInitialLoad = true)
        }
        val savedPosition = focusMemoryManager.getSavedPosition(fragmentKey)
        if (savedPosition >= 0 && savedPosition < contentAdapter.itemCount) {
            binding.root.postDelayed({
                binding.rvContent.scrollToPosition(savedPosition)
                binding.rvContent.postDelayed({
                    binding.rvContent.layoutManager?.findViewByPosition(savedPosition)?.requestFocus()
                }, 100)
            }, 150)
        }
    }

    override fun onPause() {
        super.onPause()
        val currentView = view?.findFocus()
        if (currentView != null && currentView.parent == binding.rvContent) {
            val position = binding.rvContent.getChildAdapterPosition(currentView)
            if (position != RecyclerView.NO_POSITION) {
                focusMemoryManager.saveFocus(fragmentKey, currentView, position)
            }
        }
    }

    fun handleKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (event?.action == KeyEvent.ACTION_DOWN) {
            when (keyCode) {
                KeyEvent.KEYCODE_DPAD_DOWN -> {
                    if (binding.rvContent.hasFocus() || binding.rvContent.focusedChild != null) {
                        cycleToNextList()
                        return true
                    }
                }
                KeyEvent.KEYCODE_DPAD_UP -> {
                    if (binding.rvContent.hasFocus() || binding.rvContent.focusedChild != null) {
                        binding.root.findViewById<View>(R.id.btnPlay)?.requestFocus()
                        return true
                    }
                }
            }
        }
        return false
    }

    private fun setupUI() {
        // No dropdown setup needed
    }

    private fun setupAdapters() {
        contentAdapter = PosterAdapter(
            items = emptyList(),
            onClick = { item ->
                if (item.type == "movie" || item.type == "episode") {
                    updateDetailsPane(item)
                    binding.root.findViewById<View>(R.id.btnPlay)?.requestFocus()
                } else {
                    openDetails(item)
                }
            },
            onLongClick = { item ->
                val pos = contentAdapter.getItemPosition(item)
                val holder = binding.rvContent.findViewHolderForAdapterPosition(pos)
                if (holder != null) showItemMenu(holder.itemView, item)
            }
        )

        binding.rvContent.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        binding.rvContent.adapter = contentAdapter

        binding.rvContent.addOnChildAttachStateChangeListener(object : RecyclerView.OnChildAttachStateChangeListener {
            override fun onChildViewAttachedToWindow(view: View) {
                view.setOnFocusChangeListener { v, hasFocus ->
                    if (hasFocus) {
                        val position = binding.rvContent.getChildAdapterPosition(v)
                        if (position != RecyclerView.NO_POSITION) {
                            focusMemoryManager.saveFocus(fragmentKey, v, position)
                            val item = contentAdapter.getItem(position)
                            if (item != null) {
                                detailsUpdateJob?.cancel()
                                detailsUpdateJob = viewLifecycleOwner.lifecycleScope.launch {
                                    delay(300)
                                    if (isAdded) updateDetailsPane(item)
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

        binding.root.findViewById<View>(R.id.btnPlay)?.setOnClickListener {
            currentSelectedItem?.let { item -> showStreamDialog(item) }
        }
        binding.root.findViewById<View>(R.id.btnTrailer)?.setOnClickListener {
            currentSelectedItem?.let { item -> playTrailer(item) }
        }
        binding.root.findViewById<View>(R.id.btnRelated)?.setOnClickListener {
            currentSelectedItem?.let { item -> showRelatedContent(item) }
        }
    }

    private fun showItemMenu(view: View, item: MetaItem) {
        val wrapper = ContextThemeWrapper(requireContext(), android.R.style.Theme_DeviceDefault_Light_NoActionBar)
        val popup = PopupMenu(wrapper, view)

        viewLifecycleOwner.lifecycleScope.launch {
            val isInLibrary = viewModel.isItemInLibrarySync(item.id)

            if (isInLibrary) {
                popup.menu.add("Remove from Library")
            } else {
                popup.menu.add("Add to Library")
            }

            popup.menu.add("Mark as Watched")
            popup.menu.add("Not Watching")

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
                        true
                    }
                    "Not Watching" -> {
                        viewModel.markAsNotWatching(item)
                        true
                    }
                    else -> false
                }
            }
            popup.show()
        }
    }

    private fun loadHomeCatalogs() {
        currentCatalogs = viewModel.getHomeCatalogs()
        currentCatalogIndex = 0
        if (currentCatalogs.isNotEmpty()) {
            updateCurrentListLabel(currentCatalogs[0].displayName)
            viewModel.loadContentForCatalog(currentCatalogs[0], isInitialLoad = true)
        }
    }

    private fun updateCurrentListLabel(labelText: String) {
        val label = binding.root.findViewById<android.widget.TextView>(R.id.currentListLabel)
        label?.text = labelText
    }

    private fun cycleToNextList() {
        if (currentCatalogs.isEmpty()) return
        currentCatalogIndex = (currentCatalogIndex + 1) % currentCatalogs.size
        val nextCatalog = currentCatalogs[currentCatalogIndex]
        updateCurrentListLabel(nextCatalog.displayName)
        viewModel.loadContentForCatalog(nextCatalog, isInitialLoad = true)
    }

    private fun updateDetailsPane(item: MetaItem) {
        currentSelectedItem = item
        Glide.with(this).load(item.background ?: item.poster).into(binding.pageBackground)
        binding.detailTitle.text = item.name
        binding.detailTitle.visibility = View.VISIBLE
        binding.detailDescription.text = item.description ?: "No description available."
        val formattedDate = try { item.releaseDate?.let { java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).parse(it)?.let { d -> java.text.SimpleDateFormat("yyyy", java.util.Locale.getDefault()).format(d) } } } catch (e: Exception) { item.releaseDate }
        binding.detailDate.text = formattedDate ?: ""
        binding.detailDate.visibility = if (formattedDate.isNullOrEmpty()) View.GONE else View.VISIBLE
        if (item.type == "episode") {
            val parts = item.id.split(":")
            if (parts.size >= 4) {
                val season = parts[2]
                val episode = parts[3]
                binding.detailEpisode.text = "S${season.padStart(2, '0')}E${episode.padStart(2, '0')}"
                binding.detailEpisode.visibility = View.VISIBLE
            } else binding.detailEpisode.visibility = View.GONE
        } else binding.detailEpisode.visibility = View.GONE
        binding.detailRating.visibility = if (item.rating != null) {
            binding.detailRating.text = "★ ${item.rating}"
            View.VISIBLE
        } else View.GONE
        viewModel.fetchItemLogo(item)
        updateActorChips(item)
    }

    private fun updateActorChips(item: MetaItem) {
        val actorChipGroup = binding.root.findViewById<com.google.android.material.chip.ChipGroup>(R.id.actorChips)
        actorChipGroup?.removeAllViews()
        viewModel.fetchCast(item.id, item.type)
    }

    private fun openDetails(item: MetaItem) {
        val type = when {
            item.type == "episode" -> {
                val parts = item.id.split(":")
                if (parts.size >= 2) "series" else item.type
            }
            else -> item.type
        }
        val intent = Intent(requireContext(), DetailsActivity2::class.java).apply {
            putExtra("metaId", item.id)
            putExtra("title", item.name)
            putExtra("poster", item.poster)
            putExtra("background", item.background)
            putExtra("description", item.description)
            putExtra("type", type)
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
                android.widget.Toast.makeText(requireContext(), "No streams available", android.widget.Toast.LENGTH_SHORT).show()
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
                    android.widget.Toast.makeText(requireContext(), "No trailer available", android.widget.Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                android.widget.Toast.makeText(requireContext(), "Error loading trailer", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showRelatedContent(item: MetaItem) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val similarContent = viewModel.fetchSimilarContent(item.id, item.type)
                if (similarContent.isNotEmpty()) {
                    contentAdapter.updateData(similarContent)
                    updateDetailsPane(similarContent[0])
                    updateCurrentListLabel("Related to ${item.name}")
                } else {
                    android.widget.Toast.makeText(requireContext(), "No related content found", android.widget.Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                android.widget.Toast.makeText(requireContext(), "Error loading related content", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupObservers() {
        viewModel.currentCatalogContent.observe(viewLifecycleOwner) { items ->
            contentAdapter.updateData(items)
            if (items.isNotEmpty()) updateDetailsPane(items[0])
            else currentSelectedItem = null
        }
        viewModel.currentLogo.observe(viewLifecycleOwner) { logoUrl ->
            when (logoUrl) {
                "" -> { binding.detailTitle.visibility = View.GONE; binding.detailLogo.visibility = View.GONE }
                null -> { binding.detailTitle.visibility = View.VISIBLE; binding.detailLogo.visibility = View.GONE }
                else -> { binding.detailTitle.visibility = View.GONE; binding.detailLogo.visibility = View.VISIBLE; Glide.with(this).load(logoUrl).fitCenter().into(binding.detailLogo) }
            }
        }
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading -> binding.loadingCard.visibility = if (isLoading) View.VISIBLE else View.GONE }
        viewModel.castList.observe(viewLifecycleOwner) { castList ->
            val actorChipGroup = binding.root.findViewById<com.google.android.material.chip.ChipGroup>(R.id.actorChips)
            actorChipGroup?.removeAllViews()
            castList.take(5).forEach { actor ->
                val chip = com.google.android.material.chip.Chip(requireContext())
                chip.text = actor.name
                // ... set properties ...
                actorChipGroup?.addView(chip)
            }
        }
    }

    fun focusSidebar(): Boolean {
        binding.root.post {
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