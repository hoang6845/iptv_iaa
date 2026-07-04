package com.iptvplayer.m3u.stream.ui.recent_open

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.viewModelScope
import com.iptvplayer.m3u.stream.model.dao.FavouriteChannelDao
import com.iptvplayer.m3u.stream.model.dao.FavouriteDao
import com.iptvplayer.m3u.stream.model.dao.PlaylistDao
import com.iptvplayer.m3u.stream.model.dao.RecentChannelDao
import com.iptvplayer.m3u.stream.model.entity.FavouriteChannel
import com.iptvplayer.m3u.stream.model.entity.RecentChannel
import com.iptvplayer.m3u.stream.utils.CommonAppSharePref
import dagger.hilt.android.lifecycle.HiltViewModel
import hoang.dqm.codebase.base.viewmodel.BaseViewModel
import hoang.dqm.codebase.data.CategoryItem
import hoang.dqm.codebase.data.Language
import hoang.dqm.codebase.firebase.AppRemoteConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RecentOpenViewModel @Inject constructor(
    private val recentChannelDao: RecentChannelDao,
    private val favouriteDao: FavouriteChannelDao,
    private val playlistDao: PlaylistDao
) : BaseViewModel() {
    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)
        getCategories()
    }

    private val appSharePref: CommonAppSharePref by lazy {
        CommonAppSharePref(context)
    }

    private val _listCategoryChannel = MutableStateFlow<List<CategoryItem>>(emptyList())
    val listCategoryChannel = _listCategoryChannel.asStateFlow()

    fun getCategories() {
        launchHandler {
            flowOnIO {
                AppRemoteConfig.getListCategoryMovie(
                    appSharePref.languageCode ?: Language.ENGLISH.countryCode
                )
            }.subscribe {
                _listCategoryChannel.value = it
            }
        }
    }
    private val searchQuery = MutableStateFlow("")

    private val _allRecent: StateFlow<List<RecentChannel>> = recentChannelDao.getRecentChannels()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val currentChannelId = MutableStateFlow<String?>(null)
    fun setCurrentChannel(id: String?) {
        currentChannelId.value = id
    }
    private val _selectedCategory = MutableStateFlow<CategoryItem?>(null)
    val selectedCategory = _selectedCategory.asStateFlow()
    val recentChannels: StateFlow<List<RecentChannel>> = combine(
        _allRecent, searchQuery,
        currentChannelId,
        selectedCategory

    ) { channels, query, excludeId, selectedCategory ->
        channels.filter { ((query.isBlank() || it.name.contains(query, ignoreCase = true) )&& (it.group?.contains(selectedCategory?.type ?: "", ignoreCase = true) == true) || selectedCategory?.type == "all") && it.channelId != excludeId }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun selectCategory(category: CategoryItem?) {
        _selectedCategory.value = category
    }

    val favouriteChannels: StateFlow<List<FavouriteChannel>> =
        favouriteDao.getAllFavouriteChannels()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun search(query: String) {
        searchQuery.value = query
    }

    fun addRecent(channel: RecentChannel) {
        viewModelScope.launch { recentChannelDao.insertRecent(channel) }
    }

    fun removeRecent(channelId: String) {
        viewModelScope.launch { recentChannelDao.deleteRecent(channelId) }
    }

    fun clearAll() {
        viewModelScope.launch { recentChannelDao.clearRecent() }
    }

    fun toggleFavourite(item: RecentChannel) {
        viewModelScope.launch {
            val isCurrentlyFav = favouriteDao.isFavourite(item.channelId, item.playlistId)
            val newFav = !isCurrentlyFav

            playlistDao.updateFavourite(item.channelId, newFav, item.playlistId)

            val favouriteChannel = FavouriteChannel(
                id = item.channelId,
                name = item.name,
                logo = item.channelIcon,
                group = null,
                url = item.url,
                playlistId = item.playlistId,
                isFavourite = true
            )

            if (newFav) {
                favouriteDao.addFavouriteChannel(favouriteChannel)
            } else {
                favouriteDao.removeFavouriteChannel(favouriteChannel)
            }
        }
    }

    fun removeRecent(item: RecentChannel) {
        viewModelScope.launch {
            recentChannelDao.deleteRecent(item.channelId)
        }
    }
}