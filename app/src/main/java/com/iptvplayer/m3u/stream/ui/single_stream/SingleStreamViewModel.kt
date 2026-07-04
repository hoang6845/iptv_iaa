package com.iptvplayer.m3u.stream.ui.single_stream

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.iptvplayer.m3u.stream.model.dao.PlaylistDao
import com.iptvplayer.m3u.stream.model.entity.Channel
import com.iptvplayer.m3u.stream.model.entity.PlaylistEntity
import com.iptvplayer.m3u.stream.model.entity.PlaylistWithChannels
import com.iptvplayer.m3u.stream.utils.AppConstants
import dagger.hilt.android.lifecycle.HiltViewModel
import hoang.dqm.codebase.base.viewmodel.BaseViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class SaveChannelState {
    object Idle : SaveChannelState()
    object Loading : SaveChannelState()
    object Success : SaveChannelState()
    data class Error(val message: String) : SaveChannelState()
}

@HiltViewModel
class SingleStreamViewModel @Inject constructor(
    private val playlistDao: PlaylistDao
) : BaseViewModel() {

    private val _saveState = MutableLiveData<SaveChannelState>(SaveChannelState.Idle)
    val saveState: LiveData<SaveChannelState> = _saveState

    private val _selectedLogoUri = MutableLiveData<Uri?>(null)
    val selectedLogoUri: LiveData<Uri?> = _selectedLogoUri

    fun onLogoSelected(uri: Uri?) {
        _selectedLogoUri.value = uri
    }

    fun saveChannel(name: String, url: String, group: String?) {
        viewModelScope.launch {
            try {
                _saveState.value = SaveChannelState.Loading

                val logoPath = _selectedLogoUri.value?.toString()

                val playlist = playlistDao.getPlaylistByType(
                    AppConstants.TYPE_PLAYLIST_SINGLE_STREAM
                ) ?: PlaylistWithChannels(PlaylistEntity(0, "Single Stream", AppConstants.TYPE_PLAYLIST_SINGLE_STREAM), emptyList())
                val channel = Channel(
                    id = System.currentTimeMillis().toString(),
                    name = name,
                    logo = logoPath,
                    group = group,
                    url = url,
                    isFavourite = false
                )
                val newChannels = playlist.channels + channel
                playlist.channels = newChannels
                playlistDao.insertPlaylistWithChannels(playlist.playlist, playlist.channels)

                _saveState.value = SaveChannelState.Success

            } catch (e: Exception) {
                _saveState.value = SaveChannelState.Error(e.message ?: "Unknown error")
            }
        }
    }
}