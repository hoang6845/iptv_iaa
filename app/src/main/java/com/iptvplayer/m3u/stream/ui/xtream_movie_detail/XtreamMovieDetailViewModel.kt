package com.iptvplayer.m3u.stream.ui.xtream_movie_detail

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.iptvplayer.m3u.stream.api.MovieRepository
import com.iptvplayer.m3u.stream.model.dao.FavouriteDao
import com.iptvplayer.m3u.stream.model.entity.Favourite
import com.iptvplayer.m3u.stream.model.entity.MovieDetailResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import hoang.dqm.codebase.base.viewmodel.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class XtreamMovieDetailViewModel @Inject constructor(
    private val favouriteDao: FavouriteDao
) : BaseViewModel() {
    private val _movieInfo = MutableStateFlow<MovieDetailResponse?>(null)
    val movieInfo = _movieInfo.asStateFlow()
    private val _fetchError = MutableStateFlow(false)
    val fetchError = _fetchError.asStateFlow()
    fun getMovieInfo(repository: MovieRepository, username: String, password: String, vodId: Long){
        viewModelScope.launch {
            repository.getMovieInfo(username, password, vodId)
                .onSuccess { movie ->
                    _movieInfo.value = movie
                    _fetchError.value = false
                    Log.d("check fetch", "getMovieInfo: success fetch $movie")
                }
                .onFailure {
                    _movieInfo.value = null
                    _fetchError.value = true
                }
        }
    }

    fun resetFetchError() {
        _fetchError.value = false
    }

    fun setMovieInfo(movieInfo: MovieDetailResponse) {
        _movieInfo.value = movieInfo
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