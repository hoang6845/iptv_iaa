package com.iptvplayer.m3u.stream.ui.series_detail

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.iptvplayer.m3u.stream.api.SeriesRepository
import com.iptvplayer.m3u.stream.model.dao.FavouriteDao
import com.iptvplayer.m3u.stream.model.entity.Favourite
import com.iptvplayer.m3u.stream.model.entity.SeriesDetailResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import hoang.dqm.codebase.base.viewmodel.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SeriesDetailViewModel @Inject constructor(
    private val favouriteDao: FavouriteDao
) : BaseViewModel() {
    private val _seriesInfo = MutableStateFlow<SeriesDetailResponse?>(null)
    val seriesInfo = _seriesInfo.asStateFlow()
    private val _fetchError = MutableStateFlow(false)
    val fetchError = _fetchError.asStateFlow()
    fun getSeriesInfo(
        repository: SeriesRepository,
        username: String,
        password: String,
        seriesId: Long
    ) {
        viewModelScope.launch {
            repository.getSeriesInfo(username = username, password = password, seriesId = seriesId)
                .onSuccess { series ->
                    _seriesInfo.value = series
                    _fetchError.value = false
                    Log.d("check fetch", "getMovieInfo: success fetch $series")
                }
                .onFailure {
                    _seriesInfo.value = null
                    _fetchError.value = true

                }
        }
    }
    fun resetFetchError() {
        _fetchError.value = false
    }
    private val _isFavourite = MutableStateFlow(false)
    val isFavourite = _isFavourite.asStateFlow()

    private var initialFavourite = false
    fun setInitialFavourite(value: Boolean) {
        initialFavourite = value
    }

    fun toggleFavourite() {
        _isFavourite.value = !_isFavourite.value
    }

    fun loadFavourite(server: Int, uniqueId: String) {
        viewModelScope.launch {
            val fav = favouriteDao.isFavourite(server, uniqueId)
            _isFavourite.value = fav
            setInitialFavourite(fav)
        }
    }

    fun saveFavourite(server: Int, uniqueId: String) {
        viewModelScope.launch {
            if (initialFavourite != _isFavourite.value) {

                if (_isFavourite.value) {
                    favouriteDao.addFavourite(Favourite(server, uniqueId))
                } else {
                    favouriteDao.removeFavourite(server, uniqueId)
                }

            }
        }
    }
}