package com.iptvplayer.m3u.stream.ui.gallery

import android.app.Activity
import android.content.Intent
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.navigation.fragment.findNavController
import com.iptvplayer.m3u.stream.R
import com.iptvplayer.m3u.stream.databinding.FragmentAddGalleryBinding
import com.iptvplayer.m3u.stream.ui.home.HomeFragment
import dagger.hilt.android.AndroidEntryPoint
import hoang.dqm.codebase.base.activity.BaseFragment

@AndroidEntryPoint
class AddGalleryFragment : BaseFragment<FragmentAddGalleryBinding, GalleryViewModel>() {

    private lateinit var videoAdapter: VideoAdapter
    private var isVideoPickerOpening = false

    private val pickVideoLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            isVideoPickerOpening = false
            binding.btnUploadVideo.isEnabled = true

            if (result.resultCode == Activity.RESULT_OK) {
                val data = result.data ?: return@registerForActivityResult
                val uris = mutableListOf<android.net.Uri>()

                if (data.clipData != null) {
                    val clipData = data.clipData!!
                    for (i in 0 until clipData.itemCount) {
                        uris.add(clipData.getItemAt(i).uri)
                    }
                } else {
                    data.data?.let { uris.add(it) }
                }

                viewModel.onVideosSelected(uris)
            }
        }


    override fun initView() {
        adjustInsetsForBottomNavigation(binding.toolBar)

        videoAdapter = VideoAdapter { channel ->
            viewModel.removeChannel(channel)
        }

        binding.rcvVideos.adapter = videoAdapter
    }

    override fun initListener() {
        binding.btnClose.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        binding.btnUploadVideo.setOnClickListener {
            openVideoPicker()
        }

        binding.btnSave.setOnClickListener {
            val playlistName = binding.edtPlaylistName.text?.toString()?.trim()

            if (playlistName.isNullOrEmpty()) {
                binding.textName.error = getString(R.string.text_enter_playlist_name)
                return@setOnClickListener
            }

            if (viewModel.channels.value.isNullOrEmpty()) {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.text_please_select_at_least_1_file),
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            binding.textName.error = null
            viewModel.savePlaylist(playlistName)
        }
    }

    override fun initData() {
        viewModel.channels.observe(viewLifecycleOwner) { channels ->
            videoAdapter.submitList(channels.toList())
            binding.rcvVideos.visibility = if (channels.isEmpty()) View.GONE else View.VISIBLE
        }

        viewModel.saveState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is GallerySaveState.Idle -> setLoadingVisible(false)
                is GallerySaveState.Loading -> setLoadingVisible(false)
                is GallerySaveState.Success -> {
                    setLoadingVisible(false)
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.text_save_playlist_success),
                        Toast.LENGTH_SHORT
                    ).show()
                    popToHomeGalleryTab()
                }
                is GallerySaveState.Error -> {
                    setLoadingVisible(false)
                    Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun popToHomeGalleryTab() {
        findNavController()
            .previousBackStackEntry
            ?.savedStateHandle
            ?.set(HomeFragment.KEY_HOME_TAB, HomeFragment.TAB_GALLERY)

        parentFragmentManager.popBackStack()
    }

    private fun openVideoPicker() {
        if (isVideoPickerOpening) return

        isVideoPickerOpening = true
        binding.btnUploadVideo.isEnabled = false

        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "video/*"
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        }

        pickVideoLauncher.launch(Intent.createChooser(intent, "Select Videos"))
    }
    private fun setLoadingVisible(visible: Boolean) {
        binding.layoutLoading.visibility = if (visible) View.VISIBLE else View.GONE
        binding.btnSave.isEnabled = !visible
    }

    override fun onResume() {
        super.onResume()

        isVideoPickerOpening = false
        binding.btnUploadVideo.isEnabled = true
    }

}