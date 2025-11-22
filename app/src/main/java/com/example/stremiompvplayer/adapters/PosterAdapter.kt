package com.example.stremiompvplayer.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.example.stremiompvplayer.R
import com.example.stremiompvplayer.databinding.ItemPosterBinding
import com.example.stremiompvplayer.models.MetaItem

// Helper object to define the MetaItem instances used as placeholders for navigation buttons.
object NavItem {
    val NAV_PREV = MetaItem("NAV_PREV", "nav_button", "Previous Page", null, null, null)
    val NAV_NEXT = MetaItem("NAV_NEXT", "nav_button", "Next Page", null, null, null)
}

class PosterAdapter(
    private var items: List<MetaItem>,
    private val onClick: (MetaItem) -> Unit,
    private val onLongClick: ((MetaItem) -> Unit)? = null
) : RecyclerView.Adapter<PosterAdapter.ViewHolder>() {

    // Pagination State & Constants
    private var currentPage: Int = 1
    private var isLastPage: Boolean = true
    private val PREV_BUTTON_INDEX = 10 // Position 11
    private val NEXT_BUTTON_INDEX = 19 // Position 20
    private val MAX_DISPLAY_ITEMS = 20

    // IMPORTANT: The views must be accessed correctly from the ItemPosterBinding object
    class ViewHolder(val binding: ItemPosterBinding) : RecyclerView.ViewHolder(binding.root) {
        val posterContainer: View = binding.root.findViewById(R.id.poster_container)
        val navButtonContainer: View = binding.root.findViewById(R.id.nav_button_container)
        val navIcon: android.widget.ImageView = binding.root.findViewById(R.id.nav_icon)
        val navText: android.widget.TextView = binding.root.findViewById(R.id.nav_text)

        // Note: poster, iconWatched, iconInProgress are accessed via binding.xxx below
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemPosterBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return if (currentPage > 0) MAX_DISPLAY_ITEMS else items.size
    }

    private fun getDisplayItem(position: Int): MetaItem? {
        return when (position) {
            PREV_BUTTON_INDEX -> NavItem.NAV_PREV
            NEXT_BUTTON_INDEX -> NavItem.NAV_NEXT
            else -> {
                val dataIndex = position - if (position > PREV_BUTTON_INDEX) 1 else 0
                if (dataIndex in items.indices) items[dataIndex] else null
            }
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val displayItem = getDisplayItem(position)

        // --- 1. HANDLE NAVIGATION BUTTONS (INDEX 10 and 19) ---
        if (displayItem?.id == NavItem.NAV_PREV.id || displayItem?.id == NavItem.NAV_NEXT.id) {

            val isNext = displayItem.id == NavItem.NAV_NEXT.id
            val isPrevVisible = currentPage > 1
            val isNextVisible = !isLastPage

            val isVisible = (isNext && isNextVisible) || (!isNext && isPrevVisible)

            // Show/Hide Containers
            holder.navButtonContainer.visibility = if (isVisible) View.VISIBLE else View.GONE
            holder.binding.poster.visibility = View.GONE
            holder.binding.iconWatched.visibility = View.GONE
            holder.binding.iconInProgress.visibility = View.GONE

            holder.itemView.isFocusable = isVisible
            holder.itemView.isFocusableInTouchMode = isVisible

            if (isVisible) {
                // Setup button appearance
                holder.navIcon.setImageResource(R.drawable.ic_exit_to_app)
                holder.navIcon.scaleX = if (isNext) 1f else -1f

                holder.navText.text = if (isNext) "Next Page" else "Previous Page"

                holder.itemView.setOnClickListener { onClick(displayItem) }
            } else {
                holder.itemView.setOnClickListener(null)
            }
            return
        }

        // --- 2. HANDLE REGULAR POSTERS AND EMPTY SLOTS ---

        // Hide button container and show poster container
        holder.navButtonContainer.visibility = View.GONE
        holder.posterContainer.visibility = View.VISIBLE

        if (displayItem != null) {
            val item = displayItem

            holder.binding.poster.visibility = View.VISIBLE

            // --- ASPECT RATIO LOGIC ---
            val params = holder.binding.poster.layoutParams as ConstraintLayout.LayoutParams
            if (item.isLandscape) {
                params.dimensionRatio = "H,4:3"
            } else {
                params.dimensionRatio = "H,2:3"
            }
            holder.binding.poster.layoutParams = params

            // Load Image
            Glide.with(holder.itemView.context)
                .load(item.poster)
                .placeholder(R.drawable.movie)
                .error(R.drawable.movie)
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(holder.binding.poster)

            // Watched Ticks
            holder.binding.iconWatched.visibility = if (item.isWatched) View.VISIBLE else View.GONE
            holder.binding.iconInProgress.visibility = if (!item.isWatched && item.progress > 0) View.VISIBLE else View.GONE

            holder.itemView.setOnClickListener { onClick(item) }

            if (onLongClick != null) {
                holder.itemView.setOnLongClickListener {
                    onLongClick.invoke(item)
                    true
                }
            }

            holder.itemView.isFocusable = true
            holder.itemView.isFocusableInTouchMode = true
        } else {
            // Empty slot (position > actual item count)
            holder.binding.poster.visibility = View.GONE
            holder.binding.iconWatched.visibility = View.GONE
            holder.binding.iconInProgress.visibility = View.GONE
            holder.itemView.setOnClickListener(null)
            holder.itemView.isFocusable = false
            holder.itemView.isFocusableInTouchMode = false
        }
    }

    fun updateData(newItems: List<MetaItem>, currentPage: Int, isLastPage: Boolean) {
        this.items = newItems
        this.currentPage = currentPage
        this.isLastPage = isLastPage
        notifyDataSetChanged()
    }

    fun getItem(position: Int): MetaItem? {
        return getDisplayItem(position)
    }

    fun getItemPosition(item: MetaItem): Int {
        if (item.id == NavItem.NAV_PREV.id) return PREV_BUTTON_INDEX
        if (item.id == NavItem.NAV_NEXT.id) return NEXT_BUTTON_INDEX

        val index = items.indexOf(item)
        return if (index != -1) {
            if (index < PREV_BUTTON_INDEX) index else index + 1
        } else {
            -1
        }
    }
}