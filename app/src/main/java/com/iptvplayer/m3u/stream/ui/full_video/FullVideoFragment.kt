package com.iptvplayer.m3u.stream.ui.full_video

import android.util.Log
import com.iptvplayer.m3u.stream.databinding.FragmentFullVideoBinding
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import hoang.dqm.codebase.base.activity.BaseFragment


class FullVideoFragment : BaseFragment<FragmentFullVideoBinding, FullVideoViewModel>() {
    private val videoId: String? by lazy {
        arguments?.getString("videoId")
    }
    override fun initView() {
        lifecycle.addObserver(binding.youtubePlayerView)

        binding.youtubePlayerView.addYouTubePlayerListener(object : AbstractYouTubePlayerListener() {
            override fun onReady(youTubePlayer: YouTubePlayer) {
                videoId?.let {
                    Log.d("check load video", "onReady: $it")
                    youTubePlayer.cueVideo(it, 0f)
                }
            }

            override fun onError(
                youTubePlayer: YouTubePlayer,
                error: PlayerConstants.PlayerError
            ) {
                Log.e("check load video", "error = $error") // VIDEO_NOT_PLAYABLE_IN_EMBEDDED_PLAYER
//                openYoutube(videoId!!)
            }
        })
    }

    override fun initListener() {

    }

    override fun initData() {

    }
}