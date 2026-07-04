package com.iptvplayer.m3u.stream.ui.add_playlist.upload_file

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.iptvplayer.m3u.stream.model.dao.PlaylistDao
import com.iptvplayer.m3u.stream.model.entity.Channel
import com.iptvplayer.m3u.stream.model.entity.PlaylistEntity
import com.iptvplayer.m3u.stream.utils.AppConstants
import com.iptvplayer.m3u.stream.utils.parseM3U
import dagger.hilt.android.lifecycle.HiltViewModel
import hoang.dqm.codebase.base.viewmodel.BaseViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class UploadFileViewModel @Inject constructor(
    private val playlistDao: PlaylistDao
): BaseViewModel() {
    private val _selectedFiles = MutableLiveData<List<Uri>>(emptyList())
    val selectedFiles: LiveData<List<Uri>> = _selectedFiles

    private val _saveState = MutableLiveData<SaveState>(SaveState.Idle)
    val saveState: LiveData<SaveState> = _saveState

    fun onFilesSelected(uris: List<Uri>) {
        _selectedFiles.value = uris
    }

    fun savePlaylist(name: String, isPassCode: Boolean) {
        val files = _selectedFiles.value
        if (files.isNullOrEmpty()) {
            _saveState.value = SaveState.Error("Chưa có file nào được chọn")
            return
        }

        viewModelScope.launch {
            _saveState.value = SaveState.Loading(0)

            try {
                val allChannels = mutableListOf<Channel>()
                val total = files.size

                files.forEachIndexed { index, uri ->
                    val content = withContext(Dispatchers.IO) {
                        context.contentResolver.openInputStream(uri)
                            ?.bufferedReader()
                            ?.use { it.readText() }
                            ?: throw Exception("Không thể đọc file: $uri")
                    }

                    val channels = parseM3U(content)
                    allChannels.addAll(channels)

                    // Cập nhật % sau mỗi file parse xong
                    val progress = ((index + 1) * 80 / total) // 0–80% cho parse
                    _saveState.value = SaveState.Loading(progress)
                }

                if (allChannels.isEmpty()) {
                    _saveState.value = SaveState.Error("Không tìm thấy kênh nào trong file")
                    return@launch
                }

                // 80–100%: lưu DB
                _saveState.value = SaveState.Loading(90)
                withContext(Dispatchers.IO) {
                    val playlist = PlaylistEntity(
                        name = name,
                        typePlayList = AppConstants.TYPE_PLAYLIST_FILE,
                        url = null,
                        isPasscodeEnabled = isPassCode
                    )
                    val channelEntities = allChannels.map { ch ->
                        Channel(
                            playlistId = 0, name = ch.name, url = ch.url,
                            id = ch.id,
                            logo = ch.logo,
                            group = ch.group
                        )
                    }
                    playlistDao.insertPlaylistWithChannels(playlist, channelEntities)
                }

                _saveState.value = SaveState.Loading(100)
                _saveState.value = SaveState.Success
            } catch (e: Exception) {
                _saveState.value = SaveState.Error(e.message ?: "Đã xảy ra lỗi")
            }
        }
    }
}

sealed class SaveState {
    object Idle : SaveState()
    data class Loading(val progress: Int) : SaveState()
    object Success : SaveState()
    data class Error(val message: String) : SaveState()
}