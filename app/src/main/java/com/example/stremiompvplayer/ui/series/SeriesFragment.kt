package com.example.stremiompvplayer.ui.series

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.stremiompvplayer.databinding.FragmentDiscoverBinding // REUSE: DiscoverFragment's layout
import com.example.stremiompvplayer.models.MetaItem
import com.example.stremiompvplayer.DetailsActivity2
import com.example.stremiompvplayer.ui.discover.DiscoverSectionAdapter // REUSE: DiscoverFragment's adapter
import com.example.stremiompvplayer.viewmodels.MainViewModel

/**
 * REWRITTEN: This fragment no longer fetches its own data.
 * It observes the ViewModel's `seriesSections` LiveData, which is
 * a filtered version of the main `discoverSections` list.
 */
class SeriesFragment : Fragment() {

    private var _binding: FragmentDiscoverBinding? = null
    private val binding get() = _binding!!

    // Get the SAME ViewModel instance as the Activity and other fragments
    private val viewModel: MainViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // We can reuse the same layout file as DiscoverFragment
        _binding = FragmentDiscoverBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // We can reuse the same adapter as DiscoverFragment
        val sectionAdapter = DiscoverSectionAdapter { metaItem ->
            onPosterClick(metaItem)
        }

        binding.discoverRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = sectionAdapter
        }

        // The ViewModel should be fetched by the DiscoverFragment (or MainActivity)
        // We just observe the result.

        // NEW: Observe the `seriesSections` filtered list
        viewModel.seriesSections.observe(viewLifecycleOwner) { seriesSections ->
            if (seriesSections.isNotEmpty()) {
                binding.progressBar.visibility = View.GONE
                binding.discoverRecyclerView.visibility = View.VISIBLE
                sectionAdapter.submitList(seriesSections)
            } else {
                // You could show "No series found"
                binding.progressBar.visibility = View.GONE
                binding.discoverRecyclerView.visibility = View.GONE
            }
        }
    }

    private fun onPosterClick(metaItem: MetaItem) {
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