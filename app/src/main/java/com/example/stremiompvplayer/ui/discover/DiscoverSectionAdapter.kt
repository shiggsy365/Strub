package com.example.stremiompvplayer.ui.discover

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.stremiompvplayer.adapters.PosterAdapter
import com.example.stremiompvplayer.databinding.ItemContentSectionBinding
import com.example.stremiompvplayer.models.FeedList
// FIX: Use MetaItem, not MetaPreview
import com.example.stremiompvplayer.models.MetaItem

class DiscoverSectionAdapter(
    // FIX: Use MetaItem
    private val onClick: (MetaItem) -> Unit
) : ListAdapter<FeedList, DiscoverSectionAdapter.ViewHolder>(DiffCallback) {

    class ViewHolder(val binding: ItemContentSectionBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemContentSectionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val feedList = getItem(position)

        holder.binding.sectionTitle.text = feedList.name

        // FIX: PosterAdapter now takes List<MetaItem> and (MetaItem) -> Unit
        val posterAdapter = PosterAdapter(feedList.content) {
            onClick(it) // 'it' is now MetaItem
        }
        holder.binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(holder.itemView.context, LinearLayoutManager.HORIZONTAL, false)
            adapter = posterAdapter
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<FeedList>() {
        override fun areItemsTheSame(oldItem: FeedList, newItem: FeedList): Boolean {
            // Check if the unique ID and catalog ID are the same
            return oldItem.id == newItem.id && oldItem.catalogId == newItem.catalogId
        }

        override fun areContentsTheSame(oldItem: FeedList, newItem: FeedList): Boolean {
            // Check if the full content of the object is the same
            return oldItem == newItem
        }
    }
}