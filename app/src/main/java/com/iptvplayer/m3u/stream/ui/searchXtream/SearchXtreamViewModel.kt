package com.iptvplayer.m3u.stream.ui.searchXtream

import android.util.Log
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.iptvplayer.m3u.stream.api.LiveXtreamRepository
import com.iptvplayer.m3u.stream.model.dao.FavouriteDao
import com.iptvplayer.m3u.stream.model.dao.LiveXtreamDao
import com.iptvplayer.m3u.stream.model.dao.SearchDao
import com.iptvplayer.m3u.stream.model.dao.XtreamCategoryDao
import com.iptvplayer.m3u.stream.model.entity.SearchFtsEntity
import com.iptvplayer.m3u.stream.model.entity.SearchResult
import com.iptvplayer.m3u.stream.utils.AppConstants
import dagger.hilt.android.lifecycle.HiltViewModel
import hoang.dqm.codebase.base.viewmodel.BaseViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchXtreamViewModel @Inject constructor(
    private val searchDao: SearchDao,
    private val liveXtreamDao: LiveXtreamDao,
    private val categoryDao: XtreamCategoryDao,
    private val favouriteDao: FavouriteDao
) : BaseViewModel() {
    private val _serverId = MutableStateFlow<Int?>(null)
    private val _searchName = MutableStateFlow("")
    private val _selectedCategory = MutableStateFlow("all")
    private val _sortOrder = MutableStateFlow("none")

    fun setSort(order: String) {
        _sortOrder.value = order
    }
    fun getSort(): String {
        return _sortOrder.value
    }
    @OptIn(FlowPreview::class)
    val searchName = _searchName
        .debounce(400)
        .distinctUntilChanged()

    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    fun search(name: String) {
        _searchName.value = name
    }

    fun setServerId(value: Int) {
        _serverId.value = value
    }

    fun setCategory(type: String) {
        _selectedCategory.value = type
    }
    data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

    val showMovieSection: StateFlow<Boolean> =
        _selectedCategory
            .map { it == "all" || it == AppConstants.MOVIE || it == AppConstants.SERIES }
            .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val showLiveSection: StateFlow<Boolean> =
        _selectedCategory
            .map { it == "all" || it == AppConstants.LIVE }
            .stateIn(viewModelScope, SharingStarted.Eagerly, true)
    @OptIn(ExperimentalCoroutinesApi::class)
    val movieResult: Flow<PagingData<SearchResult>> =
        combine(
            _serverId.filterNotNull(),
            searchName,
            _selectedCategory,
            _sortOrder  // 👈 thêm vào combine
        ) { serverId, name, category, sort ->
            Quadruple(serverId, name, category, sort)
        }.flatMapLatest { (serverId, name, category, sort) ->
            val safeQuery = if (name.isBlank()) "*" else "$name*"
            val typeFilter = when (category) {
                AppConstants.MOVIE -> AppConstants.MOVIE
                AppConstants.SERIES -> AppConstants.SERIES
                else -> null
            }
            Pager(PagingConfig(pageSize = 20, prefetchDistance = 10, enablePlaceholders = false)) {
                when (typeFilter) {
                    null -> when {
                        safeQuery == "*" -> searchDao.searchAllPagingMovieWSort(serverId, sort)
                        else -> searchDao.searchPagingMovieWSort(serverId, safeQuery, sort)
                    }
                    AppConstants.MOVIE -> when {
                        safeQuery == "*" -> searchDao.searchAllMovieWSort(serverId, sort)
                        else -> searchDao.searchMovieWSort(serverId, safeQuery, sort)
                    }
                    else -> when {
                        safeQuery == "*" -> searchDao.searchAllSeriesWSort(serverId, sort)
                        else -> searchDao.searchSeriesWSort(serverId, safeQuery, sort)
                    }
                }
            }.flow
        }.cachedIn(viewModelScope)

    @OptIn(ExperimentalCoroutinesApi::class)
    val liveResult: Flow<PagingData<SearchResult>> =
        combine(
            _serverId.filterNotNull(),
            searchName,
            _selectedCategory,
            _sortOrder  // 👈 thêm vào combine
        ) { serverId, name, category, sort ->
            Quadruple(serverId, name, category, sort)
        }.flatMapLatest { (serverId, name, category, sort) ->
            if (category != "all" && category != "live") {
                return@flatMapLatest flowOf(PagingData.empty())
            }
            val safeQuery = if (name.isBlank()) "*" else "$name*"
            Pager(PagingConfig(pageSize = 20, prefetchDistance = 10, enablePlaceholders = false)) {
                when {
                    safeQuery == "*" -> searchDao.searchAllPagingLiveWSort(serverId, sort)
                    else -> searchDao.searchPagingLiveWSort(serverId, safeQuery, sort)
                }
            }.flow
        }.cachedIn(viewModelScope)

    @OptIn(ExperimentalCoroutinesApi::class)
    val favouriteResult: StateFlow<List<SearchResult>> =
        combine(_serverId.filterNotNull(), _selectedCategory) { _, category ->
            category
        }.flatMapLatest { category ->
            when (category) {
                AppConstants.MOVIE   -> favouriteDao.getFavouriteMovies()
                AppConstants.SERIES  -> favouriteDao.getFavouriteSeries()
                AppConstants.LIVE    -> favouriteDao.getFavouriteLives()
                else -> combine(        // "all"
                    favouriteDao.getFavouriteMovies(),
                    favouriteDao.getFavouriteSeries(),
                    favouriteDao.getFavouriteLives()
                ) { movies, series, lives -> movies + series + lives }
            }
        }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())


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
                    Log.d("SearchXtreamViewModel", "getLiveXtream: Failed")
                }
            repository.getCategories(username = username, password = password)
                .onSuccess { listCategory ->
                    val category = listCategory.map { category ->
                        category.copy(serverId = serverId, type = "live")
                    }
                    categoryDao.insertAll(category)
                }
                .onFailure {
                }
        }
    }

}