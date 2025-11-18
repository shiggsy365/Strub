package com.example.stremiompvplayer.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.stremiompvplayer.R
import com.example.stremiompvplayer.databinding.ItemPosterBinding
import com.example.stremiompvplayer.models.LibraryItem

class LibraryAdapter(
    // CHANGED: Initialize with empty list, allowing the main data source to be mutable.
    private val onClick: (LibraryItem) -> Unit,
    private val onLongClick: (LibraryItem) -> Unit
) : RecyclerView.Adapter<LibraryAdapter.ViewHolder>() {

    // NEW: The data list must be a mutable variable (var) to be updated.
    private var items: MutableList<LibraryItem> = mutableListOf()

    // NEW: Public function required by LibraryFragment.kt
    fun setItems(newItems: List<LibraryItem>) {
        this.items.clear()
        this.items.addAll(newItems)
        notifyDataSetChanged()
    }

    class ViewHolder(val binding: ItemPosterBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
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