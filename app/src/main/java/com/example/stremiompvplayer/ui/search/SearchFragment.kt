package com.example.stremiompvplayer.ui.search

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.example.stremiompvplayer.adapters.PosterAdapter
import com.example.stremiompvplayer.data.AppDatabase
import com.example.stremiompvplayer.databinding.FragmentSearchBinding
import com.example.stremiompvplayer.models.MetaPreview
import com.example.stremiompvplayer.network.StremioClient
import kotlinx.coroutines.launch

class SearchFragment : Fragment() {

    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!

    private lateinit var database: AppDatabase
    private lateinit var stremioClient: StremioClient
    private lateinit var adapter: PosterAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        database = AppDatabase.Companion.getInstance(requireContext())
        stremioClient = StremioClient()

        setupRecyclerView()
        setupSearch()
    }

    private fun setupRecyclerView() {
        adapter = PosterAdapter { meta ->
            openDetails(meta)
        }
        binding.searchResults.layoutManager = GridLayoutManager(requireContext(), 3)
        binding.searchResults.adapter = adapter
    }

    private fun setupSearch() {
        binding.searchButton.setOnClickListener {
            val query = binding.searchInput.text.toString().trim()
            if (query.isNotEmpty()) {
                performSearch(query)
            } else {
                Toast.makeText(requireContext(), "Please enter a search term", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun performSearch(query: String) {
        val userId = database.getCurrentUserId()
        if (userId == null) {
            Toast.makeText(requireContext(), "Please select a user", Toast.LENGTH_SHORT).show()
            return
        }

        val addonUrls = database.getUserAddonUrls(userId)
        if (addonUrls.isEmpty()) {
            Toast.makeText(requireContext(), "No addons configured", Toast.LENGTH_SHORT).show()
            return
        }

        binding.loadingProgress.visibility = View.VISIBLE
        binding.searchResults.visibility = View.GONE
        binding.emptyText.visibility = View.GONE

        lifecycleScope.launch {
            try {
                val results = mutableListOf<MetaPreview>()

                for (addonUrl in addonUrls) {
                    try {
                        val manifest = stremioClient.getManifest(addonUrl)
                        if (manifest != null && manifest.catalogs != null) {
                            for (catalog in manifest.catalogs) {
                                // Search in catalog with search extra
                                val catalogResponse = stremioClient.getCatalog(
                                    addonUrl,
                                    catalog.type,
                                    catalog.id,
                                    mapOf("search" to query)
                                )

                                if (catalogResponse != null) {
                                    results.addAll(catalogResponse.metas)
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("SearchFragment", "Error searching addon: $addonUrl", e)
                    }
                }

                binding.loadingProgress.visibility = View.GONE

                if (results.isEmpty()) {
                    binding.emptyText.visibility = View.VISIBLE
                    binding.emptyText.text = "No results found for \"$query\""
                } else {
                    adapter.setItems(results)
                    binding.searchResults.visibility = View.VISIBLE
                }
            } catch (e: Exception) {
                Log.e("SearchFragment", "Error performing search", e)
                binding.loadingProgress.visibility = View.GONE
                binding.emptyText.visibility = View.VISIBLE
                binding.emptyText.text = "Error: ${e.message}"
            }
        }
    }

    private fun openDetails(meta: MetaPreview) {
        val intent = Intent(requireContext(), DetailsActivity2::class.java)
        intent.putExtra("META_ID", meta.id)
        intent.putExtra("META_TYPE", meta.type)
        intent.putExtra("META_NAME", meta.name)
        startActivity(intent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}