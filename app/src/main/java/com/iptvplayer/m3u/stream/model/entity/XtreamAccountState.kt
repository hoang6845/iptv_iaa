package com.iptvplayer.m3u.stream.model.entity

sealed class XtreamAccountState {
    object Idle : XtreamAccountState()
    object Loading : XtreamAccountState()
    data class Success(
        val data: LoginResponse
    ) : XtreamAccountState()

    data class  Error(val message: String) : XtreamAccountState()
}