package com.iptvplayer.m3u.stream.ui.home.category_channel

import androidx.lifecycle.LifecycleOwner
import com.iptvplayer.m3u.stream.R
import com.iptvplayer.m3u.stream.model.dao.PlaylistDao
import com.iptvplayer.m3u.stream.model.entity.Channel
import com.iptvplayer.m3u.stream.utils.fetchM3U
import com.iptvplayer.m3u.stream.utils.parseM3U
import dagger.hilt.android.lifecycle.HiltViewModel
import hoang.dqm.codebase.base.viewmodel.BaseViewModel
import hoang.dqm.codebase.data.CategoryItemChannel
import hoang.dqm.codebase.data.Language
import hoang.dqm.codebase.firebase.AppRemoteConfig
import com.iptvplayer.m3u.stream.utils.CommonAppSharePref
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltViewModel
class CategoryChannelViewModel @Inject constructor(
    private val playlistDao: PlaylistDao
) : BaseViewModel() {
    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)
        getCategories()
    }

    private val _saveState =
        MutableStateFlow<CategoryRefreshSaveState>(CategoryRefreshSaveState.Idle)
    val saveState = _saveState.asStateFlow()

    private val appSharePref: CommonAppSharePref by lazy {
        CommonAppSharePref(context)
    }

    private val _listCategoryChannel = MutableStateFlow<List<CategoryItemChannel>>(emptyList())
    val listCategoryChannel = _listCategoryChannel.asStateFlow()

    fun getCategories() {
        launchHandler {
            flowOnIO {
                AppRemoteConfig.getListCategoryChannels(
                    appSharePref.languageCode ?: Language.ENGLISH.countryCode
                )
            }.subscribe {
                _listCategoryChannel.value = it
            }
        }
    }

    fun mergeChannels(existing: List<Channel>, incoming: List<Channel>): List<Channel> {
        val existingUrls = existing.map { it.url.trim() }.toHashSet()

        val existingTvgIds = existing
            .mapNotNull { ch ->
                if ((ch.id?.length ?: 0) > 36) ch.id?.dropLast(36) else null
            }
            .filter { it.isNotBlank() }
            .toHashSet()

        val newChannels = incoming.filter { incoming ->
            val incomingUrl = incoming.url.trim()
            val incomingTvgId =
                if ((incoming.id?.length ?: 0) > 36) incoming.id?.dropLast(36) else ""

            val urlExists = incomingUrl in existingUrls
            val tvgIdExists = incomingTvgId?.isNotBlank() == true && incomingTvgId in existingTvgIds

            !urlExists && !tvgIdExists
        }

        return existing + newChannels
    }

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build()
    }


    fun updatePlaylist(playlistId: Long) {
        launchHandler {
            _saveState.value = CategoryRefreshSaveState.Loading(10)

            try {
                // Lấy playlist hiện tại từ DB
                val existingPlaylist = withContext(Dispatchers.IO) {
                    playlistDao.getOnePlaylist(playlistId.toInt())
                }

                val url = existingPlaylist.playlist.url ?: run {
                    _saveState.value =
                        CategoryRefreshSaveState.Error(context.getString(R.string.text_playlist_has_no_url_to_update_from))
                    return@launchHandler
                }

                _saveState.value = CategoryRefreshSaveState.Loading(30)

                val m3uContent = withContext(Dispatchers.IO) {
                    fetchM3U(httpClient, url)
                }

                _saveState.value = CategoryRefreshSaveState.Loading(60)

                val incomingChannels = withContext(Dispatchers.Default) {
                    parseM3U(m3uContent)
                }

                if (incomingChannels.isEmpty()) {
                    _saveState.value = CategoryRefreshSaveState.Error(
                        context.getString(R.string.text_no_channel_were_founded_at_this_url)
                    )
                    return@launchHandler
                }

                _saveState.value = CategoryRefreshSaveState.Loading(80)

                val mergedChannels = mergeChannels(
                    existing = existingPlaylist.channels,
                    incoming = incomingChannels
                )

                val addedCount = mergedChannels.size - existingPlaylist.channels.size

                withContext(Dispatchers.IO) {
                    playlistDao.updateChannels(
                        playlistId = playlistId, channels = mergedChannels
                    )
                }

                _saveState.value = CategoryRefreshSaveState.Loading(100)
                _saveState.value = CategoryRefreshSaveState.Success(playlistId)

            } catch (e: IOException) {
                _saveState.value = CategoryRefreshSaveState.Error(
                    context.getString(R.string.text_connection_error, e.message)
                )
            } catch (e: Exception) {
                _saveState.value = CategoryRefreshSaveState.Error(e.message ?: "Error")
            }
        }
    }
}

sealed class CategoryRefreshSaveState {
    object Idle : CategoryRefreshSaveState()
    data class Loading(val progress: Int) : CategoryRefreshSaveState()
    data class Success(val playlistId: Long) : CategoryRefreshSaveState()

    data class Error(val message: String) : CategoryRefreshSaveState()
}