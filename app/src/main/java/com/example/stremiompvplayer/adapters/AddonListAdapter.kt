package com.example.stremiompvplayer.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.stremiompvplayer.R

class AddonListAdapter(
    private val addons: MutableList<String>,
    private val onDeleteClick: (String) -> Unit
) : RecyclerView.Adapter<AddonListAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_addon, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(addons[position])
    }

    override fun getItemCount(): Int = addons.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val addonUrlText: TextView = itemView.findViewById(R.id.addonUrlText)
        private val addonNameText: TextView = itemView.findViewById(R.id.addonNameText)
        private val deleteButton: ImageButton = itemView.findViewById(R.id.deleteButton)

        fun bind(addonUrl: String) {
            // Extract a readable name from the URL
            val name = extractAddonName(addonUrl)
            addonNameText.text = name
            addonUrlText.text = addonUrl

            deleteButton.setOnClickListener {
                onDeleteClick(addonUrl)
            }
        }

        private fun extractAddonName(url: String): String {
            // Try to extract a meaningful name from the URL
            return try {
                val urlWithoutProtocol = url.replace("https://", "").replace("http://", "")
                val parts = urlWithoutProtocol.split("/")
                if (parts.isNotEmpty()) {
                    parts[0].replace("-", " ").split(".").firstOrNull() ?: "Unknown Addon"
                } else {
                    "Unknown Addon"
                }
            } catch (e: Exception) {
                "Unknown Addon"
            }
        }
    }
}