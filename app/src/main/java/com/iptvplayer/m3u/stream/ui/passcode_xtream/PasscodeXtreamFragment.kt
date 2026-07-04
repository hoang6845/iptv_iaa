package com.iptvplayer.m3u.stream.ui.passcode_xtream

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.content.res.ColorStateList
import android.os.Bundle
import android.util.Log
import android.view.animation.AnimationUtils
import android.widget.ImageView
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.iptvplayer.m3u.stream.R
import com.iptvplayer.m3u.stream.databinding.FragmentPasscodeXtreamBinding
import com.iptvplayer.m3u.stream.main.SharedViewModel
import com.iptvplayer.m3u.stream.utils.gone
import com.iptvplayer.m3u.stream.utils.invisible
import com.iptvplayer.m3u.stream.utils.visible
import dagger.hilt.android.AndroidEntryPoint
import hoang.dqm.codebase.base.activity.BaseFragment
import hoang.dqm.codebase.base.activity.navigate
import hoang.dqm.codebase.base.activity.onBackPressed
import hoang.dqm.codebase.base.activity.popBackStack
import hoang.dqm.codebase.utils.loadImageSketch
import kotlinx.coroutines.launch

@AndroidEntryPoint
class PasscodeXtreamFragment :
    BaseFragment<FragmentPasscodeXtreamBinding, PasscodeXtreamViewModel>() {
    private val sharedViewModel: SharedViewModel by activityViewModels()

    private val dots: List<ImageView> by lazy {
        listOf(binding.dot1, binding.dot2, binding.dot3, binding.dot4)
    }
    private val avatar: String by lazy {
        arguments?.getString("avatar") ?: "avatar/avatar_1.png"
    }

    private val isChangeMode: Boolean by lazy {
        arguments?.getBoolean("isChangeMode", false) ?: false
    }

    private val name: String by lazy {
        arguments?.getString("name") ?: "User"
    }

    private val serverId: Int by lazy {
        arguments?.getInt("serverId") ?: 0
    }
    private val biometricTriggeredForMode = mutableSetOf<PasscodeMode>()
    override fun initView() {
        adjustInsetsForBottomNavigation(binding.toolBar)
        setupNumpad()
        if (isChangeMode) viewModel.startChangePasscode()
    }

    override fun initListener() {
        binding.btnBack.setOnClickListener { handleBack() }
        onBackPressed { handleBack() }

        binding.tvReset.setOnClickListener {
            viewModel.resetPin()
            binding.tvReset.gone()
        }

        binding.btnFingerprint.setOnClickListener {
            showBiometricPrompt()
        }
    }

    override fun initData() {
        observeState()
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    Log.d("PasscodeXtream", "state=$state")
                    updateDots(state.pin.length)
                    updateTitleForMode(state.mode)
                    state.event?.let { event ->
                        handleEvent(event)
                        viewModel.consumeEvent()
                    }
                }
            }
        }
    }


    private fun updateTitleForMode(mode: PasscodeMode) {
        val isBiometricAvailable =
            (mode == PasscodeMode.VERIFY || mode == PasscodeMode.VERIFY_OLD) && canUseBiometric()

        binding.btnFingerprint.visibility =
            if (isBiometricAvailable) android.view.View.VISIBLE else android.view.View.GONE

        when (mode) {
            PasscodeMode.VERIFY -> {
                binding.ivAvatar.visible()
                binding.tvName.visible()
                binding.tvDescription.visible()
                binding.tvTitle.invisible()
                binding.ivLock.invisible()
                binding.ivAvatar.loadImageSketch(avatar, isFull = false)
                binding.tvName.text = getString(R.string.text_hello, name)

                if (isBiometricAvailable && biometricTriggeredForMode.add(mode)) {
                    showBiometricPrompt()
                }
            }

            PasscodeMode.VERIFY_OLD -> {
                hideAvatarSection()
                binding.tvTitle.text = getString(R.string.passcode_enter_old)
                binding.tvDescription.text = getString(R.string.passcode_enter_old_desc)
                binding.tvDescription.visible()

                if (isBiometricAvailable && biometricTriggeredForMode.add(mode)) {
                    showBiometricPrompt()
                }
            }

            PasscodeMode.SET_NEW -> {
                hideAvatarSection()
                binding.tvTitle.text = getString(R.string.passcode_set_new)
                binding.tvDescription.gone()
            }

            PasscodeMode.CONFIRM_NEW -> {
                hideAvatarSection()
                binding.tvTitle.text = getString(R.string.passcode_confirm)
                binding.tvDescription.gone()
            }
        }
    }

    private fun hideAvatarSection() {
        binding.ivAvatar.gone()
        binding.tvName.gone()
        binding.tvTitle.visible()
        binding.ivLock.visible()
        binding.tvDescription.gone()
    }

    private fun handleEvent(event: PasscodeEvent) {
        when (event) {

            is PasscodeEvent.Success -> {
                setFragmentResult(
                    "passcode_result",
                    Bundle().apply { putBoolean("passcodeSet", true) }
                )
                val bundle = Bundle().apply {
                    putInt("serverId", serverId)
                    putString("avatar", avatar)
                    putString("name", name)
                }
                sharedViewModel.setData(serverId)
                navigate(R.id.xtreamFragment, bundle, isPop = true)
            }

            is PasscodeEvent.WrongPin -> {
                shakeAndFlashDots()
                binding.tvReset.gone()
            }

            is PasscodeEvent.WrongOldPin -> {
                shakeAndFlashDots()
                binding.tvDescription.text = getString(R.string.passcode_wrong_old)
                binding.tvDescription.setTextColor(
                    ContextCompat.getColor(requireContext(), hoang.dqm.codebase.R.color.colorRed500)
                )
                binding.tvDescription.visible()
                binding.tvReset.gone()
            }

            is PasscodeEvent.PinMismatch -> {
                shakeAndFlashDots()
                binding.tvReset.visible()
            }

            is PasscodeEvent.ChangeSuccess -> {
                setFragmentResult(
                    "passcode_result",
                    Bundle().apply { putBoolean("passcodeSet", true) }
                )
                binding.success.visible()
                binding.tvReset.gone()
                binding.btnOk.setOnClickListener { popBackStack() }
            }
        }
    }

    private fun canUseBiometric(): Boolean =
        BiometricManager.from(requireContext())
            .canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK) ==
                BiometricManager.BIOMETRIC_SUCCESS

    private fun showBiometricPrompt() {
        val executor = ContextCompat.getMainExecutor(requireContext())

        val biometricPrompt = BiometricPrompt(
            this,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    viewModel.onBiometricSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    Log.d("PasscodeXtream", "Biometric error $errorCode: $errString")
                }

                override fun onAuthenticationFailed() {
                    Log.d("PasscodeXtream", "Biometric failed (wrong finger)")
                }
            }
        )

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(getString(R.string.biometric_title))
            .setSubtitle(getString(R.string.biometric_subtitle))
            .setNegativeButtonText(getString(R.string.biometric_use_pin))
            .build()

        biometricPrompt.authenticate(promptInfo)
    }

    private fun handleBack() {
        val currentEvent = viewModel.uiState.value.event
        if (currentEvent != PasscodeEvent.Success && currentEvent != PasscodeEvent.ChangeSuccess) {
            setFragmentResult(
                "passcode_result",
                Bundle().apply { putBoolean("passcodeSet", false) }
            )
        }
        popBackStack()
    }

    private fun setupNumpad() {
        val digitButtons = mapOf(
            binding.btn1 to "1", binding.btn2 to "2", binding.btn3 to "3",
            binding.btn4 to "4", binding.btn5 to "5", binding.btn6 to "6",
            binding.btn7 to "7", binding.btn8 to "8", binding.btn9 to "9",
            binding.btn0 to "0"
        )
        digitButtons.forEach { (button, digit) ->
            button.setOnClickListener {
                binding.tvReset.gone()
                animateButtonPress(it)
                viewModel.onDigitPressed(digit)
            }
        }
        binding.btnBackspace.setOnClickListener {
            animateButtonPress(it)
            viewModel.onBackspacePressed()
        }
    }

    private fun updateDots(filledCount: Int) {
        dots.forEachIndexed { index, dot ->
            if (index < filledCount) {
                dot.setImageResource(R.drawable.ic_dot_filled)
                dot.playScaleIn()
            } else {
                dot.setImageResource(R.drawable.ic_dot_empty)
            }
        }
    }

    private fun shakeAndFlashDots() {
        shakeDotsError()
        dots.forEach { animateTint(it) }
    }

    private fun shakeDotsError() {
        val shake = AnimationUtils.loadAnimation(requireContext(), R.anim.shake)
        binding.llPinDots.startAnimation(shake)
    }

    private fun animateTint(view: ImageView) {
        val from = ContextCompat.getColor(requireContext(), hoang.dqm.codebase.R.color.colorRed500)
        val to   = ContextCompat.getColor(requireContext(), R.color.color_primary)
        ValueAnimator.ofObject(ArgbEvaluator(), from, to).apply {
            duration = 1500
            addUpdateListener {
                view.imageTintList = ColorStateList.valueOf(it.animatedValue as Int)
            }
            start()
        }
    }

    private fun animateButtonPress(view: android.view.View) {
        view.animate().scaleX(0.88f).scaleY(0.88f).setDuration(80)
            .withEndAction {
                view.animate().scaleX(1f).scaleY(1f).setDuration(80).start()
            }.start()
    }

    private fun ImageView.playScaleIn() {
        scaleX = 0f; scaleY = 0f
        animate().scaleX(1f).scaleY(1f).setDuration(150).start()
    }
}