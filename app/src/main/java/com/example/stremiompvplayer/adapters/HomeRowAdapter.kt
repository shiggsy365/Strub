package com.example.stremiompvplayer.adapters

import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.stremiompvplayer.R
import com.example.stremiompvplayer.models.MetaItem
import com.example.stremiompvplayer.viewmodels.HomeRow

class HomeRowAdapter(
    private val onContentClick: (MetaItem) -> Unit,
    private val onContentFocused: (MetaItem) -> Unit
) : RecyclerView.Adapter<HomeRowAdapter.RowViewHolder>() {

    private var rows = listOf<HomeRow>()
    private var onContentLongClick: ((MetaItem) -> Unit)? = null
    private val scrollStates = hashMapOf<String, Parcelable?>()
    private val viewPool = RecyclerView.RecycledViewPool()

    fun updateData(newRows: List<HomeRow>, longClickListener: (MetaItem) -> Unit) {
        rows = newRows
        onContentLongClick = longClickListener
        notifyDataSetChanged()
    }

    // Keep original submitList for compatibility if needed
    fun submitList(newRows: List<HomeRow>) {
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

        fun bind(row: HomeRow) {
            titleView.text = row.title

            val layoutManager = LinearLayoutManager(itemView.context, LinearLayoutManager.HORIZONTAL, false)
            recyclerView.layoutManager = layoutManager
            recyclerView.setRecycledViewPool(viewPool)
            recyclerView.setHasFixedSize(true)

            val adapter = PosterAdapter(
                items = row.items,
                onClick = onContentClick,
                onLongClick = onContentLongClick
            )

            recyclerView.adapter = adapter

            scrollStates[row.id]?.let { state ->
                layoutManager.onRestoreInstanceState(state)
            }

            recyclerView.setOnHierarchyChangeListener(object : ViewGroup.OnHierarchyChangeListener {
                override fun onChildViewAdded(parent: View?, child: View?) {
                    child?.setOnFocusChangeListener { v, hasFocus ->
                        if (hasFocus) {
                            val pos = recyclerView.getChildAdapterPosition(v)
                            if (pos != RecyclerView.NO_POSITION) {
                                val item = adapter.getItem(pos)
                                if (item != null) onContentFocused(item)
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