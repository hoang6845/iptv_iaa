package com.iptvplayer.m3u.stream.ui.profile

import androidx.lifecycle.viewModelScope
import com.iptvplayer.m3u.stream.model.dao.ServerDao
import dagger.hilt.android.lifecycle.HiltViewModel
import hoang.dqm.codebase.base.viewmodel.BaseViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val serverDao: ServerDao
): BaseViewModel() {
    fun changeAvatar(avatarUrl: String, serverId: Int){
        viewModelScope.launch {
            serverDao.updateAvatar(avatarUrl, serverId)
        }
    }
}