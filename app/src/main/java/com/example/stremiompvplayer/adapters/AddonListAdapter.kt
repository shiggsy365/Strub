package com.example.stremiompvplayer.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.stremiompvplayer.R

class AddonListAdapter(
    val addons: MutableList<String>,
    val onDeleteClick: (String) -> Unit
) : RecyclerView.Adapter<AddonListAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_addon, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val addonUrl = addons[position]
        holder.bind(addonUrl)

        // FIX: Attach the click logic here, where 'onDeleteClick' is visible.
        holder.deleteButton.setOnClickListener {
            // This is the correct scope to access the outer class's lambda property
            onDeleteClick(addonUrl)
        } // <--- THIS IS THE CORRECT LOCATION

    }

    override fun getItemCount(): Int = addons.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val addonUrlText: TextView = itemView.findViewById(R.id.addonUrlText)
        val addonNameText: TextView = itemView.findViewById(R.id.addonNameText)
        val deleteButton: ImageButton = itemView.findViewById(R.id.deleteButton)

        fun bind(addonUrl: String) {
            // Extract a readable name from the URL
            val name = extractAddonName(addonUrl)
            addonNameText.text = name
            addonUrlText.text = addonUrl

            // FIX: REMOVE THIS BLOCK! It is out of scope and redundant.
            /* deleteButton.setOnClickListener {
                onDeleteClick(addonUrl)
            } */
        }

        private fun extractAddonName(url: String): String {
            // ... (extraction logic) ...
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