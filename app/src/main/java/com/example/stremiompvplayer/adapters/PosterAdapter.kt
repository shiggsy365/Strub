package com.example.stremiompvplayer.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.example.stremiompvplayer.R
import com.example.stremiompvplayer.databinding.ItemPosterBinding
import com.example.stremiompvplayer.models.MetaItem

class PosterAdapter(
    private var items: List<MetaItem>,
    private val onClick: (MetaItem) -> Unit,
    private val onLongClick: ((MetaItem) -> Unit)? = null
) : RecyclerView.Adapter<PosterAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemPosterBinding) : RecyclerView.ViewHolder(binding.root) {
        val poster: ImageView = binding.poster
        val iconWatched: ImageView = binding.iconWatched
        val iconInProgress: ImageView = binding.iconInProgress
        val genreText: android.widget.TextView = binding.genreText
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemPosterBinding.inflate(LayoutInflater.from(parent.context), parent, false)

        // Ensure the root card can accept focus
        binding.root.isFocusable = true
        binding.root.isFocusableInTouchMode = true

        return ViewHolder(binding)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]

        // Use scaleType centerCrop to fill the fixed 2:3 aspect ratio poster
        holder.poster.scaleType = ImageView.ScaleType.CENTER_CROP

        // Load Image with Glide
        Glide.with(holder.itemView.context)
            .load(item.poster)
            .placeholder(R.drawable.movie) // Ensure this drawable exists and has a reasonable aspect ratio (2:3)
            .error(R.drawable.movie)
            .transition(DrawableTransitionOptions.withCrossFade())
            .into(holder.poster)

        // Watched Ticks
        holder.iconWatched.visibility = if (item.isWatched) View.VISIBLE else View.GONE
        holder.iconInProgress.visibility = if (!item.isWatched && item.progress > 0) View.VISIBLE else View.GONE

        // Genre Text (show for genre items)
        if (item.type == "genre") {
            holder.genreText.text = item.name
            holder.genreText.visibility = View.VISIBLE
        } else {
            holder.genreText.visibility = View.GONE
        }

        holder.itemView.setOnClickListener { onClick(item) }

        if (onLongClick != null) {
            holder.itemView.setOnLongClickListener {
                onLongClick.invoke(item)
                true
            }
        }
    }

    fun updateData(newItems: List<MetaItem>) {
        this.items = newItems
        notifyDataSetChanged()
    }

    fun getItem(position: Int): MetaItem? {
        return if (position in items.indices) items[position] else null
    }

    fun getItemPosition(item: MetaItem): Int {
        return items.indexOf(item)
    }
}