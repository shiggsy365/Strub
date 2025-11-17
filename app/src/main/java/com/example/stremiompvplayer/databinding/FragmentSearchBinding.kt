package com.example.stremiompvplayer.databinding

import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.stremiompvplayer.R
import com.google.android.material.button.MaterialButton

class FragmentSearchBinding private constructor(
    val root: LinearLayout,
    val searchInput: EditText,
    val searchButton: MaterialButton,
    val searchResults: RecyclerView,
    val loadingProgress: ProgressBar,
    val emptyText: TextView
) {
    companion object {
        fun inflate(
            layoutInflater: android.view.LayoutInflater,
            parent: android.view.ViewGroup?,
            attachToParent: Boolean
        ): FragmentSearchBinding {
            val root = layoutInflater.inflate(R.layout.fragment_search, parent, attachToParent) as LinearLayout
            return bind(root)
        }

        fun bind(view: View): FragmentSearchBinding {
            val searchInput = view.findViewById<EditText>(R.id.searchInput)
            val searchButton = view.findViewById<MaterialButton>(R.id.searchButton)
            val searchResults = view.findViewById<RecyclerView>(R.id.searchResults)
            val loadingProgress = view.findViewById<ProgressBar>(R.id.loadingProgress)
            val emptyText = view.findViewById<TextView>(R.id.emptyText)

            return FragmentSearchBinding(
                view as LinearLayout,
                searchInput,
                searchButton,
                searchResults,
                loadingProgress,
                emptyText
            )
        }
    }

    fun getRoot(): View = root
}