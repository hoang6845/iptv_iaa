package com.iptvplayer.m3u.stream.ui.add_playlist

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.Window
import com.iptvplayer.m3u.stream.databinding.DialogSetPasscodeBinding

class PasscodeDialog(
    context: Context,
    private val onConfirm: (passcode: String) -> Unit,
    private val onCancel: () -> Unit
) {
    private val dialog = Dialog(context)
    private val binding = DialogSetPasscodeBinding.inflate(LayoutInflater.from(context))

    init {
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(binding.root)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setLayout(
            (context.resources.displayMetrics.widthPixels * 0.9).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog.setCancelable(false)

        binding.btnConfirm.setOnClickListener {
            val pin = binding.edtPasscode.text?.toString()?.trim() ?: ""
            val confirm = binding.edtConfirmPasscode.text?.toString()?.trim() ?: ""

            // Validate
            if (pin.length < 4) {
                binding.textPasscode.error = "Passcode must be 4 digits"
                return@setOnClickListener
            }
            if (pin != confirm) {
                binding.textConfirmPasscode.error = "Passcode does not match"
                return@setOnClickListener
            }

            binding.textPasscode.error = null
            binding.textConfirmPasscode.error = null
            dialog.dismiss()
            onConfirm(pin)
        }

        binding.btnCancel.setOnClickListener {
            dialog.dismiss()
            onCancel()
        }
    }

    fun show() = dialog.show()
}