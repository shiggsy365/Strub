package com.example.stremiompvplayer.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.stremiompvplayer.R
import com.example.stremiompvplayer.models.MetaPreview

class PosterAdapter(
    private val onItemClick: (MetaPreview) -> Unit
) : RecyclerView.Adapter<PosterAdapter.ViewHolder>() {

    private val items = mutableListOf<MetaPreview>()

    fun setItems(newItems: List<MetaPreview>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_poster, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val posterImage: ImageView = itemView.findViewById(R.id.posterImage)
        private val titleText: TextView = itemView.findViewById(R.id.titleText)
        private val titleOverlay: View = itemView.findViewById(R.id.titleOverlay)
        private val focusOverlay: View = itemView.findViewById(R.id.focusOverlay)

        init {
            itemView.setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) {
                    focusOverlay.visibility = View.VISIBLE
                    titleOverlay.visibility = View.VISIBLE
                    itemView.scaleX = 1.1f
                    itemView.scaleY = 1.1f
                } else {
                    focusOverlay.visibility = View.GONE
                    titleOverlay.visibility = View.GONE
                    itemView.scaleX = 1.0f
                    itemView.scaleY = 1.0f
                }
            }
        }

        fun bind(item: MetaPreview) {
            titleText.text = item.name

            // Load poster image using Coil
            posterImage.load(item.poster) {
                crossfade(true)
                placeholder(R.drawable.default_background)
                error(R.drawable.default_background)
            }

            itemView.setOnClickListener {
                onItemClick(item)
            }
        }
    }
}