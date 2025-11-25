package com.example.stremiompvplayer.utils

import android.content.Context
import com.google.android.material.dialog.MaterialAlertDialogBuilder

object ResumeRestartDialog {

    /**
     * Shows a dialog asking the user if they want to resume or restart playback
     */
    fun show(
        context: Context,
        itemTitle: String,
        progress: Long,
        duration: Long,
        onResume: () -> Unit,
        onRestart: () -> Unit
    ) {
        val progressPercent = ((progress.toFloat() / duration.toFloat()) * 100).toInt()
        val progressMinutes = (progress / 1000 / 60).toInt()
        val progressSeconds = ((progress / 1000) % 60).toInt()
        val progressTime = String.format("%d:%02d", progressMinutes, progressSeconds)

        MaterialAlertDialogBuilder(context)
            .setTitle("Resume Playback?")
            .setMessage("$itemTitle\n\nResume from $progressTime ($progressPercent%)?")
            .setPositiveButton("Resume") { _, _ ->
                onResume()
            }
            .setNegativeButton("Restart") { _, _ ->
                onRestart()
            }
            .setCancelable(false)
            .show()
    }
}
