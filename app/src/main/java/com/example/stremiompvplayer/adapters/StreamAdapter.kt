package com.example.stremiompvplayer.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.stremiompvplayer.R
import com.example.stremiompvplayer.models.Stream

class StreamAdapter(
    private val streams: List<Stream>,
    private val onStreamClick: (Stream) -> Unit
) : RecyclerView.Adapter<StreamAdapter.ViewHolder>() {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_stream, parent, false)
        return ViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(streams[position])
    }
    
    override fun getItemCount(): Int = streams.size
    
    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val streamTitleText: TextView = itemView.findViewById(R.id.streamTitleText)
        private val streamNameText: TextView = itemView.findViewById(R.id.streamNameText)
        
        fun bind(stream: Stream) {
            streamTitleText.text = stream.title ?: "Stream ${adapterPosition + 1}"
            streamNameText.text = stream.name ?: "Unknown source"
            
            itemView.setOnClickListener {
                onStreamClick(stream)
            }
        }
    }
}
