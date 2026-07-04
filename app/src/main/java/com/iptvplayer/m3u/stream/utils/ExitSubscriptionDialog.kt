package com.iptvplayer.m3u.stream.utils

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.Window
import androidx.fragment.app.FragmentActivity
import com.iptvplayer.m3u.stream.databinding.DialogDeleteBinding
import com.iptvplayer.m3u.stream.databinding.DialogFreeTrialBinding

class ExitSubscriptionDialog(
    private val activity: FragmentActivity,
    private val onConfirm: () -> Unit,
    private val onCancel: () -> Unit = {}
) {
    private val binding = DialogFreeTrialBinding.inflate(LayoutInflater.from(activity))
    private val dialog = Dialog(activity)

    init {
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(binding.root)
        dialog.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
        dialog.setCancelable(false)

        binding.btnStart.setOnClickListener {
            dialog.dismiss()
            onConfirm()
        }

        binding.btnNotNow.setOnClickListener {
            dialog.dismiss()
            onCancel()
        }
    }

    fun show() = dialog.show()
}