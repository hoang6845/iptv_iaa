package com.iptvplayer.m3u.stream.ui.episodeFragment

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.iptvplayer.m3u.stream.model.entity.SeriesDetailResponse

class ShareViewModel : ViewModel() {
    private val _listSeries: MutableLiveData<SeriesDetailResponse?> = MutableLiveData()

    fun setSeriesDetail(listSeries: SeriesDetailResponse?) {
        _listSeries.value = listSeries
    }

    val listSeries: LiveData<SeriesDetailResponse?>
        get() = _listSeries
}