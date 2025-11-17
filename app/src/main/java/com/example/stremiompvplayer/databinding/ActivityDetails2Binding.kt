package com.example.stremiompvplayer.databinding

import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.widget.NestedScrollView
import com.example.stremiompvplayer.R
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton

class ActivityDetails2Binding private constructor(
    val root: CoordinatorLayout,
    val toolbar: MaterialToolbar,
    val contentLayout: NestedScrollView,
    val backgroundImage: ImageView,
    val posterImage: ImageView,
    val titleText: TextView,
    val releaseInfoText: TextView,
    val ratingText: TextView,
    val genresText: TextView,
    val descriptionText: TextView,
    val playButton: MaterialButton,
    val libraryButton: MaterialButton,
    val loadingProgress: ProgressBar
) {
    companion object {
        fun inflate(layoutInflater: android.view.LayoutInflater): ActivityDetails2Binding {
            val root = layoutInflater.inflate(R.layout.activity_details2, null) as CoordinatorLayout
            return bind(root)
        }

        fun bind(view: View): ActivityDetails2Binding {
            val toolbar = view.findViewById<MaterialToolbar>(R.id.toolbar)
            val contentLayout = view.findViewById<NestedScrollView>(R.id.contentLayout)
            val backgroundImage = view.findViewById<ImageView>(R.id.backgroundImage)
            val posterImage = view.findViewById<ImageView>(R.id.posterImage)
            val titleText = view.findViewById<TextView>(R.id.titleText)
            val releaseInfoText = view.findViewById<TextView>(R.id.releaseInfoText)
            val ratingText = view.findViewById<TextView>(R.id.ratingText)
            val genresText = view.findViewById<TextView>(R.id.genresText)
            val descriptionText = view.findViewById<TextView>(R.id.descriptionText)
            val playButton = view.findViewById<MaterialButton>(R.id.playButton)
            val libraryButton = view.findViewById<MaterialButton>(R.id.libraryButton)
            val loadingProgress = view.findViewById<ProgressBar>(R.id.loadingProgress)

            return ActivityDetails2Binding(
                view as CoordinatorLayout,
                toolbar,
                contentLayout,
                backgroundImage,
                posterImage,
                titleText,
                releaseInfoText,
                ratingText,
                genresText,
                descriptionText,
                playButton,
                libraryButton,
                loadingProgress
            )
        }
    }

    fun getRoot(): View = root
}