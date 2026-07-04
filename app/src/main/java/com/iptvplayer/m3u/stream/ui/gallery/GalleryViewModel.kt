package com.iptvplayer.m3u.stream.ui.gallery

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.iptvplayer.m3u.stream.model.dao.PlaylistDao
import com.iptvplayer.m3u.stream.model.entity.Channel
import com.iptvplayer.m3u.stream.model.entity.PlaylistEntity
import com.iptvplayer.m3u.stream.utils.AppConstants
import dagger.hilt.android.lifecycle.HiltViewModel
import hoang.dqm.codebase.base.viewmodel.BaseViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class GallerySaveState {
    object Idle : GallerySaveState()
    object Loading : GallerySaveState()
    object Success : GallerySaveState()
    data class Error(val message: String) : GallerySaveState()
}

@HiltViewModel
class GalleryViewModel @Inject constructor(
    private val playlistDao: PlaylistDao
) : BaseViewModel() {

    private val _channels = MutableLiveData<MutableList<Channel>>(mutableListOf())
    val channels: LiveData<MutableList<Channel>> = _channels

    private val _saveState = MutableLiveData<GallerySaveState>(GallerySaveState.Idle)
    val saveState: LiveData<GallerySaveState> = _saveState

    fun onVideosSelected(uris: List<Uri>) {
        val current = _channels.value ?: mutableListOf()
        val newChannels = uris.map { uri ->
            Channel(
                id = uri.toString(),
                name = uri.lastPathSegment ?: uri.toString(),
                logo = null,
                group = null,
                url = uri.toString()
            )
        }
        current.addAll(newChannels)
        _channels.value = current
    }

    fun removeChannel(channel: Channel) {
        val current = _channels.value ?: return
        current.remove(channel)
        _channels.value = current
    }

    fun savePlaylist(playlistName: String) {
        val channelList = _channels.value
        if (channelList.isNullOrEmpty()) return

        viewModelScope.launch {
            try {
                _saveState.value = GallerySaveState.Loading
                val playlist = PlaylistEntity(name = playlistName, typePlayList = AppConstants.TYPE_PLAYLIST_GALLERY)
                playlistDao.insertPlaylistWithChannels(playlist, channelList)

                _saveState.value = GallerySaveState.Success
            } catch (e: Exception) {
                _saveState.value = GallerySaveState.Error(e.message ?: "Unknown error")
            }
        }
    }
}