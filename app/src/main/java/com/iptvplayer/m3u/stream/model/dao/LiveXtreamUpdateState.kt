package com.iptvplayer.m3u.stream.model.dao

sealed class LiveXtreamUpdateState {
    object Idle : LiveXtreamUpdateState()
    data class Loading(val progress: Int) : LiveXtreamUpdateState()
    object Success : LiveXtreamUpdateState()
    data class Error(val message: String) : LiveXtreamUpdateState()
}