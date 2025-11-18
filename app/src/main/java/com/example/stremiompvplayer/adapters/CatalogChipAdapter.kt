package com.example.stremiompvplayer.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.stremiompvplayer.MainActivity
import com.example.stremiompvplayer.R
import com.example.stremiompvplayer.models.Catalog
import com.google.android.material.chip.Chip

/**
 * Adapter for the horizontal scrolling list of Catalog Chips (navigation buttons).
 * It uses ListAdapter for efficient updates and manages the selected state visually.
 */
class CatalogChipAdapter(
    private val onClick: (Catalog) -> Unit
) : ListAdapter<Catalog, CatalogChipAdapter.ChipViewHolder>(CatalogDiffCallback()) {

    // 1. FIX: Changed to 'private var' to allow reassignment by setSelectedPosition
    private var selectedPosition: Int = RecyclerView.NO_POSITION

    // Public method to satisfy the calling convention in the Fragment
    fun setCatalogs(catalogs: List<Catalog>) {
        submitList(catalogs)

        // Ensure the first item is selected by default after the list is submitted
        if (catalogs.isNotEmpty() && selectedPosition == RecyclerView.NO_POSITION) {
            // Wait for the next layout pass, then select the first item.
            // Note: This relies on the fragment or adapter to trigger the initial content fetch.
            setSelectedPosition(0)
        }
    }

    // 2. NEW: Method to safely update state and trigger minimal repaint
    private fun setSelectedPosition(position: Int) {
        if (selectedPosition == position) return

        // Notify the previously selected item to uncheck itself
        if (selectedPosition != RecyclerView.NO_POSITION) {
            notifyItemChanged(selectedPosition)
        }

        // Set new position and notify the new item to check itself
        selectedPosition = position
        notifyItemChanged(selectedPosition)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChipViewHolder {
        val chip = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_catalog_chip, parent, false) as Chip
        return ChipViewHolder(chip)
    }

    override fun onBindViewHolder(holder: ChipViewHolder, position: Int) {
        val catalog = getItem(position)

        // 3. UPDATED BIND: Pass the adapter's state and the required lambda to update the position
        holder.bind(
            catalog,
            position,
            selectedPosition,
            onClick,
            ::setSelectedPosition // Pass the function reference to update the state
        )
    }

    override fun getItemCount(): Int = currentList.size

    class ChipViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        // 4. UPDATED BIND SIGNATURE: Accept the lambda to update the adapter's state
        fun bind(
            catalog: Catalog,
            position: Int,
            selectedPosition: Int,
            onClick: (Catalog) -> Unit,
            onPositionChanged: (Int) -> Unit // The lambda to call when the user clicks
        ) {
            val chip = itemView as Chip
            chip.text = catalog.name

            // Visual state handling
            val isSelected = position == selectedPosition
            chip.isChecked = isSelected
            chip.isClickable = true
            chip.isFocusable = true

            // Set the visual style based on selection (assuming colors are defined)
            if (isSelected) {
                // Using chip.resources.getColorStateList is only safe if R.color.chip_background_selector
                // is actually a ColorStateList XML file defining the checked and unchecked states.
                // Since the original structure relies on a simple selector XML, this is fine.
                chip.chipBackgroundColor = chip.resources.getColorStateList(R.color.chip_background_selector)
                chip.setTextColor(chip.resources.getColor(android.R.color.white))
            } else {
                chip.chipBackgroundColor = chip.resources.getColorStateList(R.color.chip_background_selector)
                // Assuming R.color.chip_text_color is defined in your resources
                chip.setTextColor(chip.resources.getColor(R.color.chip_text_color))
            }

            // Handle the click event
            chip.setOnClickListener {
                // 5. FIX: Call the external adapter function to change state
                if (position != selectedPosition) {
                    onPositionChanged(position) // Requests the adapter to change the selectedPosition state
                }

                onClick(catalog) // Executes the content loading lambda (passed from the Fragment)
            }
        }
    }
}

// DiffUtil is crucial for smooth RecyclerView updates
class CatalogDiffCallback : DiffUtil.ItemCallback<Catalog>() {
    override fun areItemsTheSame(oldItem: Catalog, newItem: Catalog): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Catalog, newItem: Catalog): Boolean {
        return oldItem == newItem
    }
}