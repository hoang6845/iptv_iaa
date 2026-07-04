package com.iptvplayer.m3u.stream.ui.home.list_popular

import androidx.lifecycle.viewModelScope
import com.iptvplayer.m3u.stream.model.dao.ChannelPopularDao
import com.iptvplayer.m3u.stream.model.entity.Channel
import com.iptvplayer.m3u.stream.model.entity.ChannelPopular
import dagger.hilt.android.lifecycle.HiltViewModel
import hoang.dqm.codebase.base.viewmodel.BaseViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
@HiltViewModel
class PopularChannelViewModel @Inject constructor(
    private val channelPopularDao: ChannelPopularDao
): BaseViewModel() {

    private val typeFlow = MutableStateFlow<String?>(null)
    private val searchQuery = MutableStateFlow("")

    @OptIn(ExperimentalCoroutinesApi::class)
    val channels: StateFlow<List<ChannelPopular>> =
        typeFlow.flatMapLatest { type ->
            if (type.isNullOrEmpty() || type.lowercase() == "all") {
                channelPopularDao.getChannels()
            } else {
                channelPopularDao.getChannelsByType(type)
            }
        }
            .combine(searchQuery) { list, query ->
                val trimmed = query.trim()
                if (trimmed.isEmpty()) {
                    list
                } else {
                    list.filter {
                        it.name.contains(trimmed, ignoreCase = true) ||
                                it.group?.contains(trimmed, ignoreCase = true) == true
                    }
                }
            }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                emptyList()
            )

    fun toggleFavourite(channel: Channel) {
        viewModelScope.launch {
            val newIsFavourite = !channel.isFavourite
            channel.id?.let {
                channelPopularDao.updateFavourite(it, newIsFavourite)
            }
        }
    }

    fun setType(type: String?) {
        typeFlow.value = type
    }

    fun search(query: String) {
        searchQuery.value = query
    }
}
