package com.example.stremiompvplayer.utils

import android.content.Context
import android.text.InputFilter
import android.text.InputType
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import com.google.android.material.dialog.MaterialAlertDialogBuilder

object PasscodeDialog {

    /**
     * Shows a dialog to set a new passcode
     */
    fun showSetPasscodeDialog(
        context: Context,
        onPasscodeSet: (String) -> Unit,
        onCancel: (() -> Unit)? = null
    ) {
        val input = EditText(context).apply {
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
            filters = arrayOf(InputFilter.LengthFilter(4))
            hint = "Enter 4-digit passcode"
            setPadding(48, 32, 48, 32)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        MaterialAlertDialogBuilder(context)
            .setTitle("Set Passcode")
            .setMessage("Enter a 4-digit passcode to protect this profile")
            .setView(input)
            .setPositiveButton("Set") { _, _ ->
                val passcode = input.text.toString()
                if (passcode.length == 4) {
                    onPasscodeSet(passcode)
                } else {
                    android.widget.Toast.makeText(
                        context,
                        "Passcode must be exactly 4 digits",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                    onCancel?.invoke()
                }
            }
            .setNegativeButton("Skip") { _, _ ->
                onCancel?.invoke()
            }
            .setCancelable(false)
            .show()
    }

    /**
     * Shows a dialog to verify an existing passcode
     */
    fun showVerifyPasscodeDialog(
        context: Context,
        expectedPasscode: String,
        onSuccess: () -> Unit,
        onCancel: (() -> Unit)? = null
    ) {
        val input = EditText(context).apply {
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
            filters = arrayOf(InputFilter.LengthFilter(4))
            hint = "Enter passcode"
            setPadding(48, 32, 48, 32)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        MaterialAlertDialogBuilder(context)
            .setTitle("Enter Passcode")
            .setMessage("This profile is protected")
            .setView(input)
            .setPositiveButton("Unlock") { _, _ ->
                val enteredPasscode = input.text.toString()
                if (enteredPasscode == expectedPasscode) {
                    onSuccess()
                } else {
                    android.widget.Toast.makeText(
                        context,
                        "Incorrect passcode",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                    onCancel?.invoke()
                }
            }
            .setNegativeButton("Cancel") { _, _ ->
                onCancel?.invoke()
            }
            .setCancelable(false)
            .show()
    }
}
