package com.example.stremiompvplayer.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.stremiompvplayer.R

class SidePanelAdapter(
    private val onItemClick: (SidePanelItem) -> Unit,
    private val onItemFocus: (SidePanelItem) -> Unit
) : ListAdapter<SidePanelItem, SidePanelAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_side_panel, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleText: TextView = itemView.findViewById(R.id.text_title)
        private val subtitleText: TextView = itemView.findViewById(R.id.text_subtitle)

        fun bind(item: SidePanelItem) {
            titleText.text = item.title
            if (item.subtitle.isNullOrEmpty()) {
                subtitleText.visibility = View.GONE
            } else {
                subtitleText.visibility = View.VISIBLE
                subtitleText.text = item.subtitle
            }

            itemView.setOnClickListener { onItemClick(item) }
            itemView.setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) onItemFocus(item)
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<SidePanelItem>() {
        override fun areItemsTheSame(oldItem: SidePanelItem, newItem: SidePanelItem): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: SidePanelItem, newItem: SidePanelItem): Boolean =
            oldItem == newItem
    }
}

data class SidePanelItem(
    val id: String,
    val title: String,
    val subtitle: String? = null,
    val type: ItemType,
    val data: Any? = null
)

enum class ItemType {
    STREAM,
    SEASON,
    EPISODE,
    BACK_NAVIGATION
}