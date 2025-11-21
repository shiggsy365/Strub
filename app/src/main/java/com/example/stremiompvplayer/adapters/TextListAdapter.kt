package com.example.stremiompvplayer.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.stremiompvplayer.R

data class TextItem(
    val id: String,
    val text: String,
    val type: String, // "season", "episode", "back", "up"
    val data: Any? = null,
    val image: String? = null // NEW: For thumbnails
)

class TextListAdapter(
    private var items: List<TextItem>,
    private val onClick: (TextItem) -> Unit,
    private val onFocus: (TextItem) -> Unit,
    private val onLongClick: ((View, TextItem) -> Unit)? = null // NEW: Long click
) : RecyclerView.Adapter<TextListAdapter.ViewHolder>() {

    // Helper to allow external access for indexing
    val currentList: List<TextItem> get() = items

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.title)
        val icon: ImageView = view.findViewById(R.id.icon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_stream, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.title.text = item.text

        // Handle Thumbnail
        if (!item.image.isNullOrEmpty()) {
            holder.icon.visibility = View.VISIBLE
            Glide.with(holder.itemView.context)
                .load(item.image)
                .centerCrop()
                .placeholder(R.drawable.movie) // Placeholder
                .into(holder.icon)
        } else {
            holder.icon.visibility = View.GONE
        }

        holder.itemView.setOnClickListener { onClick(item) }

        holder.itemView.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) onFocus(item)
        }

        if (onLongClick != null) {
            holder.itemView.setOnLongClickListener {
                onLongClick.invoke(holder.itemView, item)
                true
            }
        }
    }

    fun submitList(newItems: List<TextItem>) {
        items = newItems
        notifyDataSetChanged()
    }
}