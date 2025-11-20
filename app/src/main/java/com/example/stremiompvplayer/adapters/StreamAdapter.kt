package com.example.stremiompvplayer.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.stremiompvplayer.databinding.ItemStreamBinding
import com.example.stremiompvplayer.models.Stream

class StreamAdapter(
    private val onClick: (Stream) -> Unit
) : ListAdapter<Stream, StreamAdapter.ViewHolder>(DiffCallback) {

    class ViewHolder(val binding: ItemStreamBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemStreamBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val stream = getItem(position)
        // USE FORMATTED TITLE
        holder.binding.title.text = stream.formattedTitle

        // Optionally show raw description if needed, or hide subtitle
        // holder.binding.subtitle.text = stream.description

        holder.itemView.setOnClickListener {
            onClick(stream)
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<Stream>() {
        override fun areItemsTheSame(oldItem: Stream, newItem: Stream): Boolean {
            return oldItem.url == newItem.url && oldItem.infoHash == newItem.infoHash
        }

        override fun areContentsTheSame(oldItem: Stream, newItem: Stream): Boolean {
            return oldItem == newItem
        }
    }
}