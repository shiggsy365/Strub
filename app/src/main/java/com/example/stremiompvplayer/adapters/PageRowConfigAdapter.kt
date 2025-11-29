package com.example.stremiompvplayer.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.stremiompvplayer.databinding.ItemCatalogConfigBinding
import com.example.stremiompvplayer.utils.PageRowConfigData

/**
 * Adapter for displaying PageRowConfigData items in the Settings page.
 * Shows the protected row configurations (Trending, Latest, Popular, Watchlist, Genres).
 */
class PageRowConfigAdapter(
    private val onMoveUp: (PageRowConfigData, Int) -> Unit,
    private val onMoveDown: (PageRowConfigData, Int) -> Unit
) : ListAdapter<PageRowConfigData, PageRowConfigAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemCatalogConfigBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), position)
    }

    inner class ViewHolder(private val binding: ItemCatalogConfigBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: PageRowConfigData, position: Int) {
            binding.tvCatalogName.text = item.label

            // Hide the discovery switch since these are row configs, not discovery catalogs
            binding.switchDiscover.visibility = View.GONE

            // Protected rows cannot be deleted - hide delete button
            // Show lock indicator for protected rows
            if (item.isProtected) {
                binding.btnDelete.visibility = View.GONE
            } else {
                binding.btnDelete.visibility = View.VISIBLE
            }

            // Always hide delete for PageRowConfigData since all default rows are protected
            binding.btnDelete.visibility = View.GONE

            binding.btnUp.setOnClickListener {
                if (position > 0) onMoveUp(item, position)
            }
            binding.btnUp.alpha = if (position == 0) 0.3f else 1.0f
            binding.btnUp.isEnabled = position > 0

            binding.btnDown.setOnClickListener {
                if (position < itemCount - 1) onMoveDown(item, position)
            }
            binding.btnDown.alpha = if (position == itemCount - 1) 0.3f else 1.0f
            binding.btnDown.isEnabled = position < itemCount - 1
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<PageRowConfigData>() {
        override fun areItemsTheSame(oldItem: PageRowConfigData, newItem: PageRowConfigData) =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: PageRowConfigData, newItem: PageRowConfigData) =
            oldItem == newItem
    }
}
