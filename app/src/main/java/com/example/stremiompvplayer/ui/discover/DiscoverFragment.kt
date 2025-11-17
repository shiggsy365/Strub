package com.example.stremiompvplayer.ui.discover

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment

class DiscoverFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return TextView(context).apply {
            text = "Discover - Coming Soon"
            textSize = 24f
            setTextColor(Color.WHITE)
        }
    }
}