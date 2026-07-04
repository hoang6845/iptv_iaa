package com.iptvplayer.m3u.stream.utils

import android.app.Dialog
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.Window
import androidx.core.graphics.drawable.toDrawable
import androidx.fragment.app.FragmentActivity
import com.iptvplayer.m3u.stream.databinding.DialogDailyFreeLimitBinding

class UnlockUnlimitedDialog(
    private val activity: FragmentActivity,
    private val onConfirm: () -> Unit,
    private val onCancel: () -> Unit = {}
) {
    private val binding = DialogDailyFreeLimitBinding.inflate(LayoutInflater.from(activity))
    private val dialog = Dialog(activity)

    init {
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(binding.root)
        dialog.window?.apply {
            setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
            setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
        dialog.setCancelable(false)

        binding.btnUnlock.setOnClickListener {
            dialog.dismiss()
            onConfirm()
        }

        binding.btnClose.setOnClickListener {
            dialog.dismiss()
        }

        binding.backTomorrow.paint.isUnderlineText = true

        binding.backTomorrow.setOnClickListener {
            dialog.dismiss()
            onCancel()
        }
    }

    fun show() = dialog.show()
}