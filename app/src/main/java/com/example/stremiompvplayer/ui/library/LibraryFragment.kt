package com.example.stremiompvplayer.ui.library

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager // Changed to LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.stremiompvplayer.DetailsActivity2
import com.example.stremiompvplayer.R
import com.example.stremiompvplayer.adapters.LibraryAdapter
import com.example.stremiompvplayer.data.AppDatabase
import com.example.stremiompvplayer.models.LibraryItem
import com.example.stremiompvplayer.utils.SharedPreferencesManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LibraryFragment : Fragment() {

    private lateinit var database: AppDatabase
    private lateinit var prefsManager: SharedPreferencesManager
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyText: TextView
    private lateinit var adapter: LibraryAdapter

    // New Views for the Movie-style layout
    private lateinit var titleView: TextView
    private lateinit var descriptionView: TextView
    private lateinit var selectedPosterView: ImageView
    private lateinit var backgroundView: ImageView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // IMPORTANT: Ensure you are using the NEW layout file provided above
        val view = inflater.inflate(R.layout.fragment_library, container, false)

        prefsManager = SharedPreferencesManager.getInstance(requireContext())
        database = AppDatabase.getInstance(requireContext())

        recyclerView = view.findViewById(R.id.libraryRecycler)
        emptyText = view.findViewById(R.id.emptyText)

        // Initialize new views
        titleView = view.findViewById(R.id.libraryTitle)
        descriptionView = view.findViewById(R.id.libraryDescription)
        selectedPosterView = view.findViewById(R.id.selectedPoster)
        backgroundView = view.findViewById(R.id.backgroundImage)

        setupRecyclerView()
        loadLibrary()

        return view
    }

    override fun onResume() {
        super.onResume()
        loadLibrary()
    }

    private fun setupRecyclerView() {
        adapter = LibraryAdapter(
            onClick = { libraryItem ->
                // CHANGE: Click now updates the UI details instead of launching immediately
                updateDetailsUI(libraryItem)

                // Optional: You might want a double-click or a separate "Play" button
                // to actually launch the details/player, similar to the Movies stream selection.
                // For now, we just populate the view as requested.
            },
            onLongClick = { libraryItem ->
                // TODO: Show options dialog (remove from library, etc.)
            }
        )

        // CHANGE: Use Horizontal Layout Manager to match Movies Page
        recyclerView.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        recyclerView.adapter = adapter
    }

    private fun updateDetailsUI(item: LibraryItem) {
        titleView.text = item.name
        descriptionView.text = "Type: ${item.type}" // LibraryItem might not have full desc, using Type or placeholder

        if (!item.poster.isNullOrEmpty()) {
            Glide.with(this).load(item.poster).into(selectedPosterView)
            Glide.with(this).load(item.poster).into(backgroundView)
        } else {
            selectedPosterView.setImageResource(R.drawable.movie)
            backgroundView.setImageResource(R.drawable.movie)
        }
    }

    private fun loadLibrary() {
        val userId = prefsManager.getCurrentUserId()
        if (userId == null) {
            showEmpty("Please select a user")
            return
        }

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                val libraryItems = database.getLibraryItems(userId)

                withContext(Dispatchers.Main) {
                    if (libraryItems.isEmpty()) {
                        showEmpty("Your library is empty.\nAdd content by long-pressing items.")
                    } else {
                        adapter.setItems(libraryItems)
                        recyclerView.visibility = View.VISIBLE
                        emptyText.visibility = View.GONE

                        // CHANGE: Default Choice - Item 1
                        if (libraryItems.isNotEmpty()) {
                            updateDetailsUI(libraryItems[0])
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showEmpty("Error loading library: ${e.message}")
                }
            }
        }
    }

    private fun showEmpty(message: String) {
        recyclerView.visibility = View.GONE
        emptyText.visibility = View.VISIBLE
        emptyText.text = message
    }
}