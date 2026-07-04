package com.iptvplayer.m3u.stream.ui.home.list_channel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.iptvplayer.m3u.stream.model.dao.FavouriteChannelDao
import com.iptvplayer.m3u.stream.model.dao.PlaylistDao
import com.iptvplayer.m3u.stream.model.dao.RecentChannelDao
import com.iptvplayer.m3u.stream.model.entity.Channel
import com.iptvplayer.m3u.stream.model.entity.FavouriteChannel
import com.iptvplayer.m3u.stream.model.entity.RecentChannel
import dagger.hilt.android.lifecycle.HiltViewModel
import hoang.dqm.codebase.base.viewmodel.BaseViewModel
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class ListChannelViewModel @Inject constructor(
    private val playlistDao: PlaylistDao,
    private val favouriteChannelDao: FavouriteChannelDao,
    private val recentChannelDao: RecentChannelDao
) : BaseViewModel() {

    private var originalList: List<Channel> = emptyList()
    private var currentPlaylistId: Long = 0
    private var _currentQuery: String = ""

    val filteredChannels = MutableLiveData<List<Channel>>()

    fun setChannels(playlistId: Long, channels: List<Channel>) {
        currentPlaylistId = playlistId
        originalList = channels
        applyFilter(_currentQuery)
    }

    fun search(query: String) {
        _currentQuery = query
        applyFilter(query)
    }

    private fun applyFilter(query: String) {
        val trimmed = query.trim()
        filteredChannels.value = if (trimmed.isEmpty()) {
            originalList
        } else {
            originalList.filter {
                it.name.contains(trimmed, ignoreCase = true) ||
                        it.group?.contains(trimmed, ignoreCase = true) == true
            }
        }
    }

    fun toggleFavourite(channel: Channel) {
        viewModelScope.launch {
            val newIsFavourite = !channel.isFavourite

            originalList = originalList.map {
                if (it.id == channel.id) it.copy(isFavourite = newIsFavourite) else it
            }

            playlistDao.updateFavourite(
                channelId = channel.id ?: return@launch,
                isFavourite = newIsFavourite,
                channel.playlistId
            )

            val favouriteChannel = FavouriteChannel(
                id = channel.id ?: UUID.randomUUID().toString(),
                name = channel.name,
                logo = channel.logo,
                group = channel.group,
                url = channel.url,
                playlistId = currentPlaylistId
            )

            if (newIsFavourite) {
                favouriteChannelDao.addFavouriteChannel(favouriteChannel)
            } else {
                favouriteChannelDao.removeFavouriteChannel(favouriteChannel)
            }

            applyFilter(_currentQuery)
        }
    }

    fun addRecentChannel(channel: Channel){
        viewModelScope.launch {
            recentChannelDao.insertRecent(RecentChannel(
                channelId = channel.id?: UUID.randomUUID().toString(),
                channelIcon = channel.logo,
                lastWatchTime = System.currentTimeMillis(),
                url = channel.url,
                name = channel.name?:"",
                playlistId = currentPlaylistId,
                group = channel.group
            ))
        }
    }
}