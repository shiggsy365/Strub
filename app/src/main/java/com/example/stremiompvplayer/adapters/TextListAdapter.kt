package com.example.stremiompvplayer.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.stremiompvplayer.R

data class TextItem(
    val id: String,
    val text: String,
    val type: String, // "season", "episode", "back"
    val data: Any? = null
)

class TextListAdapter(
    private var items: List<TextItem>, // Internal list
    private val onClick: (TextItem) -> Unit,
    private val onFocus: (TextItem) -> Unit
) : RecyclerView.Adapter<TextListAdapter.ViewHolder>() {

    // Helper to allow external access for indexing (Fixes unresolved reference)
    val currentList: List<TextItem> get() = items

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.title)
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        // Reusing item_stream layout as it provides the desired text-list look with focus states
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_stream, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.title.text = item.text

        holder.itemView.setOnClickListener { onClick(item) }

        holder.itemView.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) onFocus(item)
        }
    }

    fun submitList(newItems: List<TextItem>) {
        items = newItems
        notifyDataSetChanged()
    }
}