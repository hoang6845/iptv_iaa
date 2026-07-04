package com.iptvplayer.m3u.stream.main

import android.content.Context
import androidx.media3.exoplayer.ExoPlayer


object PlayerManager {
    private var player: ExoPlayer? = null
    
    fun getPlayer(context: Context): ExoPlayer {
        if (player == null) {
            player = ExoPlayer.Builder(context.applicationContext).build()
        }
        return player!!
    }
    
    fun releasePlayer() {
        player?.release()
        player = null
    }
    
    fun pausePlayer() {
        player?.pause()
    }
    
    fun resumePlayer() {
        player?.play()
    }
    
    fun isPlaying(): Boolean {
        return player?.isPlaying ?: false
    }
}