package com.example.stremiompvplayer.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.example.stremiompvplayer.R
import com.example.stremiompvplayer.databinding.ItemPosterBinding
import com.example.stremiompvplayer.models.MetaItem

/**
 * Enhanced PosterAdapter with:
 * - Proper poster aspect ratio (2:3)
 * - Long-press support for collection
 * - Click and long-click callbacks
 */
class PosterAdapter(
    private var items: List<MetaItem>,
    private val onClick: (MetaItem) -> Unit,
    private val onLongClick: ((MetaItem) -> Unit)? = null
) : RecyclerView.Adapter<PosterAdapter.ViewHolder>() {

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

        // Set title (optional - can be hidden for cleaner look)
        holder.binding.title.text = item.name
        holder.binding.title.visibility = View.GONE // Hide by default for poster-only view

        // Load poster with Glide, maintaining aspect ratio
        Glide.with(holder.itemView.context)
            .load(item.poster)
            .placeholder(R.drawable.movie) // Your placeholder drawable
            .error(R.drawable.movie)
            .transition(DrawableTransitionOptions.withCrossFade())
            .into(holder.binding.poster)

        // Regular click
        holder.itemView.setOnClickListener {
            onClick(item)
        }

        // Long click (for collection)
        if (onLongClick != null) {
            holder.itemView.setOnLongClickListener {
                onLongClick.invoke(item)
                true // Consume the event
            }
        }

        // Focus handling for TV
        holder.itemView.isFocusable = true
        holder.itemView.isFocusableInTouchMode = true
    }

    fun updateData(newItems: List<MetaItem>) {
        this.items = newItems
        notifyDataSetChanged()
    }

    fun getItem(position: Int): MetaItem? {
        return if (position in items.indices) items[position] else null
    }
}