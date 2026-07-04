package com.iptvplayer.m3u.stream.utils

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.Window
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.iptvplayer.m3u.stream.databinding.DialogVerifyPasscodeBinding

class PasscodeVerifyDialog(
    private val activity: FragmentActivity,
    private val passcodeManager: PasscodeManager,
    private val onSuccess: () -> Unit,
    private val onCancel: () -> Unit
) {
    private val dialog = Dialog(activity)
    private val binding = DialogVerifyPasscodeBinding.inflate(LayoutInflater.from(activity))

    init {
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(binding.root)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setLayout(
            (activity.resources.displayMetrics.widthPixels * 0.9).toInt(),
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog.setCancelable(false)

        // Ẩn nút vân tay nếu thiết bị không hỗ trợ
        val biometricManager = BiometricManager.from(activity)
        val canUseBiometric = biometricManager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_WEAK
        ) == BiometricManager.BIOMETRIC_SUCCESS

        binding.btnFingerprint.visibility =
            if (canUseBiometric) android.view.View.VISIBLE else android.view.View.GONE

        binding.btnFingerprint.setOnClickListener {
            showBiometricPrompt()
        }

        binding.btnConfirm.setOnClickListener {
            val entered = binding.edtPasscode.text?.toString()?.trim() ?: ""
            if (entered == passcodeManager.getPasscode()) {
                dialog.dismiss()
                onSuccess()
            } else {
                binding.textPasscode.error = "Incorrect passcode"
            }
        }

        binding.btnCancel.setOnClickListener {
            dialog.dismiss()
            onCancel()
        }
    }

    private fun showBiometricPrompt() {
        val executor = ContextCompat.getMainExecutor(activity)
        val biometricPrompt = BiometricPrompt(
            activity, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    dialog.dismiss()
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {

                }

                override fun onAuthenticationFailed() {
                }
            }
        )

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Verify Identity")
            .setSubtitle("Use fingerprint to access playlist")
            .setNegativeButtonText("Use PIN instead")
            .build()

        biometricPrompt.authenticate(promptInfo)
    }

    fun show() = dialog.show()
}