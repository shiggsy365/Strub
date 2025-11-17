package com.example.stremiompvplayer.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.stremiompvplayer.R
import com.google.android.material.button.MaterialButton

class AddonListAdapter(
    private val addons: MutableList<String>,
    private val onRemove: (String) -> Unit
) : RecyclerView.Adapter<AddonListAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val addonUrl: TextView = view.findViewById(R.id.addonUrl)
        val removeButton: MaterialButton = view.findViewById(R.id.removeButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_addon_list, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val addon = addons[position]
        holder.addonUrl.text = addon
        holder.removeButton.setOnClickListener {
            onRemove(addon)
        }
    }

    override fun getItemCount() = addons.size
}