package com.iptvplayer.m3u.stream.ui.iptv_live

import com.iptvplayer.m3u.stream.model.dao.ChannelPopularDao
import com.iptvplayer.m3u.stream.model.entity.ChannelPopular
import dagger.hilt.android.lifecycle.HiltViewModel
import hoang.dqm.codebase.base.viewmodel.BaseViewModel
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

@HiltViewModel
class LiveViewModel @Inject constructor(
    private val channelPopularDao: ChannelPopularDao
): BaseViewModel() {
    val channels: Flow<List<ChannelPopular>> = channelPopularDao.getChannels()
}