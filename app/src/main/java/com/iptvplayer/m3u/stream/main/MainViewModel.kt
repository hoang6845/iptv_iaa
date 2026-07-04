package com.iptvplayer.m3u.stream.main

import android.util.Log
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.viewModelScope
import com.iptvplayer.m3u.stream.model.dao.ChannelPopularDao
import com.iptvplayer.m3u.stream.model.dao.PlaylistDao
import com.iptvplayer.m3u.stream.model.entity.ChannelPopular
import com.iptvplayer.m3u.stream.utils.AppConstants
import com.iptvplayer.m3u.stream.utils.HttpClientProvider
import com.iptvplayer.m3u.stream.utils.fetchM3U
import com.iptvplayer.m3u.stream.utils.parseM3UChannelPopular
import dagger.hilt.android.lifecycle.HiltViewModel
import hoang.dqm.codebase.data.CategoryItem
import hoang.dqm.codebase.service.session.isFirst
import hoang.dqm.codebase.service.session.saveFirst
import hoang.dqm.codebase.ui.vm.BaseMainViewModel
import com.iptvplayer.m3u.stream.utils.CommonAppSharePref
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val playListDao: PlaylistDao,
    private val channelPopularDao: ChannelPopularDao
) : BaseMainViewModel() {
    private val _previousIndex = MutableStateFlow<Int>(0)
    val previousIndex: StateFlow<Int> = _previousIndex
    fun setPreviousIndex(value: Int){
        _previousIndex.value = value
    }


    private val okHttpClient by lazy {
        HttpClientProvider.provide(context)
    }
    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)
//        if (isFirst()){
//            loadM3U(AppConstants.CATEGORY_URL)
//        }
    }

    private val appSharePref: CommonAppSharePref by lazy {
        CommonAppSharePref(context)
    }
    private val _listLanguagesChannel = MutableStateFlow<List<CategoryItem>>(emptyList())
    val listLanguageChannel = _listLanguagesChannel.asStateFlow()
    fun getLanguages() {
    }

    private val _channelsWithCategory = MutableStateFlow<List<ChannelPopular>>(emptyList())
    val channelsWithCategory: StateFlow<List<ChannelPopular>> = _channelsWithCategory.asStateFlow()

    private val _channelsWithCountry = MutableStateFlow<List<ChannelPopular>>(emptyList())
    val channelsWithCountry: StateFlow<List<ChannelPopular>> = _channelsWithCountry.asStateFlow()
    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun loadM3U(url: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _loading.value = true
            _error.value = null

            try {
                val m3uText = fetchM3U(okHttpClient, url)
                val result = parseM3UChannelPopular(m3uText)
                channelPopularDao.insertChannels(result)
                if (url == AppConstants.CATEGORY_URL){
                    _channelsWithCategory.value = result
                } else if (url == AppConstants.COUNTRY_URL){
                    _channelsWithCountry.value = result
                }
                Log.d("check data", "loadM3U: ${result.size}")
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _loading.value = false
            }
        }
    }
}