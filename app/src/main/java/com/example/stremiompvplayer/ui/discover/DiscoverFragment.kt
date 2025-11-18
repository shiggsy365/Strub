package com.example.stremiompvplayer.ui.discover

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.stremiompvplayer.DetailsActivity2
import com.example.stremiompvplayer.databinding.FragmentDiscoverBinding
import com.example.stremiompvplayer.models.MetaItem
import com.example.stremiompvplayer.viewmodels.MainViewModel

class DiscoverFragment : Fragment() {

    private var _binding: FragmentDiscoverBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MainViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDiscoverBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val sectionAdapter = DiscoverSectionAdapter { metaItem ->
            // Handle click on a poster
            onPosterClick(metaItem)
        }

        binding.discoverRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = sectionAdapter
        }

        // REMOVED: viewModel.fetchCatalogs()
        // This is now triggered by the Activity calling viewModel.setCurrentUser(authKey)
        // which ensures we have a user before fetching their specific catalogs.

        // NEW: Observe the new LiveData
        viewModel.discoverSections.observe(viewLifecycleOwner) { sections ->
            if (sections.isNotEmpty()) {
                binding.progressBar.visibility = View.GONE
                binding.discoverRecyclerView.visibility = View.VISIBLE
                sectionAdapter.submitList(sections)
            } else {
                // You could show an error message here
                binding.progressBar.visibility = View.GONE
            }
        }

        // OLD: Remove observation of the old feedList
        /*
        viewModel.feedList.observe(viewLifecycleOwner) { feedList ->
            if (feedList.isNotEmpty()) {
                binding.progressBar.visibility = View.GONE
                binding.discoverRecyclerView.visibility = View.VISIBLE
                sectionAdapter.submitList(feedList)
            }
        }
        */

        // OLD: Remove the call to getFeed()
        // viewModel.getFeed()
    }

    private fun onPosterClick(metaItem: MetaItem) {
        val intent = Intent(activity, DetailsActivity2::class.java).apply {
            // Assuming DetailsActivity2 is the correct destination
            // You will need to update DetailsActivity2 to fetch meta/streams
            // using the add-on logic if it relies on the authKey
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