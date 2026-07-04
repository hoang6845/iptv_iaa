package com.iptvplayer.m3u.stream.ui.livextream

import android.util.Log
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.iptvplayer.m3u.stream.api.LiveXtreamRepository
import com.iptvplayer.m3u.stream.model.dao.LiveXtreamDao
import com.iptvplayer.m3u.stream.model.dao.SearchDao
import com.iptvplayer.m3u.stream.model.dao.XtreamCategoryDao
import com.iptvplayer.m3u.stream.model.entity.LiveXtream
import com.iptvplayer.m3u.stream.model.entity.SearchFtsEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import hoang.dqm.codebase.base.viewmodel.BaseViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LiveXtreamViewModel @Inject constructor(
    private val liveXtreamDao: LiveXtreamDao,
    private val searchDao: SearchDao,
    private val categoryXtreamDao: XtreamCategoryDao
) : BaseViewModel() {
    fun getLiveXtream(
        repository: LiveXtreamRepository,
        username: String,
        password: String,
        serverId: Int
    ) {
        viewModelScope.launch {
            repository.getLiveXtream(username = username, password = password)
                .onSuccess { listLive ->
                    val listSave = listLive.map { live ->
                        live.copy(uniqueId = live.generateUniqueId(), server = serverId)

                    }
                    liveXtreamDao.insertAll(listSave)
                    searchDao.insertAll(listSave.map {
                        SearchFtsEntity(
                            name = it.name,
                            type = "live",
                            uniqueId = it.generateUniqueId()
                        )
                    })
                }
                .onFailure {
                    Log.d("check liveXtream", "initData: Fail")
                }

            repository.getCategories(username = username, password = password)
                .onSuccess { listCategory ->
                    val category = listCategory.map { category ->
                        category.copy(serverId = serverId, type = "live")
                    }
                    categoryXtreamDao.insertAll(category)
                }
                .onFailure {
                }
        }

    }

    private var _serverId = MutableStateFlow<Int?>(null)
    private val _searchName = MutableStateFlow("")

    @OptIn(FlowPreview::class)
    val searchName = _searchName
        .debounce (400)
        .distinctUntilChanged()

    fun search(name: String) {
        _searchName.value = name
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val liveXtreamPaged: Flow<PagingData<LiveXtream>> =
        combine(
            _serverId.filterNotNull(),
            searchName
        ){ serverId, name ->
            Pair(serverId, name)
        }
        .flatMapLatest { (serverId, name) ->
            Pager(
                config = PagingConfig(
                    pageSize = 30,
                    prefetchDistance = 10,
                    enablePlaceholders = false
                )
            ) {
                liveXtreamDao.getLiveXtreamPagedBySearch(serverId, name)
            }.flow
        }
        .cachedIn(viewModelScope)

    fun setServerId(serverId: Int) {
        _serverId.value = serverId
    }

}