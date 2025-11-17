package com.example.stremiompvplayer.ui.discover

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.stremiompvplayer.R
import com.example.stremiompvplayer.adapters.PosterAdapter
import com.example.stremiompvplayer.models.MetaPreview

class DiscoverSectionAdapter(
    private val onItemClick: (MetaPreview) -> Unit
) : RecyclerView.Adapter<DiscoverSectionAdapter.ViewHolder>() {

    private val sections = mutableListOf<DiscoverSection>()

    fun setSections(newSections: List<DiscoverSection>) {
        sections.clear()
        sections.addAll(newSections)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_content_section, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(sections[position])
    }

    override fun getItemCount(): Int = sections.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val sectionTitle: TextView = itemView.findViewById(R.id.sectionTitle)
        private val sectionRecycler: RecyclerView = itemView.findViewById(R.id.sectionRecycler)

        fun bind(section: DiscoverSection) {
            sectionTitle.text = section.title

            val adapter = PosterAdapter(onItemClick)
            adapter.setItems(section.items)

            sectionRecycler.layoutManager = LinearLayoutManager(
                itemView.context,
                LinearLayoutManager.HORIZONTAL,
                false
            )
            sectionRecycler.adapter = adapter
        }
    }
}