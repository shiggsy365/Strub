package com.example.stremiompvplayer.ui.search

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.stremiompvplayer.R
import com.example.stremiompvplayer.adapters.PosterAdapter
import com.example.stremiompvplayer.databinding.FragmentSearchResultsBinding
import com.example.stremiompvplayer.models.MetaItem
import com.example.stremiompvplayer.viewmodels.MainViewModel
import com.example.stremiompvplayer.viewmodels.MainViewModelFactory
import com.example.stremiompvplayer.data.ServiceLocator
import com.example.stremiompvplayer.utils.SharedPreferencesManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SearchResultsFragment : Fragment() {

    private var _binding: FragmentSearchResultsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MainViewModel by activityViewModels {
        MainViewModelFactory(
            ServiceLocator.getInstance(requireContext()),
            SharedPreferencesManager.getInstance(requireContext())
        )
    }

    private lateinit var contentAdapter: PosterAdapter
    private var currentSelectedItem: MetaItem? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSearchResultsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupAdapters()
        setupObservers()
        setupKeyHandling()
    }

    private fun setupAdapters() {
        contentAdapter = PosterAdapter(
            items = emptyList(),
            onClick = { item -> onContentClicked(item) },
            onLongClick = { /* Optionally implement long click menu */ }
        )
        binding.rvContent.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        binding.rvContent.adapter = contentAdapter

        binding.rvContent.addOnChildAttachStateChangeListener(object : androidx.recyclerview.widget.RecyclerView.OnChildAttachStateChangeListener {
            override fun onChildViewAttachedToWindow(view: View) {
                view.setOnFocusChangeListener { v, hasFocus ->
                    if (hasFocus) {
                        val position = binding.rvContent.getChildAdapterPosition(v)
                        if (position != androidx.recyclerview.widget.RecyclerView.NO_POSITION) {
                            val item = contentAdapter.getItem(position)
                            if (item != null) {
                                viewLifecycleOwner.lifecycleScope.launch {
                                    delay(300)
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

        binding.btnPlay.setOnClickListener {
            currentSelectedItem?.let { item ->
                // Implement play logic
                Toast.makeText(requireContext(), "Play ${item.name}", Toast.LENGTH_SHORT).show()
            }
        }
        binding.btnTrailer.setOnClickListener {
            currentSelectedItem?.let { item ->
                // Implement trailer logic
                Toast.makeText(requireContext(), "Trailer for ${item.name}", Toast.LENGTH_SHORT).show()
            }
        }
        binding.btnRelated.setOnClickListener {
            currentSelectedItem?.let { item ->
                // Implement related logic
                Toast.makeText(requireContext(), "Related to ${item.name}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupObservers() {
        viewModel.searchResults.observe(viewLifecycleOwner) { results ->
            binding.currentListLabel.text = "Search Results"
            contentAdapter.updateData(results)
            if (results.isNotEmpty()) {
                binding.emptyText.visibility = View.GONE
                binding.rvContent.visibility = View.VISIBLE
                updateDetailsPane(results[0])
            } else {
                binding.emptyText.visibility = View.VISIBLE
                binding.rvContent.visibility = View.GONE
            }
        }
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
        // Optionally observe cast, logo, etc. as in LibraryFragment
    }

    private fun setupKeyHandling() {
        binding.rvContent.isFocusable = true
        binding.rvContent.isFocusableInTouchMode = true
        binding.root.setOnKeyListener { _, keyCode, event ->
            if (event.action == android.view.KeyEvent.ACTION_DOWN) {
                when (keyCode) {
                    android.view.KeyEvent.KEYCODE_DPAD_DOWN -> {
                        val focusedChild = binding.rvContent.focusedChild
                        if (focusedChild != null) {
                            // Optionally cycle to next list if you support multiple lists
                            return@setOnKeyListener true
                        }
                        false
                    }
                    android.view.KeyEvent.KEYCODE_DPAD_UP -> {
                        val btnPlay = binding.btnPlay
                        val btnRelated = binding.btnRelated
                        when {
                            btnPlay.visibility == View.VISIBLE -> btnPlay.requestFocus()
                            btnRelated.visibility == View.VISIBLE -> btnRelated.requestFocus()
                            else -> btnPlay.requestFocus()
                        }
                        return@setOnKeyListener true
                    }
                    else -> false
                }
            } else {
                false
            }
        }
    }

    private fun onContentClicked(item: MetaItem) {
        updateDetailsPane(item)
        // Optionally implement drill-down navigation
    }

    private fun updateDetailsPane(item: MetaItem) {
        currentSelectedItem = item
        Glide.with(this)
            .load(item.background ?: item.poster)
            .into(binding.pageBackground)

        binding.detailTitle.text = item.name
        binding.detailDate.text = item.releaseDate ?: ""
        binding.detailRating.text = item.rating?.let { "★ $it" } ?: ""
        binding.detailDescription.text = item.description ?: "No description available."
        binding.detailEpisode.visibility = if (item.type == "episode") View.VISIBLE else View.GONE
        binding.detailEpisode.text = if (item.type == "episode") {
            val parts = item.id.split(":")
            if (parts.size >= 4) {
                val season = parts[2]
                val episode = parts[3]
                "S${season.padStart(2, '0')}E${episode.padStart(2, '0')}"
            } else ""
        } else ""
        binding.detailLogo.visibility = View.GONE
        // Optionally update actor chips, logo, etc.
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
