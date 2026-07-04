package com.iptvplayer.m3u.stream.ui.watch_channel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.iptvplayer.m3u.stream.model.dao.FavouriteChannelDao
import com.iptvplayer.m3u.stream.model.dao.PlaylistDao
import com.iptvplayer.m3u.stream.model.entity.FavouriteChannel
import com.iptvplayer.m3u.stream.model.entity.PlaylistEntity
import com.iptvplayer.m3u.stream.model.entity.PlaylistWithChannels
import dagger.hilt.android.lifecycle.HiltViewModel
import hoang.dqm.codebase.base.viewmodel.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WatchChannelViewModel @Inject constructor(
    private val favouriteDao: FavouriteChannelDao,
    private val playlistDao: PlaylistDao
): BaseViewModel() {
    private val _position = MutableLiveData<Long>(0L)
    val position: LiveData<Long> get() = _position
    fun setPosition(newPosition: Long){
        _position.value = newPosition
    }

    private val _playlist = MutableStateFlow<PlaylistWithChannels>(PlaylistWithChannels(PlaylistEntity(0, "", ""), emptyList()))
    val playlist: StateFlow<PlaylistWithChannels> = _playlist

    fun getPlaylist(id: Long) {
        viewModelScope.launch {
            playlistDao.getItems(id).collect {
                _playlist.value = it
            }
        }
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

    fun loadFavourite(playlistId: Long, channelId: String) {
        viewModelScope.launch {
            val fav = favouriteDao.isFavourite(channelId, playlistId)
            _isFavourite.value = fav
            setInitialFavourite(fav)
        }
    }

    private val TAG = "SaveFavourite"

    suspend fun saveFavourite(playlistId: Long, channelId: String) {

        val currentFav = _isFavourite.value

        if (initialFavourite == currentFav) {
            return
        }

        // Update đúng 1 row, không cần load cả playlist
        playlistDao.updateFavourite(channelId, currentFav, playlistId)

        val channel = playlistDao.getChannelById(channelId) ?: run {
            return
        }

        val favouriteChannel = FavouriteChannel(
            id = channelId,
            name = channel.name,
            logo = channel.logo,
            group = channel.group,
            url = channel.url,
            playlistId = playlistId,
            isFavourite = true
        )

        if (currentFav) {
            favouriteDao.addFavouriteChannel(favouriteChannel)
        } else {
            favouriteDao.removeFavouriteChannel(favouriteChannel)
        }
    }
}