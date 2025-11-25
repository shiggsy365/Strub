package com.example.stremiompvplayer

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.example.stremiompvplayer.databinding.DialogStartupLoadingBinding

class StartupLoadingDialog(context: Context) {

    private val dialog: AlertDialog
    private val binding: DialogStartupLoadingBinding

    init {
        binding = DialogStartupLoadingBinding.inflate(LayoutInflater.from(context))
        dialog = AlertDialog.Builder(context)
            .setView(binding.root)
            .setCancelable(false)
            .create()
    }

    fun show() {
        dialog.show()
    }

    fun dismiss() {
        dialog.dismiss()
    }

    fun setUserListsComplete() {
        binding.iconUserLists.setImageResource(R.drawable.ic_check_circle)
        binding.iconUserLists.setColorFilter(Color.parseColor("#4CAF50")) // Green
        binding.textUserLists.setTextColor(Color.parseColor("#4CAF50"))
        checkIfAllComplete()
    }

    fun setMetadataComplete() {
        binding.iconMetadata.setImageResource(R.drawable.ic_check_circle)
        binding.iconMetadata.setColorFilter(Color.parseColor("#4CAF50"))
        binding.textMetadata.setTextColor(Color.parseColor("#4CAF50"))
        checkIfAllComplete()
    }

    fun setTvChannelsComplete() {
        binding.iconTvChannels.setImageResource(R.drawable.ic_check_circle)
        binding.iconTvChannels.setColorFilter(Color.parseColor("#4CAF50"))
        binding.textTvChannels.setTextColor(Color.parseColor("#4CAF50"))
        checkIfAllComplete()
    }

    fun setUserListsLoading() {
        binding.progressUserLists.visibility = View.VISIBLE
    }

    fun setMetadataLoading() {
        binding.progressMetadata.visibility = View.VISIBLE
    }

    fun setTvChannelsLoading() {
        binding.progressTvChannels.visibility = View.VISIBLE
    }

    private fun checkIfAllComplete() {
        // Auto-dismiss when all tasks are complete
        val allComplete = binding.iconUserLists.colorFilter != null &&
                         binding.iconMetadata.colorFilter != null &&
                         binding.iconTvChannels.colorFilter != null

        if (allComplete) {
            binding.root.postDelayed({ dismiss() }, 500)
        }
    }
}
