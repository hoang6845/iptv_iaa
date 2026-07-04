package com.iptvplayer.m3u.stream.ui.profile_edit

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.setFragmentResultListener
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.iptvplayer.m3u.stream.R
import com.iptvplayer.m3u.stream.databinding.FragmentEditProfileBinding
import com.iptvplayer.m3u.stream.utils.DeletePlaylistDialog
import com.iptvplayer.m3u.stream.utils.PasscodeManagerXtream
import dagger.hilt.android.AndroidEntryPoint
import hoang.dqm.codebase.base.activity.BaseFragment
import hoang.dqm.codebase.base.activity.navigate
import hoang.dqm.codebase.base.activity.onBackPressed
import hoang.dqm.codebase.base.activity.popBackStack
import hoang.dqm.codebase.utils.collectLatestFlow
import hoang.dqm.codebase.utils.loadImageSketch
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class EditProfileFragment : BaseFragment<FragmentEditProfileBinding, EditProfileViewModel>() {
    @Inject
    lateinit var passcodeManagerXtream: PasscodeManagerXtream

    private val serverId: Int by lazy {
        arguments?.getInt("serverId") ?: 0
    }

    private var avatarUrl: String = "avatar/avatar_1.png"

    private var isSettingProgrammatically = false


    override fun initView() {
        adjustInsetsForBottomNavigation(binding.toolBar)
        setUpObserver()

    }

    override fun initListener() {
        binding.btnBack.setOnClickListener {
            popBackStack()
        }

        onBackPressed {
            popBackStack()
        }

        binding.btnSave.setOnClickListener {
            saveProfile()
        }

        binding.btnEditAvatar.setOnClickListener {
            val bundle = Bundle().apply { putString("avatar", avatarUrl) }
            navigate(R.id.editAvatarFragment, bundle)
        }

        setFragmentResultListener("edit_avatar_result") { _, bundle ->
            if (bundle.getBoolean("isChange")) {
                bundle.getString("avatarSelected")?.let { url ->
                    avatarUrl = url
                    binding.avatar.loadImageSketch(url, isFull = false)
                }
            }
        }

        binding.layoutUrlServer.setEndIconOnClickListener {
            val url = binding.urlServer.text?.toString().orEmpty()
            if (url.isNotBlank()) {
                val clipboard =
                    requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("server_url", url))
                Toast.makeText(
                    requireContext(),
                    getString(R.string.text_copied),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        setFragmentResultListener("passcode_result") { _, bundle ->
            val passcodeSet = bundle.getBoolean("passcodeSet", false)
            if (!passcodeSet) {
                isSettingProgrammatically = true
                binding.btnToggleMusic.isChecked = false
                isSettingProgrammatically = false
                binding.btnChangePasscode.isVisible = false
                binding.line.visibility = View.INVISIBLE
            }
        }

        binding.btnChangePasscode.setOnClickListener {
            val bundle = Bundle().apply {
                putBoolean("isChangeMode", true)
                putString("avatar", avatarUrl)
            }
            navigate(R.id.passcodeXtreamFragment, bundle)
        }

        binding.btnRemoveProfile.setOnClickListener {
            showDeleteConfirmDialog()
        }

        binding.btnToggleMusic.setOnCheckedChangeListener { _, isChecked ->
            if (!isSettingProgrammatically) {
                binding.btnChangePasscode.isVisible = isChecked
                binding.line.visibility = if (isChecked) {
                    View.VISIBLE
                } else {
                    View.INVISIBLE
                }
                if (isChecked && !passcodeManagerXtream.hasPasscode()) {
                    isSettingProgrammatically = true
                    val bundle = Bundle().apply {
                        putString("avatar", avatarUrl)
                    }
                    navigate(R.id.passcodeXtreamFragment, bundle)
                }
            }
        }

        collectLatestFlow(viewModel.saveResult) { result ->
            when (result) {
                is EditProfileViewModel.SaveResult.Success -> {
                    Log.d("check edit", "initData: success $result")
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.text_save_success),
                        Toast.LENGTH_SHORT
                    ).show()
                    viewModel.resetSaveResult()
                    popBackStack()
                }
                is EditProfileViewModel.SaveResult.Error -> {
                    Log.d("check edit", "initData: error $result")
                    Toast.makeText(
                        requireContext(),
                        result.message,
                        Toast.LENGTH_SHORT
                    ).show()
                    viewModel.resetSaveResult()
                }
                else -> {
                    Log.d("check edit", "initData: else $result")
                }
            }
        }

    }

    override fun initData() {
        viewModel.loadServer(serverId)
    }


    private fun saveProfile() {
        val name     = binding.profileName.text?.toString().orEmpty()
        val username = binding.username.text?.toString().orEmpty()
        val password = binding.password.text?.toString().orEmpty()
        val url      = binding.urlServer.text?.toString().orEmpty()
        val passcode = binding.btnToggleMusic.isChecked

        viewModel.saveProfile(
            serverId        = serverId,
            name            = name,
            username        = username,
            password        = password,
            urlServer       = url,
            isEnablePasscode = passcode,
            avatar = avatarUrl
        )
    }

    private fun showDeleteConfirmDialog() {
        DeletePlaylistDialog(
            activity = requireActivity(),
            onConfirm = {
                viewModel.deleteProfile(serverId)

            }
        ).show()
    }

    private var isInitialized = false

    fun setUpObserver() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.serverState.collect { server ->
                        server ?: return@collect

                        if (!isInitialized) {
                            isInitialized = true
                            avatarUrl = server.urlAvatar

                            with(binding) {
                                profileName.setText(server.name.orEmpty())
                                username.setText(server.username)
                                password.setText(server.password)
                                urlServer.setText(server.server)

                                isSettingProgrammatically = true
                                btnToggleMusic.isChecked = server.isEnablePasscode
                                isSettingProgrammatically = false
                                btnChangePasscode.isVisible = server.isEnablePasscode
                                line.visibility = if (server.isEnablePasscode) View.VISIBLE else View.INVISIBLE
                            }
                        }

                        binding.avatar.loadImageSketch(avatarUrl, isFull = false)
                    }
                }
                launch {
                    viewModel.deleteResult.collect { result ->
                        if (result is EditProfileViewModel.DeleteResult.Success) {
                            viewModel.resetDeleteResult()
                            popBackStack(R.id.xtreamHomeFragment)
                        }
                    }
                }
            }
        }
    }
}