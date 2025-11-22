package com.example.stremiompvplayer.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.example.stremiompvplayer.R
import com.example.stremiompvplayer.databinding.ItemPosterBinding
import com.example.stremiompvplayer.models.LibraryItem

class LibraryAdapter(
    private val onClick: (LibraryItem) -> Unit,
    private val onLongClick: (LibraryItem) -> Unit
) : RecyclerView.Adapter<LibraryAdapter.ViewHolder>() {

    private var items: MutableList<LibraryItem> = mutableListOf()

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

        // REMOVED: holder.binding.title.text = item.name (Title view no longer exists in item_poster.xml)

        Glide.with(holder.itemView.context)
          .load(item.poster)
          .placeholder(R.drawable.movie)
        .error(R.drawable.movie)
        .transition(DrawableTransitionOptions.withCrossFade())
            .into(holder.binding.poster) // <-- FIX IS LIKELY HERE

        holder.itemView.setOnClickListener {
            onClick(item)
        }
        holder.itemView.setOnLongClickListener {
            onLongClick(item)
            true
        }
    }
}

// app/src/main/java/com/example/stremiompvplayer/adapters/LibraryAdapter.kt (FIX ASSUMED)

// Line 45: Likely in onBindViewHolder or similar function
// Replace: Glide.with(holder.itemView.context).load(item.poster).into(holder.poster)
// WITH: Glide.with(holder.itemView.context).load(item.poster).into(holder.binding.poster)
// OR: Glide.with(holder.itemView.context).load(item.poster).into(holder.binding.imagePoster)
// depending on the view ID in your item_library.xml binding class.

// Assuming the view is named 'poster' in your library item binding:

// In ViewHolder class:
// class ViewHolder(val binding: ItemLibraryBinding) : RecyclerView.ViewHolder(binding.root)
// { ... }

// In onBindViewHolder(holder, position):
// Glide.with(holder.itemView.context)
//      .load(item.poster)
//      .placeholder(R.drawable.movie)
//      .error(R.drawable.movie)
//      .transition(DrawableTransitionOptions.withCrossFade())
//      .into(holder.binding.poster) // <-- FIX IS LIKELY HERE