package com.example.stremiompvplayer.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.stremiompvplayer.databinding.ItemCatalogConfigBinding
import com.example.stremiompvplayer.models.UserCatalog

class CatalogConfigAdapter(
    private val onUpdate: (UserCatalog) -> Unit,
    private val onMoveUp: (UserCatalog, Int) -> Unit,
    private val onMoveDown: (UserCatalog, Int) -> Unit
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
            binding.tvCatalogName.text = item.name

            binding.switchDiscover.setOnCheckedChangeListener(null)
            binding.switchUser.setOnCheckedChangeListener(null)

            binding.switchDiscover.isChecked = item.isDiscoverEnabled
            binding.switchUser.isChecked = item.isUserEnabled

            binding.switchDiscover.setOnCheckedChangeListener { _, isChecked ->
                onUpdate(item.copy(isDiscoverEnabled = isChecked))
            }

            binding.switchUser.setOnCheckedChangeListener { _, isChecked ->
                onUpdate(item.copy(isUserEnabled = isChecked))
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
            oldItem.dbId == newItem.dbId
        override fun areContentsTheSame(oldItem: UserCatalog, newItem: UserCatalog) =
            oldItem == newItem
    }
}