package com.example.stremiompvplayer.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
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

    class ViewHolder(val binding: ItemPosterBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemPosterBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]

        // --- ASPECT RATIO LOGIC ---
        val params = holder.binding.poster.layoutParams as ConstraintLayout.LayoutParams
        if (item.isLandscape) {
            // Landscape: 4:3
            params.dimensionRatio = "H,4:3"
        } else {
            // Portrait: 2:3
            params.dimensionRatio = "H,2:3"
        }
        holder.binding.poster.layoutParams = params

        // Load Image
        Glide.with(holder.itemView.context)
            .load(item.poster)
            .placeholder(R.drawable.movie)
            .error(R.drawable.movie)
            .transition(DrawableTransitionOptions.withCrossFade())
            .into(holder.binding.poster)

        // Watched Ticks
        holder.binding.iconWatched.visibility = if (item.isWatched) View.VISIBLE else View.GONE
        holder.binding.iconInProgress.visibility = if (!item.isWatched && item.progress > 0) View.VISIBLE else View.GONE

        holder.itemView.setOnClickListener { onClick(item) }

        if (onLongClick != null) {
            holder.itemView.setOnLongClickListener {
                onLongClick.invoke(item)
                true
            }
        }

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

    fun getItemPosition(item: MetaItem): Int = items.indexOf(item)
}