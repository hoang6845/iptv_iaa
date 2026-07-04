package com.iptvplayer.m3u.stream.ui.favourite_open

import hoang.dqm.codebase.base.viewmodel.BaseViewModel


import androidx.lifecycle.viewModelScope
import com.iptvplayer.m3u.stream.model.dao.FavouriteChannelDao
import com.iptvplayer.m3u.stream.model.dao.PlaylistDao
import com.iptvplayer.m3u.stream.model.entity.FavouriteChannel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FavouriteOpenViewModel @Inject constructor(
    private val favouriteDao: FavouriteChannelDao,
    private val playlistDao: PlaylistDao
) : BaseViewModel() {

    private val searchQuery = MutableStateFlow("")
    val currentChannelId = MutableStateFlow<String?>(null)

    // ── Thêm: lưu toàn bộ channel đang phát ──────────────────────────────
    private val _currentChannel = MutableStateFlow<FavouriteChannel?>(null)

    private val _displayList = MutableStateFlow<List<FavouriteChannel>>(emptyList())
    private val _favouriteSet = MutableStateFlow<Set<String>>(emptySet())
    val favouriteStates: StateFlow<Set<String>> = _favouriteSet

    // ── Thêm: state tổng hợp "channel hiện tại có đang được yêu thích?" ──
    val currentChannelFavState: StateFlow<Boolean> = combine(
        _favouriteSet, _currentChannel
    ) { favSet, channel ->
        channel != null && favSet.contains("${channel.id}_${channel.playlistId}")
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    // ── Sửa: nhận FavouriteChannel thay vì chỉ String id ─────────────────
    fun setCurrentChannel(channel: FavouriteChannel?) {
        _currentChannel.value = channel
        currentChannelId.value = channel?.id
    }

    val favouriteChannels: StateFlow<List<FavouriteChannel>> = combine(
        _displayList, searchQuery, currentChannelId
    ) { list, query, excludeId ->
        list.filter {
            (query.isBlank() || it.name.contains(query, ignoreCase = true)) && it.id != excludeId
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun loadSnapshot() {
        viewModelScope.launch {
            val list = favouriteDao.getAllFavouriteChannels().first()
            _displayList.value = list
            _favouriteSet.value = list.map { "${it.id}_${it.playlistId}" }.toSet()
        }
    }

    fun search(query: String) { searchQuery.value = query }

    fun toggleFavourite(item: FavouriteChannel) {
        viewModelScope.launch {
            val key = "${item.id}_${item.playlistId}"
            val newFav = !_favouriteSet.value.contains(key)

            playlistDao.updateFavourite(item.id, newFav, item.playlistId)
            if (newFav) favouriteDao.addFavouriteChannel(item.copy(isFavourite = true))
            else favouriteDao.removeFavouriteChannel(item)

            _favouriteSet.value = if (newFav) _favouriteSet.value + key
            else _favouriteSet.value - key
        }
    }

    fun isFavourite(item: FavouriteChannel): Boolean =
        _favouriteSet.value.contains("${item.id}_${item.playlistId}")
}