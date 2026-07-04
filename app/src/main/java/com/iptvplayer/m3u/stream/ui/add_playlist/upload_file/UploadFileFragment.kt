package com.iptvplayer.m3u.stream.ui.add_playlist.upload_file

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import com.iptvplayer.m3u.stream.R
import com.iptvplayer.m3u.stream.databinding.FragmentUploadFileBinding
import com.iptvplayer.m3u.stream.ui.add_playlist.PasscodeDialog
import com.iptvplayer.m3u.stream.ui.home.HomeFragment
import com.iptvplayer.m3u.stream.utils.PasscodeManager
import dagger.hilt.android.AndroidEntryPoint
import hoang.dqm.codebase.base.activity.BaseFragment
import hoang.dqm.codebase.base.activity.navigate
import hoang.dqm.codebase.base.activity.onBackPressed
import hoang.dqm.codebase.base.activity.popBackStack
import javax.inject.Inject

@AndroidEntryPoint
class UploadFileFragment : BaseFragment<FragmentUploadFileBinding, UploadFileViewModel>() {
    @Inject
    lateinit var passcodeManager: PasscodeManager
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.fileUpload.registerLauncher(this)
    }

    override fun initView() {
    }

    override fun initListener() {
        binding.textHowtoUpload.paint.isUnderlineText = true
        binding.textHowtoUpload.setOnClickListener {
            val bundle = Bundle().apply {
                putInt("position", 2)
            }
            navigate(R.id.howToYouFragment, bundle)
        }
        onBackPressed { popBackStack() }

        binding.fileUpload.onFilesChanged = { uris ->
            viewModel.onFilesSelected(uris)
        }

        binding.btnSave.setOnClickListener {
            val playlistName = binding.edtPlaylistName.text?.toString()?.trim()

            if (playlistName.isNullOrEmpty()) {
                binding.textName.error = getString(com.iptvplayer.m3u.stream.R.string.text_enter_playlist_name)
                return@setOnClickListener
            }

            if (viewModel.selectedFiles.value.isNullOrEmpty()) {
                Toast.makeText(
                    requireContext(),
                    getString(com.iptvplayer.m3u.stream.R.string.text_please_select_at_least_1_file),
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            binding.textName.error = null

            val isPasscodeEnabled = binding.btnToggleMusic.isChecked

            if (isPasscodeEnabled && !passcodeManager.hasPasscode()) {
                showSetPasscodeDialog(onSuccess = {
                    viewModel.savePlaylist(playlistName, true)
                })
            } else {
                viewModel.savePlaylist(playlistName, isPasscodeEnabled)
            }
        }
    }

    override fun initData() {
        viewModel.saveState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is SaveState.Idle -> {
                    setLoadingVisible(false)
                }

                is SaveState.Loading -> {
                    setLoadingVisible(true)
                    binding.tvProgress.text = "${state.progress}%"
                }

                is SaveState.Success -> {
                    setLoadingVisible(false)
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.text_save_playlist_success),
                        Toast.LENGTH_SHORT
                    ).show()
                    popToHomeFileTab()
                }

                is SaveState.Error -> {
                    setLoadingVisible(false)
                    Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun popToHomeFileTab() {
        findNavController()
            .previousBackStackEntry
            ?.savedStateHandle
            ?.set(HomeFragment.KEY_HOME_TAB, HomeFragment.TAB_FILE)

        popBackStack()
    }

    private fun setLoadingVisible(visible: Boolean) {
        binding.layoutLoading.visibility = if (visible) View.VISIBLE else View.GONE
        binding.btnSave.isEnabled = !visible
    }

    private fun showSetPasscodeDialog(onSuccess: () -> Unit) {
        PasscodeDialog(
            context = requireContext(),
            onConfirm = { passcode ->
                passcodeManager.savePasscode(passcode)
                onSuccess()
            },
            onCancel = {

            }
        ).show()
    }
}