package com.example.stremiompvplayer.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.stremiompvplayer.R
import com.example.stremiompvplayer.models.ChannelWithPrograms
import java.text.SimpleDateFormat
import java.util.*

class TVGuideAdapter(
    private val onClick: (ChannelWithPrograms) -> Unit
) : RecyclerView.Adapter<TVGuideAdapter.ViewHolder>() {

    private var channels = listOf<ChannelWithPrograms>()
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val channelLogo: ImageView = view.findViewById(R.id.channelLogo)
        val channelName: TextView = view.findViewById(R.id.channelName)
        val currentProgramTitle: TextView = view.findViewById(R.id.currentProgramTitle)
        val currentProgramTime: TextView = view.findViewById(R.id.currentProgramTime)
        val programProgress: ProgressBar = view.findViewById(R.id.programProgress)

        init {
            view.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onClick(channels[position])
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_tv_guide, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = channels[position]

        holder.channelName.text = item.channel.name

        // Load channel logo
        if (!item.channel.logo.isNullOrEmpty()) {
            Glide.with(holder.itemView.context)
                .load(item.channel.logo)
                .placeholder(R.drawable.ic_tv)
                .error(R.drawable.ic_tv)
                .into(holder.channelLogo)
        } else {
            holder.channelLogo.setImageResource(R.drawable.ic_tv)
        }

        // Display current program
        val currentProgram = item.currentProgram
        if (currentProgram != null) {
            holder.currentProgramTitle.text = currentProgram.title
            holder.currentProgramTime.text = formatTimeRange(currentProgram.startTime, currentProgram.endTime)

            // Calculate and display progress
            val progress = calculateProgress(currentProgram.startTime, currentProgram.endTime)
            holder.programProgress.progress = progress
            holder.programProgress.visibility = View.VISIBLE
        } else {
            holder.currentProgramTitle.text = "No program information"
            holder.currentProgramTime.text = ""
            holder.programProgress.visibility = View.GONE
        }
    }

    override fun getItemCount() = channels.size

    fun updateChannels(newChannels: List<ChannelWithPrograms>) {
        channels = newChannels
        notifyDataSetChanged()
    }

    fun getItemAtPosition(position: Int): ChannelWithPrograms? {
        return if (position >= 0 && position < channels.size) {
            channels[position]
        } else {
            null
        }
    }

    private fun formatTimeRange(startTime: Long, endTime: Long): String {
        val start = timeFormat.format(Date(startTime))
        val end = timeFormat.format(Date(endTime))
        return "$start - $end"
    }

    private fun calculateProgress(startTime: Long, endTime: Long): Int {
        val currentTime = System.currentTimeMillis()
        if (currentTime < startTime) return 0
        if (currentTime > endTime) return 100

        val duration = endTime - startTime
        val elapsed = currentTime - startTime
        return ((elapsed.toFloat() / duration.toFloat()) * 100).toInt()
    }
}
