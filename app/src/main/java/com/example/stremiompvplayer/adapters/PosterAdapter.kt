package com.example.stremiompvplayer.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.stremiompvplayer.R
import com.example.stremiompvplayer.databinding.ItemPosterBinding
// FIX: Use the stub model
import com.example.stremiompvplayer.models.MetaPreview

class PosterAdapter(
    // FIX: Use the stub model
    private var items: List<MetaPreview>,
    private val onClick: (MetaPreview) -> Unit
) : RecyclerView.Adapter<PosterAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemPosterBinding) : RecyclerView.ViewHolder(binding.root)
    // ... existing code ...
    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
// ... existing code ...
        holder.binding.title.text = item.name

        Glide.with(holder.itemView.context)
            .load(item.poster)
// ... existing code ...
        holder.itemView.setOnClickListener {
            onClick(item)
        }
    }

    fun updateData(newItems: List<MetaPreview>) {
        this.items = newItems
        notifyDataSetChanged()
    }
}