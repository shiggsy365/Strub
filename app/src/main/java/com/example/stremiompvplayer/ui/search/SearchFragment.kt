package com.example.stremiompvplayer.ui.search

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.widget.SearchView // FIX: Use androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels // NEW: Use ViewModel
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.stremiompvplayer.adapters.ContentAdapter
import com.example.stremiompvplayer.databinding.FragmentSearchBinding
import com.example.stremiompvplayer.models.Manifest
import com.example.stremiompvplayer.models.MetaItem // NEW: Use MetaItem
import com.example.stremiompvplayer.models.MetaPreview // Still need this for adapter
import com.example.stremiompvplayer.network.StremioClient
import com.example.stremiompvplayer.ui.details.DetailsActivity2
import com.example.stremiompvplayer.viewmodels.MainViewModel // NEW: Use ViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SearchFragment : Fragment() {

    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!

    // The adapter uses MetaPreview, but the new API gives MetaItem.
    // We will convert MetaItem -> MetaPreview.
    private lateinit var searchAdapter: ContentAdapter

    // NEW: Get the ViewModel
    private val viewModel: MainViewModel by activityViewModels()

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

        setupRecyclerView()

        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.let {
                    // TODO: The ViewModel should have a search function that does both
                    // For now, this is still mixed logic, but it fixes the build errors
                    searchStremio(it) // This is the old API search
                    searchAddon(it)   // This is the new addon search
                }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                return true
            }
        })
    }

    private fun setupRecyclerView() {
        // The adapter expects MetaPreview
        searchAdapter = ContentAdapter(emptyList()) { item ->
            onPosterClick(item)
        }
        binding.searchRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = searchAdapter
        }
    }

    // This function should be moved to the ViewModel
    private fun searchStremio(query: String) {
        // FIX: Call StremioClient.api, not StremioClient()
        val stremioApi = StremioClient.api
        // TODO: This should be in ViewModel
        CoroutineScope(Dispatchers.Main).launch {
            try {
                // This original search uses authKey and FeedList, which is different from MetaPreview
                // We will need to adapt this or change the adapter
                // For now, let's just log it
                // We need to get the authKey from the ViewModel
                // val results = stremioApi.search(authKey, query)
                // Log.d("SearchFragment", "Stremio search results: ${results.size}")
                Log.d("SearchFragment", "Stremio search (authKey) is not implemented in ViewModel yet.")
            } catch (e: Exception) {
                Log.e("SearchFragment", "Stremio search error", e)
            }
        }
    }

    // This function should also be moved to the ViewModel
    // It's still making network calls, which is what the error log told us.
    private fun searchAddon(query: String) {
        val stremioApi = StremioClient.api
        // TODO: Get manifest URLs from ViewModel (which gets from SharedPreferences)
        // This is just a placeholder and will not work correctly.
        // It needs to get the *list* of manifests from the ViewModel.
        val manifestUrl =
            "http://10.0.2.2:7878/manifest.json" // Placeholder: This is incorrect logic
        val baseUrl = manifestUrl.substringBeforeLast("/")

        CoroutineScope(Dispatchers.Main).launch {
            try {
                // FIX: Removed Unresolved reference: getManifest
                val manifest = withContext(Dispatchers.IO) {
                    stremioApi.getManifest(manifestUrl)
                }

                // Find a search catalog if it exists
                val searchCatalog = manifest.catalogs.find { it.id == "search" }
                if (searchCatalog != null) {
                    val searchUrl =
                        "$baseUrl/catalog/${searchCatalog.type}/${searchCatalog.id}/search=$query.json"
                    // FIX: Removed Unresolved reference: getCatalog
                    val catalogResponse = withContext(Dispatchers.IO) {
                        stremioApi.getCatalog(searchUrl)
                    }
                    // We need to convert MetaItem to MetaPreview
                    // FIX: Use MetaPreview constructor
                    val metaPreviews = catalogResponse.metas.map {
                        MetaPreview(it.id, it.type, it.name, it.poster)
                    }
                    searchAdapter.updateData(metaPreviews)
                } else {
                    Toast.makeText(context, "This addon doesn't support search", Toast.LENGTH_SHORT)
                        .show()
                }

            } catch (e: Exception) {
                Log.e("SearchFragment", "Addon search error", e)
                Toast.makeText(context, "Addon search failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun onPosterClick(metaItem: MetaPreview) {
        val intent = Intent(activity, DetailsActivity2::class.java).apply {
            putExtra("id", metaItem.id)
            putExtra("type", metaItem.type)
        }
        startActivity(intent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}