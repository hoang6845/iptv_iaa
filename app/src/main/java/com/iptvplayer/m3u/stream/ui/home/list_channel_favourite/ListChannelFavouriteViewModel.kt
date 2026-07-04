package com.iptvplayer.m3u.stream.ui.home.list_channel_favourite

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
class ListChannelFavouriteViewModel @Inject constructor(
    private val favouriteChannelDao: FavouriteChannelDao,
    private val playlistDao: PlaylistDao,
    private val recentChannelDao: RecentChannelDao
) : BaseViewModel() {

    private var originalList: List<FavouriteChannel> = emptyList()
    private var _currentQuery: String = ""

    val filteredChannels = MutableLiveData<List<FavouriteChannel>>()

    fun setChannels(channels: List<FavouriteChannel>) {
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
            val channelId = channel.id ?: return@launch
            val newIsFavourite = !channel.isFavourite

            // Update local list
            val indexOriginal = originalList.indexOfFirst { it.id == channelId }
            if (indexOriginal != -1) {
                val mutableOriginal = originalList.toMutableList()
                mutableOriginal[indexOriginal] =
                    mutableOriginal[indexOriginal].copy(isFavourite = newIsFavourite)
                originalList = mutableOriginal
            }

            playlistDao.updateFavourite(channelId, newIsFavourite, channel.playlistId)

            val favouriteChannel = FavouriteChannel(
                id = channelId,
                name = channel.name,
                logo = channel.logo,
                group = channel.group,
                url = channel.url,
                playlistId = channel.playlistId
            )

            if (newIsFavourite) {
                favouriteChannelDao.addFavouriteChannel(favouriteChannel)
            } else {
                favouriteChannelDao.removeFavouriteChannel(favouriteChannel)
            }

            applyFilter(_currentQuery)
        }
    }

    fun addRecentChannel(channel: Channel, playListId: Long){
        viewModelScope.launch {
            recentChannelDao.insertRecent(RecentChannel(
                channelId = channel.id?: UUID.randomUUID().toString(),
                channelIcon = channel.logo,
                lastWatchTime = System.currentTimeMillis(),
                url = channel.url,
                name = channel.name?:"",
                playlistId = playListId,
                group = channel.group
            ))
        }
    }
}