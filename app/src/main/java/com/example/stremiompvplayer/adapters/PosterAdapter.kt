package com.example.stremiompvplayer.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
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

    class ViewHolder(val binding: ItemPosterBinding) : RecyclerView.ViewHolder(binding.root) {
        val posterContainer: View = binding.root.findViewById(R.id.poster_container)
        val poster: ImageView by lazy { posterContainer.findViewById(R.id.poster) }
        val iconWatched: ImageView by lazy { posterContainer.findViewById(R.id.iconWatched) }
        val iconInProgress: ImageView by lazy { posterContainer.findViewById(R.id.iconInProgress) }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemPosterBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]

        // Show poster container
        holder.posterContainer.visibility = View.VISIBLE

        // --- ASPECT RATIO LOGIC ---
        val params = holder.poster.layoutParams as ConstraintLayout.LayoutParams
        if (item.isLandscape) {
            params.dimensionRatio = "H,4:3"
        } else {
            params.dimensionRatio = "H,2:3"
        }
        holder.poster.layoutParams = params

        // Load Image
        Glide.with(holder.itemView.context)
            .load(item.poster)
            .placeholder(R.drawable.movie)
            .error(R.drawable.movie)
            .transition(DrawableTransitionOptions.withCrossFade())
            .into(holder.poster)

        // Watched Ticks
        holder.iconWatched.visibility = if (item.isWatched) View.VISIBLE else View.GONE
        holder.iconInProgress.visibility = if (!item.isWatched && item.progress > 0) View.VISIBLE else View.GONE

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

    fun getItemPosition(item: MetaItem): Int {
        return items.indexOf(item)
    }
}
