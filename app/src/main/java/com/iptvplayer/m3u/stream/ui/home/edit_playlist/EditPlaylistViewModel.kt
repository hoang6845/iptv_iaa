package com.iptvplayer.m3u.stream.ui.home.edit_playlist

import androidx.lifecycle.viewModelScope
import com.iptvplayer.m3u.stream.model.dao.PlaylistDao
import dagger.hilt.android.lifecycle.HiltViewModel
import hoang.dqm.codebase.base.viewmodel.BaseViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EditPlaylistViewModel @Inject constructor(
    private val playlistDao: PlaylistDao
): BaseViewModel(){

    fun togglePasscode(id: Long, isPasscodeEnabled: Boolean){
        viewModelScope.launch {
            playlistDao.togglePasscode(id, isPasscodeEnabled)
        }

    }
}