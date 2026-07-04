package com.iptvplayer.m3u.stream.ui.stream

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.iptvplayer.m3u.stream.model.dao.FavouriteDao
import com.iptvplayer.m3u.stream.model.entity.Favourite
import dagger.hilt.android.lifecycle.HiltViewModel
import hoang.dqm.codebase.base.viewmodel.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StreamViewModel @Inject constructor(
    private val favouriteDao: FavouriteDao
): BaseViewModel() {
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