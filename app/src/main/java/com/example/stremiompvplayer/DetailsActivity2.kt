package com.example.stremiompvplayer

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.stremiompvplayer.adapters.StreamAdapter
import com.example.stremiompvplayer.data.AppDatabase
import com.example.stremiompvplayer.databinding.ActivityDetails2Binding
import com.example.stremiompvplayer.models.LibraryItem
import com.example.stremiompvplayer.models.MetaDetail
import com.example.stremiompvplayer.models.Stream
import com.example.stremiompvplayer.network.StremioClient
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import java.util.UUID

class DetailsActivity2 : AppCompatActivity() {

    private lateinit var binding: ActivityDetails2Binding
    private lateinit var database: AppDatabase
    private lateinit var stremioClient: StremioClient

    private var metaId: String? = null
    private var metaType: String? = null
    private var currentMeta: MetaDetail? = null
    private var availableStreams = listOf<Stream>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetails2Binding.Companion.inflate(layoutInflater)
        setContentView(binding.root)

        database = AppDatabase.Companion.getInstance(this)
        stremioClient = StremioClient()

        metaId = intent.getStringExtra("META_ID")
        metaType = intent.getStringExtra("META_TYPE")

        setupToolbar()
        loadDetails()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun loadDetails() {
        val id = metaId
        val type = metaType
        val userId = database.getCurrentUserId()

        if (id == null || type == null || userId == null) {
            Toast.makeText(this, "Invalid content", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        binding.loadingProgress.visibility = View.VISIBLE
        binding.contentLayout.visibility = View.GONE

        lifecycleScope.launch {
            try {
                val addonUrls = database.getUserAddonUrls(userId)
                var meta: MetaDetail? = null

                // Try to load metadata from each addon
                for (addonUrl in addonUrls) {
                    try {
                        val result = stremioClient.getMeta(addonUrl, type, id)
                        if (result != null) {
                            meta = result
                            break
                        }
                    } catch (e: Exception) {
                        Log.e("DetailsActivity2", "Error loading from $addonUrl", e)
                    }
                }

                if (meta != null) {
                    currentMeta = meta
                    displayDetails(meta)
                    loadStreams(id, type, addonUrls)
                } else {
                    Toast.makeText(this@DetailsActivity2, "Content not found", Toast.LENGTH_SHORT).show()
                    finish()
                }
            } catch (e: Exception) {
                Log.e("DetailsActivity2", "Error loading details", e)
                Toast.makeText(this@DetailsActivity2, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun displayDetails(meta: MetaDetail) {
        binding.apply {
            titleText.text = meta.name
            descriptionText.text = meta.description ?: "No description available"
            releaseInfoText.text = buildString {
                meta.releaseInfo?.let { append(it) }
                if (meta.runtime != null && meta.releaseInfo != null) append(" • ")
                meta.runtime?.let { append(it) }
            }

            // Load poster
            posterImage.load(meta.poster) {
                crossfade(true)
                placeholder(R.drawable.default_background)
                error(R.drawable.default_background)
            }

            // Load background
            backgroundImage.load(meta.background) {
                crossfade(true)
                error(R.drawable.default_background)
            }

            // Show genres
            if (!meta.genres.isNullOrEmpty()) {
                genresText.text = meta.genres.joinToString(" • ")
                genresText.visibility = View.VISIBLE
            }

            // Show rating
            if (meta.imdbRating != null) {
                ratingText.text = "★ ${meta.imdbRating}"
                ratingText.visibility = View.VISIBLE
            }

            // Check if in library
            val userId = database.getCurrentUserId()
            if (userId != null) {
                val inLibrary = database.isInLibrary(userId, meta.id)
                updateLibraryButton(inLibrary)

                libraryButton.setOnClickListener {
                    toggleLibrary(userId, meta, inLibrary)
                }
            }

            loadingProgress.visibility = View.GONE
            contentLayout.visibility = View.VISIBLE
        }
    }

    private fun loadStreams(id: String, type: String, addonUrls: List<String>) {
        lifecycleScope.launch {
            val streams = mutableListOf<Stream>()

            for (addonUrl in addonUrls) {
                try {
                    val addonStreams = stremioClient.getStreams(addonUrl, type, id)
                    streams.addAll(addonStreams)
                } catch (e: Exception) {
                    Log.e("DetailsActivity2", "Error loading streams from $addonUrl", e)
                }
            }

            availableStreams = streams

            if (streams.isNotEmpty()) {
                binding.playButton.isEnabled = true
                binding.playButton.setOnClickListener {
                    if (streams.size == 1) {
                        playStream(streams[0])
                    } else {
                        showStreamSelectionDialog(streams)
                    }
                }

                // Auto-play if enabled
                val userId = database.getCurrentUserId()
                if (userId != null) {
                    val settings = database.getUserSettings(userId)
                    if (settings.autoPlayFirstStream) {
                        playStream(streams[0])
                    }
                }
            } else {
                binding.playButton.isEnabled = false
                Toast.makeText(this@DetailsActivity2, "No streams available", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showStreamSelectionDialog(streams: List<Stream>) {
        val view = layoutInflater.inflate(R.layout.dialog_stream_selection, null)
        val recyclerView = view.findViewById<RecyclerView>(R.id.streamsRecyclerView)

        val adapter = StreamAdapter(streams) { stream ->
            playStream(stream)
        }

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        MaterialAlertDialogBuilder(this)
            .setView(view)
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun playStream(stream: Stream) {
        val url = stream.url ?: stream.externalUrl
        if (url != null) {
            val intent = Intent(this, PlayerActivity::class.java)
            intent.putExtra("STREAM_URL", url)
            intent.putExtra("STREAM_TITLE", currentMeta?.name ?: "Playing")
            startActivity(intent)
        } else {
            Toast.makeText(this, "Invalid stream URL", Toast.LENGTH_SHORT).show()
        }
    }

    private fun toggleLibrary(userId: String, meta: MetaDetail, currentlyInLibrary: Boolean) {
        if (currentlyInLibrary) {
            database.removeFromLibrary(userId, meta.id)
            updateLibraryButton(false)
            Toast.makeText(this, "Removed from library", Toast.LENGTH_SHORT).show()
        } else {
            val libraryItem = LibraryItem(
                id = UUID.randomUUID().toString(),
                userId = userId,
                metaId = meta.id,
                type = meta.type,
                name = meta.name,
                poster = meta.poster,
                background = meta.background,
                genres = meta.genres
            )
            database.addToLibrary(userId, libraryItem)
            updateLibraryButton(true)
            Toast.makeText(this, "Added to library", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateLibraryButton(inLibrary: Boolean) {
        binding.libraryButton.text = if (inLibrary) "Remove from Library" else "Add to Library"
    }
}