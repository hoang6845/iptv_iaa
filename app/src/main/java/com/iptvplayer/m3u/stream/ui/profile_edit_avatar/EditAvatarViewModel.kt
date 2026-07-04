package com.iptvplayer.m3u.stream.ui.profile_edit_avatar

import androidx.lifecycle.LifecycleOwner
import com.iptvplayer.m3u.stream.model.entity.Avatar
import hoang.dqm.codebase.base.viewmodel.BaseViewModel
import hoang.dqm.codebase.firebase.AppRemoteConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class EditAvatarViewModel: BaseViewModel() {
    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)
        getAvatars()
    }

    private val _avatars = MutableStateFlow<List<Avatar>>(emptyList())
    val avatars = _avatars.asStateFlow()

    fun getAvatars() {
        launchHandler {
            flowOnIO {
                AppRemoteConfig.getListData(
                    AppRemoteConfig.DATA_AVATAR, Avatar::class.java
                )
            }.subscribe {
                _avatars.value = it
            }
        }
    }

}