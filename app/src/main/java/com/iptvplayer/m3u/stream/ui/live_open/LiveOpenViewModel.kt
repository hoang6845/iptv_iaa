package com.iptvplayer.m3u.stream.ui.live_open

import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.filter
import com.iptvplayer.m3u.stream.api.LiveXtreamRepository
import com.iptvplayer.m3u.stream.api.LiveXtreamService
import com.iptvplayer.m3u.stream.model.dao.LiveXtreamDao
import com.iptvplayer.m3u.stream.model.dao.XtreamCategoryDao
import com.iptvplayer.m3u.stream.model.entity.LiveXtream
import com.iptvplayer.m3u.stream.model.entity.XtreamCategory
import dagger.hilt.android.lifecycle.HiltViewModel
import hoang.dqm.codebase.base.viewmodel.BaseViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Inject

// ---------- State ----------
sealed class LiveXtreamUpdateState {
    object Idle : LiveXtreamUpdateState()
    data class Loading(val progress: Int) : LiveXtreamUpdateState()
    object Success : LiveXtreamUpdateState()
    data class Error(val message: String) : LiveXtreamUpdateState()
}

@HiltViewModel
class LiveOpenViewModel @Inject constructor(
    private val liveXtreamDao: LiveXtreamDao,
    private val xtreamCategoryDao: XtreamCategoryDao
) : BaseViewModel() {

    private val searchQuery = MutableStateFlow("")
    val currentChannelId = MutableStateFlow<Int?>(null)
    private val categoryId = MutableStateFlow<Int?>(null)

    private val _categories = MutableStateFlow<List<XtreamCategory>>(emptyList())
    val categories: StateFlow<List<XtreamCategory>> = _categories

    private val _updateState = MutableStateFlow<LiveXtreamUpdateState>(LiveXtreamUpdateState.Idle)
    val updateState: StateFlow<LiveXtreamUpdateState> = _updateState.asStateFlow()

    var prefs: Triple<String?, String?, String?>? = null

    fun setPrefs(username: String?, password: String?, server: String?) {
        prefs = Triple(server, username, password)
    }

    fun getFullUrl(streamId: Int): String {
        val server = prefs?.first
        val username = prefs?.second
        val password = prefs?.third
        return if (server.isNullOrBlank() || username.isNullOrBlank() || password.isNullOrBlank()) ""
        else "$server/live/$username/$password/$streamId.m3u8"
    }

    fun loadCategories(serverId: Int = 1) {
        viewModelScope.launch {
            val list = xtreamCategoryDao.selectByServerIdLive(serverId)
            _categories.value = list
        }
    }

    fun selectCategory(position: Int) {
        val cat = _categories.value.getOrNull(position) ?: return
        categoryId.value = if (cat.categoryId == -1) null else cat.categoryId
    }

    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    val channels: Flow<PagingData<LiveXtream>> = combine(
        searchQuery,
        categoryId,
        currentChannelId
    ) { query, categoryId, excludeId ->
        Triple(query, categoryId, excludeId)
    }
        .debounce(300L)
        .flatMapLatest { (query, categoryId, excludeId) ->
            Pager(
                config = PagingConfig(
                    pageSize = 20,
                    prefetchDistance = 5,
                    enablePlaceholders = false
                )
            ) {
                liveXtreamDao.getPagedQuery(
                    server = 1,
                    query = query.trim(),
                    categoryId ?: 1
                )
            }.flow
                .map { pagingData ->
                    if (excludeId == null) pagingData
                    else pagingData.filter { it.streamId != excludeId }
                }
        }
        .cachedIn(viewModelScope)

    fun setLiveXtreamId(id: Int?) {
        currentChannelId.value = id
    }

    fun search(query: String) {
        searchQuery.value = query
    }

    // ---------- Update ----------

    fun updateLiveXtream(serverId: Int) {
        val server = prefs?.first
        val username = prefs?.second
        val password = prefs?.third

        if (server.isNullOrBlank() || username.isNullOrBlank() || password.isNullOrBlank()) {
            _updateState.value = LiveXtreamUpdateState.Error("Missing credentials")
            return
        }

        viewModelScope.launch {
            _updateState.value = LiveXtreamUpdateState.Loading(10)

            try {
                // Step 1: Load existing data from DB
                val existingList = withContext(Dispatchers.IO) {
                    liveXtreamDao.getAll(serverId)
                }

                _updateState.value = LiveXtreamUpdateState.Loading(30)

                // Step 2: Create repository dynamically (same pattern as reference)
                val repository = LiveXtreamRepository(createApiService(server))

                // Step 3: Fetch fresh data from API
                val result = withContext(Dispatchers.IO) {
                    repository.getLiveXtream(username = username, password = password)
                }

                _updateState.value = LiveXtreamUpdateState.Loading(60)

                result
                    .onSuccess { incomingList ->
                        if (incomingList.isEmpty()) {
                            _updateState.value =
                                LiveXtreamUpdateState.Error("No channels found from server")
                            return@launch
                        }

                        _updateState.value = LiveXtreamUpdateState.Loading(80)

                        // Step 4: Merge existing with incoming
                        val mergedList = mergeLiveXtream(
                            existing = existingList,
                            incoming = incomingList,
                            serverId = serverId
                        )

                        // Step 5: Save merged list (REPLACE strategy handles upsert)
                        withContext(Dispatchers.IO) {
                            liveXtreamDao.insertAll(mergedList)
                        }

                        _updateState.value = LiveXtreamUpdateState.Loading(100)
                        _updateState.value = LiveXtreamUpdateState.Success
                    }
                    .onFailure { e ->
                        _updateState.value =
                            LiveXtreamUpdateState.Error(e.message ?: "Failed to fetch channels")
                    }

            } catch (e: Exception) {
                _updateState.value = LiveXtreamUpdateState.Error(e.message ?: "Unknown error")
            }
        }
    }

    private fun mergeLiveXtream(
        existing: List<LiveXtream>,
        incoming: List<LiveXtream>,
        serverId: Int
    ): List<LiveXtream> {
        // Build lookup map by uniqueId for O(1) access
        val existingMap = existing.associateBy { it.uniqueId }

        return incoming.map { newItem ->
            val prepared = newItem.copy(
                uniqueId = newItem.generateUniqueId(),
                server = serverId
            )
            val old = existingMap[prepared.uniqueId]

            if (old != null) {
                // Channel exists → keep local-only fields, update everything else
                prepared.copy(
                    // e.g. isFavorite = old.isFavorite  ← uncomment if you have such fields
                )
            } else {
                // Brand new channel from API
                prepared
            }
        }
    }

    fun resetUpdateState() {
        _updateState.value = LiveXtreamUpdateState.Idle
    }

    private fun createApiService(baseUrl: String): LiveXtreamService {
        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        return retrofit.create(LiveXtreamService::class.java)
    }
}