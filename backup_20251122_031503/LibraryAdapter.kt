package com.example.stremiompvplayer.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
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

        // Find the ImageView with id "poster" inside the poster_container
        val posterImageView = holder.binding.root.findViewById<ImageView>(R.id.poster)

        Glide.with(holder.itemView.context)
            .load(item.poster)
            .placeholder(R.drawable.movie)
            .error(R.drawable.movie)
            .transition(DrawableTransitionOptions.withCrossFade())
            .into(posterImageView)

        holder.itemView.setOnClickListener {
            onClick(item)
        }
        holder.itemView.setOnLongClickListener {
            onLongClick(item)
            true
        }
    }
}