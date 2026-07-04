package com.iptvplayer.m3u.stream.utils

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.Window
import androidx.fragment.app.FragmentActivity
import com.iptvplayer.m3u.stream.databinding.DialogRewardBinding
import androidx.core.graphics.drawable.toDrawable

class RewardDialog(
    private val activity: FragmentActivity,
    private val onConfirm: () -> Unit,
    private val onCancel: () -> Unit = {}
) {
    private val binding = DialogRewardBinding.inflate(LayoutInflater.from(activity))
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


    }

    fun show() = dialog.show()
}