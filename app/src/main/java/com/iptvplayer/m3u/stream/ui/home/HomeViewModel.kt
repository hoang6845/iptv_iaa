package com.iptvplayer.m3u.stream.ui.home

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.iptvplayer.m3u.stream.model.dao.ChannelPopularDao
import com.iptvplayer.m3u.stream.model.dao.PlaylistDao
import com.iptvplayer.m3u.stream.model.dao.RecentChannelDao
import com.iptvplayer.m3u.stream.model.entity.PlaylistFavourite
import com.iptvplayer.m3u.stream.model.entity.PlaylistWithChannels
import com.iptvplayer.m3u.stream.model.entity.RecentChannel
import dagger.hilt.android.lifecycle.HiltViewModel
import hoang.dqm.codebase.base.viewmodel.BaseViewModel
import hoang.dqm.codebase.data.CategoryItem
import hoang.dqm.codebase.data.Language
import hoang.dqm.codebase.firebase.AppRemoteConfig
import com.iptvplayer.m3u.stream.utils.CommonAppSharePref
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val playlistDao: PlaylistDao,
    private val channelPopularDao: ChannelPopularDao,
    private val recentChannelDao: RecentChannelDao
): BaseViewModel() {
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

    val playlists: StateFlow<List<PlaylistWithChannels>> = playlistDao.getItems()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(3000),
            initialValue = emptyList()
        )

    val recents: StateFlow<List<RecentChannel>> = recentChannelDao.getRecentChannels()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(3000),
            initialValue = emptyList()
        )

    private val playListSelected = MutableLiveData<PlaylistWithChannels>()
    val playlistSelected: MutableLiveData<PlaylistWithChannels> = playListSelected

    fun setPlaylistSelected(playlist: PlaylistWithChannels) {
        playListSelected.value = playlist
    }

    private val _favouriteList = MutableLiveData<PlaylistFavourite>()
    val favouriteList: LiveData<PlaylistFavourite> get() = _favouriteList

    fun setFavouriteList(value: PlaylistFavourite){
        _favouriteList.value = value
    }
}