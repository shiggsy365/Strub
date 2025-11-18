package com.example.stremiompvplayer.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
// FIX: Import ListAdapter and DiffUtil
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.stremiompvplayer.databinding.ItemStreamBinding
import com.example.stremiompvplayer.models.Stream

class StreamAdapter(
    // FIX: Changed from List<Stream> to just the click listener
    private val onClick: (Stream) -> Unit
) : ListAdapter<Stream, StreamAdapter.ViewHolder>(DiffCallback) { // FIX: Extend ListAdapter

    class ViewHolder(val binding: ItemStreamBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemStreamBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    // REMOVED: getItemCount() - ListAdapter handles this

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        // FIX: Get item from ListAdapter
        val stream = getItem(position)
        holder.binding.title.text = stream.title
        holder.binding.subtitle.text = stream.subtitle

        holder.itemView.setOnClickListener {
            onClick(stream)
        }
    }

    // REMOVED: updateData() - ListAdapter uses submitList()

    // FIX: Add DiffCallback for ListAdapter
    companion object DiffCallback : DiffUtil.ItemCallback<Stream>() {
        override fun areItemsTheSame(oldItem: Stream, newItem: Stream): Boolean {
            return oldItem.url == newItem.url && oldItem.infoHash == newItem.infoHash
        }

        override fun areContentsTheSame(oldItem: Stream, newItem: Stream): Boolean {
            return oldItem == newItem
        }
    }
}