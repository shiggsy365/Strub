package com.example.stremiompvplayer.ui.library

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_library, container, false)

        prefsManager = SharedPreferencesManager.getInstance(requireContext())
        database = AppDatabase.getInstance(requireContext())

        recyclerView = view.findViewById(R.id.libraryRecycler)
        emptyText = view.findViewById(R.id.emptyText)

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
                val intent = Intent(requireContext(), DetailsActivity2::class.java)
                intent.putExtra("metaId", libraryItem.id)
                intent.putExtra("title", libraryItem.name)
                intent.putExtra("poster", libraryItem.poster)
                intent.putExtra("type", libraryItem.type)
                startActivity(intent)
            },
            onLongClick = { libraryItem ->
                // TODO: Show options dialog (remove from library, etc.)
            }
        )

        recyclerView.layoutManager = GridLayoutManager(requireContext(), 3)
        recyclerView.adapter = adapter
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