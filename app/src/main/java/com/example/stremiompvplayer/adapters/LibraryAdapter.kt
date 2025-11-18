package com.example.stremiompvplayer.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.stremiompvplayer.R
import com.example.stremiompvplayer.databinding.ItemPosterBinding
// FIX: Use the stub model
import com.example.stremiompvplayer.models.LibraryItem

class LibraryAdapter(
    // FIX: Use the stub model
    private val items: List<LibraryItem>,
    private val onClick: (LibraryItem) -> Unit,
    private val onLongClick: (LibraryItem) -> Unit
) : RecyclerView.Adapter<LibraryAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemPosterBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        // FIX: Re-added the function body
        val binding = ItemPosterBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.binding.title.text = item.name

        Glide.with(holder.itemView.context)
            .load(item.poster)
            // FIX: Re-added placeholder and .into()
            .placeholder(R.drawable.movie)
            .into(holder.binding.poster)

        holder.itemView.setOnClickListener {
            onClick(item)
        }
        holder.itemView.setOnLongClickListener {
            onLongClick(item)
            true
        }
    }
}