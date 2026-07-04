package com.iptvplayer.m3u.stream.ui.xtream_movie

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import hoang.dqm.codebase.base.viewmodel.BaseViewModel

class XtreamMovieViewModel: BaseViewModel() {
    private val _position = MutableLiveData<Long>(0L)
    val position: LiveData<Long> get() = _position
    fun setPosition(newPosition: Long){
        _position.value = newPosition
    }

    private val _isPlaying = MutableLiveData<Boolean>(true)
    val isPlaying: LiveData<Boolean> get() = _isPlaying
    fun setPlay(value: Boolean){
        _isPlaying.value = value
    }
}