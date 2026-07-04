package com.iptvplayer.m3u.stream.utils

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.Window
import androidx.fragment.app.FragmentActivity
import com.iptvplayer.m3u.stream.databinding.DialogDeleteBinding

class DeletePlaylistDialog(
    private val activity: FragmentActivity,
    private val onConfirm: () -> Unit,
    private val onCancel: () -> Unit = {}
) {
    private val binding = DialogDeleteBinding.inflate(LayoutInflater.from(activity))
    private val dialog = Dialog(activity)

    init {
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(binding.root)
        dialog.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setLayout(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
        dialog.setCancelable(false)

        binding.btnConfirm.setOnClickListener {
            dialog.dismiss()
            onConfirm()
        }

        binding.btnCancel.setOnClickListener {
            dialog.dismiss()
            onCancel()
        }
    }

    fun show() = dialog.show()
}