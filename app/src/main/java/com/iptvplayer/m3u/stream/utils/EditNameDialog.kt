package com.iptvplayer.m3u.stream.utils

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.Window
import androidx.fragment.app.FragmentActivity
import com.iptvplayer.m3u.stream.databinding.DialogRenameBinding

class EditNameDialog(
    private val activity: FragmentActivity,
    private val currentName: String = "",
    private val onConfirm: (name: String) -> Unit,
    private val onCancel: () -> Unit = {}
) {
    private val binding = DialogRenameBinding.inflate(LayoutInflater.from(activity))
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

        // Set tên hiện tại vào EditText
        binding.edtPlaylistName.setText(currentName)
        binding.edtPlaylistName.setSelection(currentName.length)

        // Nút clear text
        binding.textName.setEndIconOnClickListener {
            binding.edtPlaylistName.text?.clear()
        }

        binding.btnConfirm.setOnClickListener {
            val name = binding.edtPlaylistName.text?.toString()?.trim() ?: ""
            if (name.isEmpty()) {
                binding.textName.error = "Please enter a name"
                return@setOnClickListener
            }
            binding.textName.error = null
            dialog.dismiss()
            onConfirm(name)
        }

        binding.btnCancel.setOnClickListener {
            dialog.dismiss()
            onCancel()
        }
    }

    fun show() = dialog.show()
}