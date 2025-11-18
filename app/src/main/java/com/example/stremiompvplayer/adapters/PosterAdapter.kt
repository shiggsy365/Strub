package com.example.stremiompvplayer.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.stremiompvplayer.R
import com.example.stremiompvplayer.databinding.ItemPosterBinding
// FIX: Use MetaItem, not MetaPreview
import com.example.stremiompvplayer.models.MetaItem

class PosterAdapter(
    // FIX: Use MetaItem
    private var items: List<MetaItem>,
    private val onClick: (MetaItem) -> Unit
) : RecyclerView.Adapter<PosterAdapter.ViewHolder>() {

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
// ... existing code ...
        holder.binding.title.text = item.name

        Glide.with(holder.itemView.context)
            .load(item.poster)
            // FIX: Re-added placeholder and .into()
            .placeholder(R.drawable.movie)
            .into(holder.binding.poster)

        holder.itemView.setOnClickListener {
            onClick(item)
        }
    }

    fun updateData(newItems: List<MetaItem>) {
        this.items = newItems
        notifyDataSetChanged()
    }
}