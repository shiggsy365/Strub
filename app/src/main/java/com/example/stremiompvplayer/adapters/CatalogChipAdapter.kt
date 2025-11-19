package com.example.stremiompvplayer.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.stremiompvplayer.R
import com.example.stremiompvplayer.models.Catalog
import com.google.android.material.chip.Chip

/**
 * Adapter for the horizontal scrolling list of Catalog Chips.
 * Updated to work with the Material Chip layout.
 */
class CatalogChipAdapter(
    private val onClick: (Catalog) -> Unit,
    private val onLongClick: ((Catalog) -> Unit)? = null
) : ListAdapter<Catalog, CatalogChipAdapter.ChipViewHolder>(CatalogDiffCallback()) {

    private var selectedPosition: Int = RecyclerView.NO_POSITION

    fun setCatalogs(catalogs: List<Catalog>) {
        submitList(catalogs)
        if (catalogs.isNotEmpty() && selectedPosition == RecyclerView.NO_POSITION) {
            setSelectedPosition(0)
        }
    }

    private fun setSelectedPosition(position: Int) {
        if (selectedPosition == position) return
        val oldPosition = selectedPosition
        selectedPosition = position

        // Refresh both items to update visual state
        if (oldPosition != RecyclerView.NO_POSITION) notifyItemChanged(oldPosition)
        notifyItemChanged(selectedPosition)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChipViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_catalog_chip, parent, false)
        return ChipViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChipViewHolder, position: Int) {
        val catalog = getItem(position)
        holder.bind(
            catalog,
            position,
            selectedPosition,
            onClick,
            ::setSelectedPosition
        )
    }

    class ChipViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // FIX: Cast the itemView directly to Chip
        private val chip: Chip = itemView as Chip

        fun bind(
            catalog: Catalog,
            position: Int,
            selectedPosition: Int,
            onClick: (Catalog) -> Unit,
            onPositionChanged: (Int) -> Unit
        ) {
            chip.text = catalog.name

            val isSelected = position == selectedPosition
            chip.isChecked = isSelected

            // Visual State Handling for the Chip
            if (isSelected) {
                chip.setTextColor(Color.WHITE)
            } else {
                // Dimmed text for unselected
                chip.setTextColor(Color.parseColor("#AAAAAA"))
            }

            // Handle Click
            chip.setOnClickListener {
                if (position != selectedPosition) {
                    onPositionChanged(position)
                }
                onClick(catalog)
            }

            chip.setOnLongClickListener {
                // You can trigger the long click callback here if needed
                true
            }

            // Ensure focus handling for TV navigation
            chip.isFocusable = true
            chip.isClickable = true
        }
    }
}

class CatalogDiffCallback : DiffUtil.ItemCallback<Catalog>() {
    override fun areItemsTheSame(oldItem: Catalog, newItem: Catalog): Boolean {
        return oldItem.id == newItem.id && oldItem.type == newItem.type
    }

    override fun areContentsTheSame(oldItem: Catalog, newItem: Catalog): Boolean {
        return oldItem == newItem
    }
}