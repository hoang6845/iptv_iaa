//package com.iptvplayer.m3u.stream.ui.channels
//
//import androidx.lifecycle.LifecycleOwner
//import androidx.lifecycle.viewModelScope
//import com.iptvplayer.m3u.stream.model.dao.ChannelPopularDao
//import com.iptvplayer.m3u.stream.model.entity.Channel
//import com.iptvplayer.m3u.stream.model.entity.ChannelPopular
//import dagger.hilt.android.lifecycle.HiltViewModel
//import hoang.dqm.codebase.base.viewmodel.BaseViewModel
//import hoang.dqm.codebase.data.CategoryItem
//import hoang.dqm.codebase.data.Language
//import hoang.dqm.codebase.firebase.AppRemoteConfig
//import hoang.dqm.codebase.utils.CommonAppSharePref
//import kotlinx.coroutines.flow.MutableStateFlow
//import kotlinx.coroutines.flow.SharingStarted
//import kotlinx.coroutines.flow.StateFlow
//import kotlinx.coroutines.flow.asStateFlow
//import kotlinx.coroutines.flow.combine
//import kotlinx.coroutines.flow.stateIn
//import kotlinx.coroutines.launch
//import javax.inject.Inject
//
//@HiltViewModel
//class ChannelsViewModel @Inject constructor(
//    private val channelPopularDao: ChannelPopularDao
//): BaseViewModel() {
//    override fun onCreate(owner: LifecycleOwner) {
//        super.onCreate(owner)
//        getCategories()
//    }
//
//    private val appSharePref: CommonAppSharePref by lazy {
//        CommonAppSharePref(context)
//    }
//
//    private val _listCategoryChannel = MutableStateFlow<List<CategoryItem>>(emptyList())
//    val listCategoryChannel = _listCategoryChannel.asStateFlow()
//
//    private val searchQuery = MutableStateFlow("")
//    private val selectedCategory = MutableStateFlow<CategoryItem?>(null)
//
//    private val _allChannels: StateFlow<List<ChannelPopular>> = channelPopularDao.getChannels()
//        .stateIn(
//            scope = viewModelScope,
//            started = SharingStarted.WhileSubscribed(5000),
//            initialValue = emptyList()
//        )
//
//    val channels: StateFlow<List<ChannelPopular>> = combine(
//        _allChannels,
//        searchQuery,
//        selectedCategory
//    ) { channels, query, category ->
//        channels
//            .filter { channel ->
//                query.isBlank() || channel.name.contains(query, ignoreCase = true)
//            }
//            .filter { channel ->
//                category == null ||
//                        category.type.equals("all", ignoreCase = true) ||
//                        channel.group.equals(category.type, ignoreCase = true)
//            }
//    }.stateIn(
//        scope = viewModelScope,
//        started = SharingStarted.WhileSubscribed(5000),
//        initialValue = emptyList()
//    )
//
//    fun getCategories() {
//        launchHandler {
//            flowOnIO {
//                AppRemoteConfig.getListCategoryMovie(
//                    appSharePref.languageCode ?: Language.ENGLISH.countryCode
//                )
//            }.subscribe {
//                _listCategoryChannel.value = it
//            }
//        }
//    }
//
//    fun search(query: String) {
//        searchQuery.value = query
//    }
//
//    fun selectCategory(category: CategoryItem?) {
//        selectedCategory.value = category
//    }
//
//    fun toggleFavourite(channel: Channel) {
//        viewModelScope.launch {
//            val newIsFavourite = !channel.isFavourite
//            channel.id?.let {
//                channelPopularDao.updateFavourite(it, newIsFavourite)
//            }
//        }
//    }
//    fun isFavourite(channel: Channel?): Boolean = channel?.isFavourite ?: false
//}

package com.iptvplayer.m3u.stream.ui.channels

import android.util.Log
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.filter
import com.iptvplayer.m3u.stream.model.dao.ChannelPopularDao
import com.iptvplayer.m3u.stream.model.dao.FavouriteChannelDao
import com.iptvplayer.m3u.stream.model.dao.PlaylistDao
import com.iptvplayer.m3u.stream.model.entity.Channel
import com.iptvplayer.m3u.stream.model.entity.ChannelPopular
import com.iptvplayer.m3u.stream.model.entity.FavouriteChannel
import com.iptvplayer.m3u.stream.utils.CommonAppSharePref
import dagger.hilt.android.lifecycle.HiltViewModel
import hoang.dqm.codebase.base.viewmodel.BaseViewModel
import hoang.dqm.codebase.data.CategoryItem
import hoang.dqm.codebase.data.Language
import hoang.dqm.codebase.firebase.AppRemoteConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.collections.filter
import kotlin.collections.map
import kotlin.collections.toSet

@HiltViewModel
class ChannelsViewModel @Inject constructor(
    private val channelPopularDao: ChannelPopularDao,
    private val favouriteChannelDao: FavouriteChannelDao,
    private val playlistDao: PlaylistDao
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

    private val searchQuery = MutableStateFlow("")
    private val _selectedCategory = MutableStateFlow<CategoryItem?>(null)
    val selectedCategory = _selectedCategory.asStateFlow()
    val currentChannelId = MutableStateFlow<String?>(null)

    data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)


    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    val channels: Flow<PagingData<Channel>> = combine(
        searchQuery,
        selectedCategory,
        currentChannelId,
    ) { query, category, excludeId ->
        Triple(query, category, excludeId,)
    }
        .debounce(300L)
        .flatMapLatest { (query, category, excludeId) ->
            Pager(
                config = PagingConfig(
                    pageSize = 50,
                    prefetchDistance = 5,
                    enablePlaceholders = false
                )
            ) {
                Log.d("check all cate", "initData: ${category?.type?.trim() ?: ""}")

                channelPopularDao.getChannelsPagedQueryOnly(
                    query = query.trim(),
                    group = category?.type?.trim() ?: ""
                )
            }.flow
                .map { pagingData ->
                    if (excludeId == null) pagingData
                    else pagingData.filter { it.id != excludeId }
                }


        }
        .cachedIn(viewModelScope)

    fun setCurrentChannel(id: String?) {
        currentChannelId.value = id
    }

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

    fun search(query: String) {
        searchQuery.value = query
    }

    fun selectCategory(category: CategoryItem?) {
        _selectedCategory.value = category
    }

    fun toggleFavourite(channel: Channel) {
        viewModelScope.launch {
            val newIsFavourite = !channel.isFavourite
            val channelId = channel.id ?: return@launch
            Log.d("toggleFavourite", "toggleFavourite: $channelId ${channel.playlistId} $newIsFavourite")
            playlistDao.updateFavourite(
                channelId = channelId,
                isFavourite = newIsFavourite,
                playlistId = channel.playlistId
            )

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
        }
    }

    fun isFavourite(channel: Channel?): Boolean = channel?.isFavourite ?: false


}