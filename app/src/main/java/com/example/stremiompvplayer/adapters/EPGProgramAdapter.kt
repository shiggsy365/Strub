package com.example.stremiompvplayer.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.stremiompvplayer.R
import com.example.stremiompvplayer.models.EPGProgram
import java.text.SimpleDateFormat
import java.util.*

/**
 * Adapter for displaying EPG (Electronic Program Guide) programs in the TV Guide list.
 * Shows program time, title, description and highlights currently airing programs.
 */
class EPGProgramAdapter(
    private val onClick: ((EPGProgram) -> Unit)? = null
) : ListAdapter<EPGProgram, EPGProgramAdapter.ViewHolder>(EPGProgramDiffCallback()) {

    private val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())

    private class EPGProgramDiffCallback : DiffUtil.ItemCallback<EPGProgram>() {
        override fun areItemsTheSame(oldItem: EPGProgram, newItem: EPGProgram): Boolean {
            return oldItem.channelId == newItem.channelId && 
                   oldItem.startTime == newItem.startTime
        }

        override fun areContentsTheSame(oldItem: EPGProgram, newItem: EPGProgram): Boolean {
            return oldItem == newItem
        }
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val programTime: TextView = view.findViewById(R.id.programTime)
        val programTitle: TextView = view.findViewById(R.id.programTitle)
        val programDescription: TextView = view.findViewById(R.id.programDescription)
        val nowAiringLabel: TextView = view.findViewById(R.id.nowAiringLabel)

        init {
            view.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION && onClick != null) {
                    onClick.invoke(getItem(position))
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_epg_program, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val program = getItem(position)
        val currentTime = System.currentTimeMillis()
        val isCurrentlyAiring = currentTime >= program.startTime && currentTime <= program.endTime

        // Format time slot
        holder.programTime.text = formatTimeRange(program.startTime, program.endTime)

        // Program title
        holder.programTitle.text = program.title

        // Program description
        if (!program.description.isNullOrEmpty()) {
            holder.programDescription.text = program.description
            holder.programDescription.visibility = View.VISIBLE
        } else {
            holder.programDescription.visibility = View.GONE
        }

        // Highlight currently airing program
        if (isCurrentlyAiring) {
            holder.nowAiringLabel.visibility = View.VISIBLE
            holder.itemView.setBackgroundColor(
                ContextCompat.getColor(holder.itemView.context, R.color.md_theme_surfaceContainerHigh)
            )
        } else {
            holder.nowAiringLabel.visibility = View.GONE
            holder.itemView.setBackgroundColor(
                ContextCompat.getColor(holder.itemView.context, android.R.color.transparent)
            )
        }
    }

    /**
     * Updates the program list using DiffUtil for efficient updates
     */
    fun updatePrograms(newPrograms: List<EPGProgram>) {
        submitList(newPrograms)
    }

    /**
     * Returns the index of the currently airing program, or -1 if none
     */
    fun getCurrentlyAiringIndex(): Int {
        val currentTime = System.currentTimeMillis()
        return currentList.indexOfFirst { currentTime >= it.startTime && currentTime <= it.endTime }
    }

    private fun formatTimeRange(startTime: Long, endTime: Long): String {
        val start = timeFormat.format(Date(startTime))
        val end = timeFormat.format(Date(endTime))
        return "$start - $end"
    }
}
