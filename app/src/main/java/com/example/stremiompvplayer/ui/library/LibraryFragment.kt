package com.example.stremiompvplayer.ui.library

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.stremiompvplayer.DetailsActivity2
import com.example.stremiompvplayer.R
import com.example.stremiompvplayer.adapters.LibraryAdapter
import com.example.stremiompvplayer.data.AppDatabase
import com.example.stremiompvplayer.utils.SharedPreferencesManager
import com.example.stremiompvplayer.models.LibraryItem // NEW IMPORT: Required for adapter list type
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers

class LibraryFragment : Fragment() {

    // CHANGED: Restored AppDatabase for fetching content
    private lateinit var database: AppDatabase
    private lateinit var prefsManager: SharedPreferencesManager
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyText: TextView
    private lateinit var adapter: LibraryAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_library, container, false)

        // FIXED: Initialize both managers
        prefsManager = SharedPreferencesManager.getInstance(requireContext())
        database = AppDatabase.getInstance(requireContext())

        // NOTE: If you are using Data Binding, replace findViewById with binding.viewName
        recyclerView = view.findViewById(R.id.libraryRecycler)
        emptyText = view.findViewById(R.id.emptyText)

        setupRecyclerView()
        loadLibrary()

        return view
    }

    override fun onResume() {
        super.onResume()
        loadLibrary() // Refresh when returning to this fragment
    }

    private fun setupRecyclerView() {
        // FIX: REMOVE the 'items = emptyList()' parameter.
        // The adapter only accepts the two function arguments.
        adapter = LibraryAdapter(
            onClick = { libraryItem ->
                val intent = Intent(requireContext(), DetailsActivity2::class.java)
                intent.putExtra("metaId", libraryItem.id)
                intent.putExtra("title", libraryItem.name)
                intent.putExtra("poster", libraryItem.poster)
                startActivity(intent)
            },
            onLongClick = { libraryItem ->
                // Add required logic here later
            }
        ) // <-- The adapter is constructed successfully here!

        recyclerView.layoutManager = GridLayoutManager(requireContext(), 3)
        recyclerView.adapter = adapter
    }

    private fun loadLibrary() {
        val userId = prefsManager.getCurrentUserId()
        if (userId == null) {
            showEmpty("Please select a user")
            return
        }

        // NEW: Launch the database query in a background coroutine
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {

            // Database access runs safely on the IO thread
            val libraryItems = database.getLibraryItems(userId)

            // Switch back to the Main thread to update the UI
            withContext(Dispatchers.Main) {
                if (libraryItems.isEmpty()) {
                    showEmpty("Your library is empty.\nAdd content from Discover, Movies, or Series.")
                } else {
                    // This resolves adapter.setItems(...)
                    adapter.setItems(libraryItems)
                    recyclerView.visibility = View.VISIBLE
                    emptyText.visibility = View.GONE
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