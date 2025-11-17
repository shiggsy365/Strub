package com.example.stremiompvplayer.ui.library

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.stremiompvplayer.R
import com.example.stremiompvplayer.adapters.LibraryAdapter
import com.example.stremiompvplayer.data.AppDatabase

class LibraryFragment : Fragment() {

    private lateinit var database: AppDatabase
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyText: TextView
    private lateinit var adapter: LibraryAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_library, container, false)

        database = AppDatabase.Companion.getInstance(requireContext())

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
        adapter = LibraryAdapter { libraryItem ->
            val intent = Intent(requireContext(), DetailsActivity2::class.java)
            intent.putExtra("META_ID", libraryItem.metaId)
            intent.putExtra("META_TYPE", libraryItem.type)
            intent.putExtra("META_NAME", libraryItem.name)
            startActivity(intent)
        }

        recyclerView.layoutManager = GridLayoutManager(requireContext(), 3)
        recyclerView.adapter = adapter
    }

    private fun loadLibrary() {
        val userId = database.getCurrentUserId()
        if (userId == null) {
            showEmpty("Please select a user")
            return
        }

        val libraryItems = database.getLibraryItems(userId)

        if (libraryItems.isEmpty()) {
            showEmpty("Your library is empty.\nAdd content from Discover, Movies, or Series.")
        } else {
            adapter.setItems(libraryItems)
            recyclerView.visibility = View.VISIBLE
            emptyText.visibility = View.GONE
        }
    }

    private fun showEmpty(message: String) {
        recyclerView.visibility = View.GONE
        emptyText.visibility = View.VISIBLE
        emptyText.text = message
    }
}