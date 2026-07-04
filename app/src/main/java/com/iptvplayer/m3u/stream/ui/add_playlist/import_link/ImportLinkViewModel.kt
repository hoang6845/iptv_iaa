package com.iptvplayer.m3u.stream.ui.add_playlist.import_link

import android.util.Log
import androidx.lifecycle.LifecycleOwner
import com.iptvplayer.m3u.stream.R
import com.iptvplayer.m3u.stream.model.dao.PlaylistDao
import com.iptvplayer.m3u.stream.model.entity.Channel
import com.iptvplayer.m3u.stream.model.entity.PlaylistEntity
import com.iptvplayer.m3u.stream.utils.AppConstants
import com.iptvplayer.m3u.stream.utils.parseM3U
import dagger.hilt.android.lifecycle.HiltViewModel
import hoang.dqm.codebase.base.viewmodel.BaseViewModel
import hoang.dqm.codebase.data.CategoryItemUrl
import hoang.dqm.codebase.data.Language
import hoang.dqm.codebase.firebase.AppRemoteConfig
import com.iptvplayer.m3u.stream.utils.CommonAppSharePref
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.Locale
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltViewModel
class ImportLinkViewModel @Inject constructor(
    private val playlistDao: PlaylistDao
) : BaseViewModel() {

    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)
        getCategories()
        Log.d("check category", "initData: data_category_url")

    }

    private val appSharePref: CommonAppSharePref by lazy {
        CommonAppSharePref(context)
    }

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    private val _listCategoryChannel = MutableStateFlow<List<CategoryItemUrl>>(emptyList())
    val listCategoryChannel = _listCategoryChannel.asStateFlow()

    private val _saveState = MutableStateFlow<ImportSaveState>(ImportSaveState.Idle)
    val saveState = _saveState.asStateFlow()

    fun getCategories() {
        launchHandler {
            flowOnIO {
                AppRemoteConfig.getListCategoryUrl(
                    appSharePref.languageCode ?: Locale.getDefault().language
                )
            }.subscribe {
                _listCategoryChannel.value = it
            }
        }
    }

    fun savePlaylist(name: String, url: String, isPassCode: Boolean) {
        launchHandler {
            _saveState.value = ImportSaveState.Loading(10)

            try {
                val alreadyExists = withContext(Dispatchers.IO){
                    playlistDao.isAlreadyExists(url)
                }
                if (alreadyExists) {
                    _saveState.value = ImportSaveState.Error(context.getString(R.string.text_playlist_already_exists))
                    return@launchHandler
                }
                val m3uContent = withContext(Dispatchers.IO) {
                    fetchM3U(httpClient, url)
                }

                _saveState.value = ImportSaveState.Loading(50)

                val channels = withContext(Dispatchers.Default) {
                    parseM3U(m3uContent)
                }

                if (channels.isEmpty()) {
                    _saveState.value = ImportSaveState.Error(context.getString(R.string.text_no_channels_were_found_at_this_url))
                    return@launchHandler
                }

                _saveState.value = ImportSaveState.Loading(80)

                withContext(Dispatchers.IO) {
                    val playlist = PlaylistEntity(
                        name = name,
                        typePlayList = AppConstants.TYPE_PLAYLIST_URL,
                        url = url,
                        isPasscodeEnabled = isPassCode
                    )
                    val channelEntities = channels.map { ch ->
                        Channel(
                            playlistId = 0, name = ch.name, url = ch.url,
                            id = ch.id,
                            logo = ch.logo,
                            group = ch.group
                        )
                    }
                    playlistDao.insertPlaylistWithChannels(playlist, channelEntities)

                }

                _saveState.value = ImportSaveState.Loading(100)
                _saveState.value = ImportSaveState.Success

            } catch (e: IOException) {
                _saveState.value = ImportSaveState.Error(
                    context.getString(
                        R.string.text_playlist_not_found
                    ))
            } catch (e: Exception) {
                _saveState.value = ImportSaveState.Error(
                    context.getString(
                        R.string.text_playlist_not_found
                    ))
            }
        }
    }

    private suspend fun fetchM3U(client: OkHttpClient, url: String): String =
        withContext(Dispatchers.IO) {
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
                response.body?.string().orEmpty()
            }
        }
}

sealed class ImportSaveState {
    object Idle : ImportSaveState()
    data class Loading(val progress: Int) : ImportSaveState()
    object Success : ImportSaveState()
    data class Error(val message: String) : ImportSaveState()
}