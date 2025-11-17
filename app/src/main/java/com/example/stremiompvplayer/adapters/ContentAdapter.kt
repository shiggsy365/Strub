package com.example.stremiompvplayer.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.stremiompvplayer.R
import com.example.stremiompvplayer.models.MetaPreview

class ContentAdapter(
    private val onItemClick: (MetaPreview) -> Unit
) : RecyclerView.Adapter<ContentAdapter.ViewHolder>() {
    
    private val items = mutableListOf<MetaPreview>()
    
    fun setItems(newItems: List<MetaPreview>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_content, parent, false)
        return ViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }
    
    override fun getItemCount(): Int = items.size
    
    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val posterImage: ImageView = itemView.findViewById(R.id.posterImage)
        private val titleText: TextView = itemView.findViewById(R.id.titleText)
        private val releaseInfoText: TextView = itemView.findViewById(R.id.releaseInfoText)
        private val descriptionText: TextView = itemView.findViewById(R.id.descriptionText)
        
        fun bind(item: MetaPreview) {
            titleText.text = item.name
            releaseInfoText.text = item.releaseInfo ?: item.type.capitalize()
            descriptionText.text = item.description ?: ""
            
            // Note: You would need to implement image loading here
            // For example, using Glide or Coil:
            // Glide.with(itemView.context).load(item.poster).into(posterImage)
            
            itemView.setOnClickListener {
                onItemClick(item)
            }
        }
    }
}
