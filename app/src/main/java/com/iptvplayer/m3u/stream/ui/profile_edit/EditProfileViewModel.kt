package com.iptvplayer.m3u.stream.ui.profile_edit

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.iptvplayer.m3u.stream.model.dao.ServerDao
import com.iptvplayer.m3u.stream.model.entity.XtreamAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import hoang.dqm.codebase.base.viewmodel.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EditProfileViewModel @Inject constructor(
    private val serverDao: ServerDao
): BaseViewModel() {
    private val _serverState = MutableStateFlow<XtreamAuth?>(null)
    val serverState: StateFlow<XtreamAuth?> = _serverState.asStateFlow()

    private val _saveResult = MutableStateFlow<SaveResult>(SaveResult.Idle)
    val saveResult: StateFlow<SaveResult> = _saveResult.asStateFlow()

    private val _deleteResult = MutableStateFlow<DeleteResult>(DeleteResult.Idle)
    val deleteResult: StateFlow<DeleteResult> = _deleteResult.asStateFlow()

    fun loadServer(serverId: Int) {
        viewModelScope.launch {
            val server = serverDao.getServerById(serverId)
            _serverState.value = server
        }
    }

    fun saveProfile(
        serverId: Int,
        name: String,
        username: String,
        password: String,
        urlServer: String,
        isEnablePasscode: Boolean,
        avatar: String
    ) {
        viewModelScope.launch {
            val current = _serverState.value ?: return@launch

            if (name.isBlank()) {
                _saveResult.value = SaveResult.Error("Profile name cannot be empty")
                return@launch
            }
            if (username.isBlank()) {
                _saveResult.value = SaveResult.Error("Username cannot be empty")
                return@launch
            }
            if (urlServer.isBlank()) {
                _saveResult.value = SaveResult.Error("Server URL cannot be empty")
                return@launch
            }

            val updated = current.copy(
                name = name.trim(),
                username = username.trim(),
                password = password.trim(),
                server = urlServer.trim(),
                isEnablePasscode = isEnablePasscode,
                urlAvatar = avatar
            )

            serverDao.updateProfile(
                id = serverId,
                name = updated.name ?: "",
                username = updated.username,
                password = updated.password,
                server = updated.server,
                isEnablePasscode = updated.isEnablePasscode,
                avatar = updated.urlAvatar
            )
            Log.d("check edit", "initData: $updated")

            _serverState.value = updated
            _saveResult.value = SaveResult.Success
        }
    }

    fun changeAvatar(avatarUrl: String, serverId: Int) {
        viewModelScope.launch {
            serverDao.updateAvatar(avatarUrl, serverId)
            _serverState.value = _serverState.value?.copy(urlAvatar = avatarUrl)
        }
    }

    fun deleteProfile(serverId: Int) {
        viewModelScope.launch {
            serverDao.deleteById(serverId)
            _deleteResult.value = DeleteResult.Success
        }
    }

    fun resetSaveResult() {
        _saveResult.value = SaveResult.Idle
    }

    fun resetDeleteResult() {
        _deleteResult.value = DeleteResult.Idle
    }

    sealed class SaveResult {
        object Idle : SaveResult()
        object Success : SaveResult()
        data class Error(val message: String) : SaveResult()
    }

    sealed class DeleteResult {
        object Idle : DeleteResult()
        object Success : DeleteResult()
    }
}