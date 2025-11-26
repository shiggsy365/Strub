package com.example.stremiompvplayer.utils

import android.content.Context
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.max

/**
 * A GridLayoutManager that automatically calculates the number of columns
 * based on the available width and a minimum column width.
 */
class AutoFitGridLayoutManager(
    context: Context,
    private val columnWidth: Int
) : GridLayoutManager(context, 1) {

    private var lastWidth = 0
    private var lastHeight = 0

    override fun onLayoutChildren(recycler: RecyclerView.Recycler?, state: RecyclerView.State?) {
        val width = width
        val height = height

        if (width > 0 && height > 0 && (lastWidth != width || lastHeight != height)) {
            val totalSpace = if (orientation == VERTICAL) {
                width - paddingRight - paddingLeft
            } else {
                height - paddingTop - paddingBottom
            }

            val spanCount = max(1, totalSpace / columnWidth)
            setSpanCount(spanCount)

            lastWidth = width
            lastHeight = height
        }

        super.onLayoutChildren(recycler, state)
    }
}
