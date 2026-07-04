package com.iptvplayer.m3u.stream.main

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class SharedViewModel : ViewModel() {

    val dataServerId = MutableLiveData<Int>()

    fun setData(value: Int) {
        dataServerId.value = value
    }
}