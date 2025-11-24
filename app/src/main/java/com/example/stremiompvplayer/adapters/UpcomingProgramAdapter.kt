package com.example.stremiompvplayer.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.stremiompvplayer.R
import com.example.stremiompvplayer.models.EPGProgram
import java.text.SimpleDateFormat
import java.util.*

class UpcomingProgramAdapter : RecyclerView.Adapter<UpcomingProgramAdapter.ViewHolder>() {

    private var programs = listOf<EPGProgram>()
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.upcomingTitle)
        val time: TextView = view.findViewById(R.id.upcomingTime)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_upcoming_program, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val program = programs[position]
        holder.title.text = program.title
        holder.time.text = formatTimeRange(program.startTime, program.endTime)
    }

    override fun getItemCount() = programs.size

    fun updatePrograms(newPrograms: List<EPGProgram>) {
        programs = newPrograms
        notifyDataSetChanged()
    }

    private fun formatTimeRange(startTime: Long, endTime: Long): String {
        val start = timeFormat.format(Date(startTime))
        val end = timeFormat.format(Date(endTime))
        return "$start - $end"
    }
}
