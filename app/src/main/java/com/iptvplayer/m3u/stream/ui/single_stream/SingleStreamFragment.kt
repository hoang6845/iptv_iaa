package com.iptvplayer.m3u.stream.ui.single_stream

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import com.iptvplayer.m3u.stream.R
import com.iptvplayer.m3u.stream.databinding.FragmentSingleStreamBinding
import dagger.hilt.android.AndroidEntryPoint
import hoang.dqm.codebase.base.activity.BaseFragment
import hoang.dqm.codebase.base.activity.onBackPressed
import hoang.dqm.codebase.base.activity.popBackStack
import hoang.dqm.codebase.utils.singleClick

@AndroidEntryPoint
class SingleStreamFragment : BaseFragment<FragmentSingleStreamBinding, SingleStreamViewModel>() {

    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val uri: Uri? = result.data?.data
                uri?.let {
                    viewModel.onLogoSelected(it)
                    binding.img.setImageURI(it)
                }
            }
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }

    override fun initView() {
        adjustInsetsForBottomNavigation(binding.toolBar)
    }

    override fun initListener() {
        onBackPressed { popBackStack() }
        binding.btnClose.singleClick { popBackStack() }
        binding.img.setOnClickListener {
            openImagePicker()
        }

        binding.btnSave.setOnClickListener {
            val name = binding.edtPlaylistName.text?.toString()?.trim()
            val url  = binding.edtUrl.text?.toString()?.trim()
            val group = binding.edtGroup.text?.toString()?.trim()

            if (name.isNullOrEmpty()) {
                binding.edtPlaylistName.error = getString(R.string.text_enter_channel_name)
                return@setOnClickListener
            }
            if (url.isNullOrEmpty()) {
                binding.edtUrl.error = getString(R.string.text_enter_url)
                return@setOnClickListener
            }

            binding.edtPlaylistName.error = null
            binding.edtUrl.error = null

            viewModel.saveChannel(name, url, group)
        }
    }

    override fun initData() {
        viewModel.saveState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is SaveChannelState.Idle -> setLoadingVisible(false)

                is SaveChannelState.Loading -> setLoadingVisible(true)

                is SaveChannelState.Success -> {
                    setLoadingVisible(false)
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.text_save_playlist_success),
                        Toast.LENGTH_SHORT
                    ).show()
                    parentFragmentManager.popBackStack()
                }

                is SaveChannelState.Error -> {
                    setLoadingVisible(false)
                    Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
                }
            }
        }

        viewModel.selectedLogoUri.observe(viewLifecycleOwner) { uri ->
            if (uri == null) {
                binding.img.setImageResource(R.drawable.ic_add)
            }
        }
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK).apply {
            type = "image/*"
        }
        pickImageLauncher.launch(intent)
    }

    private fun setLoadingVisible(visible: Boolean) {
        binding.layoutLoading.visibility = if (visible) View.VISIBLE else View.GONE
        binding.btnSave.isEnabled = !visible
    }
}