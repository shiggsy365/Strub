package com.example.stremiompvplayer.ui.series

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.stremiompvplayer.R
import com.example.stremiompvplayer.data.AppDatabase
import com.example.stremiompvplayer.models.MetaPreview
import com.example.stremiompvplayer.network.StremioClient
import com.example.stremiompvplayer.ui.discover.DiscoverSectionAdapter
import kotlinx.coroutines.launch

class SeriesFragment : Fragment() {

    private lateinit var database: AppDatabase
    private lateinit var stremioClient: StremioClient
    private lateinit var recyclerView: RecyclerView
    private lateinit var loadingProgress: ProgressBar
    private lateinit var emptyText: TextView
    private lateinit var adapter: DiscoverSectionAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_discover, container, false)

        database = AppDatabase.Companion.getInstance(requireContext())
        stremioClient = StremioClient()

        recyclerView = view.findViewById(R.id.discoverRecycler)
        loadingProgress = view.findViewById(R.id.loadingProgress)
        emptyText = view.findViewById(R.id.emptyText)

        setupRecyclerView()
        loadContent()

        return view
    }

    private fun setupRecyclerView() {
        adapter = DiscoverSectionAdapter { meta ->
            openDetails(meta)
        }
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter
    }

    private fun loadContent() {
        val userId = database.getCurrentUserId()
        if (userId == null) {
            showEmpty("Please select a user")
            return
        }

        val addonUrls = database.getUserAddonUrls(userId)
        if (addonUrls.isEmpty()) {
            showEmpty("No addons configured. Add addons in Settings.")
            return
        }

        loadingProgress.visibility = View.VISIBLE
        recyclerView.visibility = View.GONE
        emptyText.visibility = View.GONE

        lifecycleScope.launch {
            try {
                val sections = mutableListOf<DiscoverSection>()

                for (addonUrl in addonUrls) {
                    try {
                        val manifest = stremioClient.getManifest(addonUrl)
                        if (manifest != null && manifest.catalogs != null) {
                            for (catalog in manifest.catalogs) {
                                // Only load series catalogs
                                if (catalog.type == "series") {
                                    val catalogResponse = stremioClient.getCatalog(
                                        addonUrl,
                                        catalog.type,
                                        catalog.id
                                    )

                                    if (catalogResponse != null && catalogResponse.metas.isNotEmpty()) {
                                        sections.add(
                                            DiscoverSection(
                                                title = "${manifest.name} - ${catalog.name}",
                                                items = catalogResponse.metas,
                                                addonUrl = addonUrl,
                                                catalogId = catalog.id,
                                                type = catalog.type
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("SeriesFragment", "Error loading addon: $addonUrl", e)
                    }
                }

                if (sections.isEmpty()) {
                    showEmpty("No series available from configured addons")
                } else {
                    adapter.setSections(sections)
                    loadingProgress.visibility = View.GONE
                    recyclerView.visibility = View.VISIBLE
                }
            } catch (e: Exception) {
                Log.e("SeriesFragment", "Error loading content", e)
                showEmpty("Error loading content: ${e.message}")
            }
        }
    }

    private fun showEmpty(message: String) {
        loadingProgress.visibility = View.GONE
        recyclerView.visibility = View.GONE
        emptyText.visibility = View.VISIBLE
        emptyText.text = message
    }

    private fun openDetails(meta: MetaPreview) {
        val intent = Intent(requireContext(), DetailsActivity2::class.java)
        intent.putExtra("META_ID", meta.id)
        intent.putExtra("META_TYPE", meta.type)
        intent.putExtra("META_NAME", meta.name)
        startActivity(intent)
    }
}