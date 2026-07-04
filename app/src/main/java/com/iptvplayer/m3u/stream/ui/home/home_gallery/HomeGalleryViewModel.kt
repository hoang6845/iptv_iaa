package com.iptvplayer.m3u.stream.ui.home.home_gallery

import androidx.lifecycle.viewModelScope
import com.iptvplayer.m3u.stream.model.dao.FavouriteChannelDao
import com.iptvplayer.m3u.stream.model.dao.PlaylistDao
import com.iptvplayer.m3u.stream.model.entity.FavouriteChannel
import dagger.hilt.android.lifecycle.HiltViewModel
import hoang.dqm.codebase.base.viewmodel.BaseViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeGalleryViewModel @Inject constructor(
    private val favouriteChannelDao: FavouriteChannelDao,
    private val playlistDao: PlaylistDao
): BaseViewModel() {
    val favouritePlaylists: StateFlow<List<FavouriteChannel>> = favouriteChannelDao.getAllFavouriteChannels()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun updateName(id: Long, name: String){
        viewModelScope.launch {
            playlistDao.updatePlaylistName(id, name)
        }

    }
    fun deletePlaylist(id: Long){
        viewModelScope.launch {
            playlistDao.deletePlaylist(id)
        }
    }
}