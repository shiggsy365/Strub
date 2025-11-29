package com.example.stremiompvplayer.adapters

import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.stremiompvplayer.R
import com.example.stremiompvplayer.databinding.ItemChannelPosterBinding
import com.example.stremiompvplayer.models.ChannelWithPrograms

/**
 * Data class representing a row of channels in a group
 */
data class ChannelGroupRow(
    val id: String,
    val title: String,
    val channels: List<ChannelWithPrograms>
)

/**
 * Adapter for displaying channel groups as rows with horizontal scrolling channels.
 * Similar to HomeRowAdapter but specialized for Live TV channels.
 */
class ChannelGroupAdapter(
    private val onChannelClick: (ChannelWithPrograms) -> Unit,
    private val onChannelFocused: (ChannelWithPrograms) -> Unit
) : RecyclerView.Adapter<ChannelGroupAdapter.RowViewHolder>() {

    private var rows = listOf<ChannelGroupRow>()
    private val scrollStates = hashMapOf<String, Parcelable?>()
    private val viewPool = RecyclerView.RecycledViewPool()

    fun updateData(newRows: List<ChannelGroupRow>) {
        rows = newRows
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RowViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_home_section, parent, false)
        return RowViewHolder(view)
    }

    override fun onBindViewHolder(holder: RowViewHolder, position: Int) {
        val row = rows[position]
        holder.bind(row)
    }

    override fun getItemCount(): Int = rows.size

    override fun onViewRecycled(holder: RowViewHolder) {
        super.onViewRecycled(holder)
        val key = rows.getOrNull(holder.bindingAdapterPosition)?.id
        if (key != null) {
            scrollStates[key] = holder.recyclerView.layoutManager?.onSaveInstanceState()
        }
    }

    inner class RowViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleView: TextView = itemView.findViewById(R.id.rowTitle)
        val recyclerView: RecyclerView = itemView.findViewById(R.id.rvHorizontalList)

        fun bind(row: ChannelGroupRow) {
            titleView.text = row.title

            val layoutManager = LinearLayoutManager(itemView.context, LinearLayoutManager.HORIZONTAL, false)
            recyclerView.layoutManager = layoutManager
            recyclerView.setRecycledViewPool(viewPool)
            recyclerView.setHasFixedSize(true)
            // Enable nested scrolling for proper scroll coordination with parent RecyclerView
            recyclerView.isNestedScrollingEnabled = true
            recyclerView.isFocusable = false
            recyclerView.descendantFocusability = ViewGroup.FOCUS_AFTER_DESCENDANTS

            val adapter = ChannelPosterAdapter(
                channels = row.channels,
                onClick = onChannelClick
            )

            recyclerView.adapter = adapter

            // Restore scroll position
            scrollStates[row.id]?.let { state ->
                layoutManager.onRestoreInstanceState(state)
            }

            // Setup focus listener for channel items
            recyclerView.setOnHierarchyChangeListener(object : ViewGroup.OnHierarchyChangeListener {
                override fun onChildViewAdded(parent: View?, child: View?) {
                    child?.setOnFocusChangeListener { v, hasFocus ->
                        if (hasFocus) {
                            val pos = recyclerView.getChildAdapterPosition(v)
                            if (pos != RecyclerView.NO_POSITION) {
                                val channel = adapter.getItemSafe(pos)
                                if (channel != null) {
                                    onChannelFocused(channel)
                                }
                            }
                        }
                    }
                }
                override fun onChildViewRemoved(parent: View?, child: View?) {
                    child?.onFocusChangeListener = null
                }
            })
        }
    }
}

/**
 * Adapter for displaying channel posters in a horizontal row.
 * Shows channel logos on black background with centered scaling.
 */
class ChannelPosterAdapter(
    channels: List<ChannelWithPrograms>,
    private val onClick: (ChannelWithPrograms) -> Unit
) : ListAdapter<ChannelWithPrograms, ChannelPosterAdapter.ViewHolder>(ChannelDiffCallback()) {

    init {
        submitList(channels)
    }

    private class ChannelDiffCallback : DiffUtil.ItemCallback<ChannelWithPrograms>() {
        override fun areItemsTheSame(oldItem: ChannelWithPrograms, newItem: ChannelWithPrograms): Boolean {
            return oldItem.channel.id == newItem.channel.id
        }

        override fun areContentsTheSame(oldItem: ChannelWithPrograms, newItem: ChannelWithPrograms): Boolean {
            return oldItem == newItem
        }
    }

    class ViewHolder(val binding: ItemChannelPosterBinding) : RecyclerView.ViewHolder(binding.root) {
        val channelLogo: ImageView = binding.channelLogo
        val channelName: TextView = binding.channelName
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemChannelPosterBinding.inflate(LayoutInflater.from(parent.context), parent, false)

        // Ensure the root card can accept focus
        binding.root.isFocusable = true
        binding.root.isFocusableInTouchMode = true

        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position) ?: return

        // Load channel logo with Glide, centered on black background
        if (!item.channel.logo.isNullOrEmpty()) {
            Glide.with(holder.itemView.context)
                .load(item.channel.logo)
                .placeholder(R.drawable.ic_tv)
                .error(R.drawable.ic_tv)
                .fitCenter()
                .into(holder.channelLogo)
            // Hide channel name when logo is available
            holder.channelName.visibility = View.GONE
        } else {
            // Show placeholder and channel name when no logo
            holder.channelLogo.setImageResource(R.drawable.ic_tv)
            holder.channelName.text = item.channel.name
            holder.channelName.visibility = View.VISIBLE
        }

        holder.itemView.setOnClickListener { onClick(item) }
    }

    fun updateData(newChannels: List<ChannelWithPrograms>) {
        submitList(newChannels)
    }

    fun getItemSafe(position: Int): ChannelWithPrograms? {
        return if (position in 0 until itemCount) super.getItem(position) else null
    }

    fun getItemPosition(item: ChannelWithPrograms): Int {
        return currentList.indexOf(item)
    }
}
