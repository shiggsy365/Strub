package com.example.stremiompvplayer.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.stremiompvplayer.databinding.ItemCatalogConfigBinding
import com.example.stremiompvplayer.models.UserCatalog

class CatalogConfigAdapter(
    private val onUpdate: (UserCatalog) -> Unit,
    private val onMoveUp: (UserCatalog, Int) -> Unit,
    private val onMoveDown: (UserCatalog, Int) -> Unit,
    private val onDelete: ((UserCatalog) -> Unit)? = null
) : ListAdapter<UserCatalog, CatalogConfigAdapter.ViewHolder>(DiffCallback()) {

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

        fun bind(item: UserCatalog, position: Int) {
            binding.tvCatalogName.text = item.displayName

            // Clear listeners first
            binding.switchDiscover.setOnCheckedChangeListener(null)

            // Set values
            binding.switchDiscover.isChecked = item.showInDiscover

            // Set Visible
            binding.switchDiscover.visibility = View.VISIBLE

            // Re-attach listeners
            binding.switchDiscover.setOnCheckedChangeListener { _, isChecked ->
                onUpdate(item.copy(showInDiscover = isChecked))
            }

            // Protected catalog IDs that cannot be deleted
            val protectedCatalogIds = setOf(
                "popular", "latest", "trending",
                "continue_movies", "continue_episodes", "next_up",
                "trakt_next_up", "trakt_continue_movies", "trakt_continue_shows",
                "watchlist", "genres"
            )
            
            // Check if this is a protected catalog (either by catalogId or by being a built-in local list)
            val isProtected = item.catalogId in protectedCatalogIds || 
                item.catalogId !in listOf("continue_movies", "continue_episodes", "next_up") && 
                (item.addonUrl == "tmdb" && item.catalogId in listOf("popular", "latest", "trending"))
            
            val isCustomList = item.catalogId !in listOf("continue_movies", "continue_episodes", "next_up") &&
                !(item.addonUrl == "tmdb" && item.catalogId in listOf("popular", "latest", "trending"))
            
            // Show delete button only for custom lists that aren't protected
            binding.btnDelete.visibility = if (isCustomList && onDelete != null && !isProtected) View.VISIBLE else View.GONE
            binding.btnDelete.setOnClickListener {
                onDelete?.invoke(item)
            }

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

    class DiffCallback : DiffUtil.ItemCallback<UserCatalog>() {
        override fun areItemsTheSame(oldItem: UserCatalog, newItem: UserCatalog) =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: UserCatalog, newItem: UserCatalog) =
            oldItem == newItem
    }
}