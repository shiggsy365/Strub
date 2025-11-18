package com.example.stremiompvplayer.ui.details

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
// FIX: Use activity-ktx for viewModels
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.stremiompvplayer.PlayerActivity
import com.example.stremiompvplayer.R
import com.example.stremiompvplayer.adapters.StreamAdapter
import com.example.stremiompvplayer.databinding.ActivityDetails2Binding
// FIX: Use Meta from StremioModels, not the stub
import com.example.stremiompvplayer.models.Meta
// FIX: Use Stream from StremioModels
import com.example.stremiompvplayer.models.Stream
import com.example.stremiompvplayer.viewmodels.MainViewModel
import java.io.Serializable

/**
 * REFACTORED: This Activity no longer fetches its own data.
 * It observes the MainViewModel, which ensures data is loaded
 * using the correct user's authKey.
 */
class DetailsActivity2 : AppCompatActivity() {

    private lateinit var binding: ActivityDetails2Binding
    private lateinit var streamAdapter: StreamAdapter

    // FIX: Get the ViewModel
    private val viewModel: MainViewModel by viewModels()

    private var metaId: String? = null
    private var metaType: String? = null
    private var currentMeta: Meta? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetails2Binding.inflate(layoutInflater)
        setContentView(binding.root)

        metaId = intent.getStringExtra("id")
        metaType = intent.getStringExtra("type")

        if (metaId == null || metaType == null) {
            Log.e("DetailsActivity2", "No ID or Type provided")
            finish()
            return
        }

        setupRecyclerView()
        setupObservers()

        // FIX: Tell the ViewModel to fetch the data
        binding.progressBar.visibility = View.VISIBLE
        viewModel.getMeta(metaType!!, metaId!!)
        viewModel.getStreams(metaType!!, metaId!!)
    }

    private fun setupObservers() {
        // Observe Meta details
        viewModel.meta.observe(this) { meta ->
            if (meta?.id == metaId) { // Ensure this is the meta we requested
                currentMeta = meta
                updateUI(meta)
                binding.progressBar.visibility = View.GONE
                binding.contentGroup.visibility = View.VISIBLE
            } else if (meta == null && currentMeta != null) {
                // Meta was cleared (e.g., user changed)
            }
        }

        // Observe Streams
        viewModel.streams.observe(this) { streams ->
            // We can check if meta is loaded, but streams might load separately
            if (streams.isNotEmpty()) {
                streamAdapter.submitList(streams)
                binding.streamRecyclerView.visibility = View.VISIBLE
                binding.textNoStreams.visibility = View.GONE
            } else {
                streamAdapter.submitList(emptyList())
                binding.streamRecyclerView.visibility = View.GONE
                binding.textNoStreams.visibility = View.VISIBLE
            }
        }
    }

    private fun setupRecyclerView() {
        streamAdapter = StreamAdapter { stream ->
            onStreamClick(stream)
        }
        binding.streamRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@DetailsActivity2)
            adapter = streamAdapter
        }
    }

    private fun updateUI(meta: Meta) {
        binding.title.text = meta.name
        binding.description.text = meta.description
        Glide.with(this)
            .load(meta.poster)
            .placeholder(R.drawable.movie)
            .into(binding.poster)
    }

    private fun onStreamClick(stream: Stream) {
        val intent = Intent(this, PlayerActivity::class.java).apply {
            putExtra("stream", stream as Serializable)
            // Pass meta for context
            putExtra("meta", currentMeta as Serializable)
        }
        startActivity(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        // Tell the viewmodel to clear this data
        viewModel.clearMetaAndStreams()
    }
}