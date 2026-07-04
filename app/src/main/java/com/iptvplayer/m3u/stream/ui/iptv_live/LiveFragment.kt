package com.iptvplayer.m3u.stream.ui.iptv_live

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.recyclerview.widget.LinearLayoutManager
import com.iptvplayer.m3u.stream.R
import com.iptvplayer.m3u.stream.databinding.FragmentLiveBinding
import com.iptvplayer.m3u.stream.main.MainActivity
import com.iptvplayer.m3u.stream.main.MainViewModel
import com.iptvplayer.m3u.stream.main.PlayerManager
import com.iptvplayer.m3u.stream.model.entity.Channel
import com.iptvplayer.m3u.stream.model.entity.ChannelPopular
import com.iptvplayer.m3u.stream.utils.gone
import com.iptvplayer.m3u.stream.utils.visible
import dagger.hilt.android.AndroidEntryPoint
import hoang.dqm.codebase.base.activity.BaseFragment
import kotlinx.coroutines.launch


@AndroidEntryPoint
class LiveFragment : BaseFragment<FragmentLiveBinding, LiveViewModel>(), LiveTvController {
    private val mainViewModel by activityViewModels<MainViewModel>()
    private lateinit var player: ExoPlayer
    private val channelAdapter: ChannelLiveAdapter by lazy {
        ChannelLiveAdapter()
    }
    private var isFullscreen = false
    val channelSelected: ChannelPopular? by lazy {
        arguments?.getParcelable("channelSelected")
    }

    override fun initView() {
        adjustInsetsForBottomNavigation(binding.root)
        setupPlayer()
        if (channelSelected == null){
            binding.videoContainer.gone()
        }
        channelSelected?.let {
            playChannel(it)
        }
        setupRecyclerView()
    }

    override fun initListener() {
        binding.playerView.post {
            val btnFullscreen = binding.playerView.findViewById<ImageButton>(R.id.btnFullscreen)
            val btnSettings = binding.playerView.findViewById<ImageButton>(R.id.btnFullscreen)
            val btnMiniPlayer = binding.playerView.findViewById<ImageButton>(R.id.btnBack)

            btnFullscreen?.setOnClickListener {
                toggleFullscreen()
            }

            btnSettings?.setOnClickListener {
                showSettingsDialog()
            }

            btnMiniPlayer?.setOnClickListener {
                goBackWithMiniPlayer()
            }
        }

    }

    override fun initData() {
        lifecycleScope.launch {
            viewModel.channels.collect { list ->
                channelAdapter.setList(list)
            }
        }

    }

    private fun toggleFullscreen() {
        if (isFullscreen) {
            (requireActivity() as AppCompatActivity).supportActionBar?.show()
            requireActivity().window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
            binding.playerView.post {
                val btnFullscreen = binding.playerView.findViewById<ImageButton>(R.id.btnFullscreen)
                btnFullscreen.setImageResource(android.R.drawable.ic_menu_crop)
            }
            binding.channelRecyclerView.visibility = View.VISIBLE
        } else {
            (requireActivity() as AppCompatActivity).supportActionBar?.hide()
            requireActivity().window.decorView.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_FULLSCREEN
                            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    )
            binding.playerView.post {
                val btnFullscreen = binding.playerView.findViewById<ImageButton>(R.id.btnFullscreen)
                btnFullscreen.setImageResource(android.R.drawable.ic_menu_revert)
            }
            binding.channelRecyclerView.visibility = View.GONE
        }
        isFullscreen = !isFullscreen
    }

    @OptIn(UnstableApi::class)
    private fun setupPlayer() {
        player = PlayerManager.getPlayer(requireContext())
//        player = ExoPlayer.Builder(requireContext()).build()
        binding.playerView.player = player
        binding.playerView.useController = true
        binding.playerView.controllerShowTimeoutMs = 3000
        binding.playerView.controllerHideOnTouch = true
        binding.playerView.resizeMode = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FILL
    }

    private fun setupRecyclerView() {
        channelAdapter.setOnClickItemAdapter { item, position ->
            playChannel(item)
        }
        binding.channelRecyclerView.apply {
            layoutManager = LinearLayoutManager(
                requireContext(),
                LinearLayoutManager.VERTICAL,
                false
            )
            adapter = channelAdapter
        }
    }


    private fun playChannel(channel: ChannelPopular) {
        binding.videoContainer.visible()
        binding.playerView.post {
            val tvChannelName = binding.playerView.findViewById<TextView>(R.id.tvChannelName)
            tvChannelName.text = channel.name
        }
//        channelAdapter.setSelectedPosition(position)

        val mediaItem = MediaItem.fromUri(channel.url)
        player.setMediaItem(mediaItem)
        player.prepare()
        player.play()
    }

    private fun showSettingsDialog() {
        val options = arrayOf("Quality", "Speed", "Information", "Cancel")
        val builder = android.app.AlertDialog.Builder(requireContext())
        builder.setTitle("Settings")
        builder.setItems(options) { dialog, which ->
            when (which) {
                0 -> Toast.makeText(requireContext(), "Chọn chất lượng video", Toast.LENGTH_SHORT)
                    .show()

                1 -> Toast.makeText(requireContext(), "Chọn tốc độ phát", Toast.LENGTH_SHORT).show()
                2 -> showChannelInfo()
            }
        }
        builder.show()
    }

    private fun showChannelInfo() {
        channelSelected?.let { channel ->
            val builder = android.app.AlertDialog.Builder(requireContext())
            builder.setTitle("Thông tin kênh")
            builder.setMessage("Tên: ${channel.name}\n\nURL: ${channel.url}")
            builder.setPositiveButton("OK", null)
            builder.show()
        }

    }

    override fun onDestroy() {
        super.onDestroy()
//        player.release()
    }

    override fun onPause() {
        super.onPause()
//        player.pause()
    }

    override fun onResume() {
        super.onResume()
        binding.playerView.player = PlayerManager.getPlayer(requireContext())
    }

    private fun goBackWithMiniPlayer() {
        channelSelected?.let { channel ->
            val activity = requireActivity() as? MainActivity
//            activity?.showMiniPlayer(channel)
//            popBackStack()
            channelSelected?.let {
                activity?.minimizeToMiniPlayer(it)
            }
        }
    }

    companion object {
        fun newInstance(channel: Channel?): LiveFragment {
            val args = Bundle().apply {
                putParcelable("channelSelected", channel)
            }

            val fragment = LiveFragment()
            fragment.arguments = args
            return fragment
        }
    }

    override fun minimize() {
        binding.bottomChannel.gone()
        binding.playerView.useController = false
        Log.d("Mini player", "minimize:  ${binding.playerView.width} ${binding.playerView.height}")
    }

    override fun expand() {
        binding.bottomChannel.visible()
        binding.playerView.useController = true
    }

    override fun close() {
        Log.d(
            "Mini player",
            "minimize:  ${binding.playerView.width} ${binding.playerView.height} ${binding.bottomChannel.isVisible}"
        )
    }
}