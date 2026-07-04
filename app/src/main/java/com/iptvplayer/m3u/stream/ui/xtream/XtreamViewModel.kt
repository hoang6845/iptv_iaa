package com.iptvplayer.m3u.stream.ui.xtream

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.iptvplayer.m3u.stream.api.LiveXtreamRepository
import com.iptvplayer.m3u.stream.model.dao.LiveXtreamDao
import com.iptvplayer.m3u.stream.model.dao.MovieDao
import com.iptvplayer.m3u.stream.model.dao.MovieDetailDao
import com.iptvplayer.m3u.stream.model.dao.SearchDao
import com.iptvplayer.m3u.stream.model.dao.SeriesDao
import com.iptvplayer.m3u.stream.model.dao.XtreamCategoryDao
import com.iptvplayer.m3u.stream.model.entity.CarouselItem
import com.iptvplayer.m3u.stream.model.entity.CategoryMoviesState
import com.iptvplayer.m3u.stream.model.entity.SearchFtsEntity
import com.iptvplayer.m3u.stream.model.entity.XtreamCategory
import com.iptvplayer.m3u.stream.utils.AppConstants
import dagger.hilt.android.lifecycle.HiltViewModel
import hoang.dqm.codebase.base.viewmodel.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import java.net.URLDecoder
import javax.inject.Inject

@HiltViewModel
class XtreamViewModel @Inject constructor(
    private val categoryDao: XtreamCategoryDao,
    private val searchDao: SearchDao,
    private val movieDao: MovieDao,
    private val seriesDao: SeriesDao,
    private val movieDetailDao: MovieDetailDao,
    private val liveXtreamDao: LiveXtreamDao
) : BaseViewModel() {
    private var liveOffset = 0
    private val livePageSize = 100
    private var isAllLiveLoaded = false
    companion object {
        private const val TAG = "XtreamVM"
        const val LIVE_VIRTUAL_CATEGORY_ID    = -999
        const val RECOMMEND_VIRTUAL_CATEGORY_ID = -998

        private const val RECOMMEND_PER_CATEGORY    = 5
        private const val RECOMMEND_SOURCE_CATEGORIES = 6
    }

    private val _categories = MutableStateFlow<List<XtreamCategory>>(emptyList())
    val categories: StateFlow<List<XtreamCategory>> = _categories.asStateFlow()

    private val _categoryMoviesState = MutableStateFlow<Map<Int, CategoryMoviesState>>(emptyMap())
    val categoryMoviesState: StateFlow<Map<Int, CategoryMoviesState>> = _categoryMoviesState.asStateFlow()

    private val _isLoadingMovie = MutableStateFlow(false)
    val isLoadingMovie: StateFlow<Boolean> = _isLoadingMovie.asStateFlow()

    fun loadCategories(idServer: Int) {
        viewModelScope.launch {
            _isLoadingMovie.value = true
            try {
                val list = categoryDao.selectByServerId(idServer)

                val movieCategories  = list.filter { it.type == AppConstants.MOVIE }
                val seriesCategories = list.filter { it.type == AppConstants.SERIES }

                val liveFakeCategory = XtreamCategory(
                    categoryId   = LIVE_VIRTUAL_CATEGORY_ID,
                    categoryName = "Live Now",
                    type         = AppConstants.LIVE,
                    serverId     = idServer
                )

                val recommendFakeCategory = XtreamCategory(
                    categoryId   = RECOMMEND_VIRTUAL_CATEGORY_ID,
                    categoryName = "Recommended For You",
                    type         = AppConstants.MOVIE,
                    serverId     = idServer
                )

                val reordered = listOfNotNull(
                    liveFakeCategory,
                    recommendFakeCategory,
                    movieCategories.getOrNull(0)?.copy(categoryName = "Trending Now"),
                    seriesCategories.getOrNull(0)?.copy(categoryName = "New Series"),
                )
//                    +    movieCategories.drop(2) +
//                        seriesCategories.drop(1)

                _categories.value = reordered
                val currentState = _categoryMoviesState.value
                _categoryMoviesState.value = reordered.associate { category ->
                    category.categoryId to (currentState[category.categoryId] ?: CategoryMoviesState.NotLoaded)
                }

                movieCategories.getOrNull(1)?.let {
                    loadCarouselMovies(it.categoryId, idServer)
                }
            } catch (e: Exception) {
                Log.e(TAG, "loadCategories error", e)
            } finally {
                _isLoadingMovie.value = false
            }
        }
    }

    fun loadRecommendMovies(serverId: Int) {
        val currentState = _categoryMoviesState.value[RECOMMEND_VIRTUAL_CATEGORY_ID]
        if (currentState is CategoryMoviesState.Loaded ||
            currentState is CategoryMoviesState.Loading) return

        viewModelScope.launch {
            _categoryMoviesState.value = _categoryMoviesState.value.toMutableMap().apply {
                this[RECOMMEND_VIRTUAL_CATEGORY_ID] = CategoryMoviesState.Loading
            }
            try {
                val countPerCategory = listOf(19, 12, 16, 9, 55, 17, 11, 8)

                val sourceCategories = categoryDao.selectByServerId(serverId)
                    .filter { it.type == AppConstants.MOVIE }
                    .sortedBy { it.categoryId }
                    .take(countPerCategory.size)

                val recommended = sourceCategories
                    .mapIndexed { index, cat ->
                        val limit = countPerCategory.getOrElse(index) { 5 }
                        movieDao.selectMovieByCategoryAndServer(cat.categoryId, serverId)
                            .take(limit)
                    }
                    .flatten()
                    .distinctBy { it.streamId }

                _categoryMoviesState.value = _categoryMoviesState.value.toMutableMap().apply {
                    this[RECOMMEND_VIRTUAL_CATEGORY_ID] = if (recommended.isEmpty())
                        CategoryMoviesState.Error("No recommendations")
                    else
                        CategoryMoviesState.Loaded(recommended)
                }
            } catch (e: Exception) {
                Log.e(TAG, "loadRecommendMovies error", e)
                _categoryMoviesState.value = _categoryMoviesState.value.toMutableMap().apply {
                    this[RECOMMEND_VIRTUAL_CATEGORY_ID] = CategoryMoviesState.Error(e.message ?: "Unknown")
                }
            }
        }
    }

    fun loadAllLive(serverId: Int) {
        val currentState = _categoryMoviesState.value[LIVE_VIRTUAL_CATEGORY_ID]

        // Guard đủ cases
        if (currentState is CategoryMoviesState.Loading) return
        if ((currentState as? CategoryMoviesState.LoadedLive)?.isLoadingMore == true) return
        if (isAllLiveLoaded) return

        // ← Lấy existing TRƯỚC khi thay đổi state
        val existingLives = (currentState as? CategoryMoviesState.LoadedLive)?.lives ?: emptyList()

        viewModelScope.launch {
            _categoryMoviesState.value = _categoryMoviesState.value.toMutableMap().apply {
                this[LIVE_VIRTUAL_CATEGORY_ID] = if (existingLives.isEmpty())
                    CategoryMoviesState.Loading                                    // lần đầu → skeleton toàn bộ
                else
                    CategoryMoviesState.LoadedLive(existingLives, canLoadMore = true, isLoadingMore = true) // load more → giữ list + skeleton cuối
            }

            try {
                val newBatch = liveXtreamDao.getPaged(serverId, livePageSize, liveOffset)
                val merged = existingLives + newBatch  // ← nối vào list cũ

                if (newBatch.size < livePageSize) isAllLiveLoaded = true
                liveOffset += newBatch.size

                _categoryMoviesState.value = _categoryMoviesState.value.toMutableMap().apply {
                    this[LIVE_VIRTUAL_CATEGORY_ID] = if (merged.isEmpty())
                        CategoryMoviesState.Error("No live channels")
                    else
                        CategoryMoviesState.LoadedLive(
                            lives = merged,
                            canLoadMore = !isAllLiveLoaded,
                            isLoadingMore = false
                        )
                }
            } catch (e: Exception) {
                Log.e(TAG, "loadAllLive error", e)
                _categoryMoviesState.value = _categoryMoviesState.value.toMutableMap().apply {
                    this[LIVE_VIRTUAL_CATEGORY_ID] = if (existingLives.isEmpty())
                        CategoryMoviesState.Error(e.message ?: "Unknown error")
                    else
                        CategoryMoviesState.LoadedLive(existingLives, canLoadMore = true, isLoadingMore = false)
                }
            }
        }
    }

    fun setLiveLoading() {
        _categoryMoviesState.value = _categoryMoviesState.value.toMutableMap().apply {
            this[LIVE_VIRTUAL_CATEGORY_ID] = CategoryMoviesState.Loading
        }
    }

    fun resetLiveState() {
        liveOffset = 0
        isAllLiveLoaded = false
        _categoryMoviesState.value = _categoryMoviesState.value.toMutableMap().apply {
            this[LIVE_VIRTUAL_CATEGORY_ID] = CategoryMoviesState.NotLoaded
        }
    }

    fun loadMoviesForCategory(categoryId: Int, serverId: Int) {
        if (categoryId == RECOMMEND_VIRTUAL_CATEGORY_ID) {
            loadRecommendMovies(serverId)
            return
        }
        val currentState = _categoryMoviesState.value[categoryId]
        if (currentState is CategoryMoviesState.Loaded ||
            currentState is CategoryMoviesState.Loading ||
            currentState is CategoryMoviesState.Error
        ) return

        viewModelScope.launch {
            _categoryMoviesState.value = _categoryMoviesState.value.toMutableMap().apply {
                this[categoryId] = CategoryMoviesState.Loading
            }
            try {
                val movies = movieDao.selectMovieByCategoryAndServer(categoryId, serverId)
                _categoryMoviesState.value = _categoryMoviesState.value.toMutableMap().apply {
                    this[categoryId] = if (movies.isEmpty())
                        CategoryMoviesState.Error("Empty movie list")
                    else
                        CategoryMoviesState.Loaded(movies)
                }
            } catch (e: Exception) {
                Log.e(TAG, "loadMovies error", e)
                _categoryMoviesState.value = _categoryMoviesState.value.toMutableMap().apply {
                    this[categoryId] = CategoryMoviesState.Error(e.message ?: "Unknown error")
                }
            }
        }
    }

    fun loadSeriesForCategory(categoryId: Int, serverId: Int) {
        val currentState = _categoryMoviesState.value[categoryId]
        if (currentState is CategoryMoviesState.LoadedSeries ||
            currentState is CategoryMoviesState.Loading ||
            currentState is CategoryMoviesState.Error
        ) return

        viewModelScope.launch {
            _categoryMoviesState.value = _categoryMoviesState.value.toMutableMap().apply {
                this[categoryId] = CategoryMoviesState.Loading
            }
            try {
                val series = seriesDao.selectSeriesByCategoryAndServer(categoryId, serverId)
                _categoryMoviesState.value = _categoryMoviesState.value.toMutableMap().apply {
                    this[categoryId] = if (series.isEmpty())
                        CategoryMoviesState.Error("Empty movie list")
                    else
                        CategoryMoviesState.LoadedSeries(series)
                }
            } catch (e: Exception) {
                Log.e(TAG, "loadSeries error", e)
                _categoryMoviesState.value = _categoryMoviesState.value.toMutableMap().apply {
                    this[categoryId] = CategoryMoviesState.Error(e.message ?: "Unknown error")
                }
            }
        }
    }

    private val _carouselMovies = MutableStateFlow<List<CarouselItem>>(emptyList())
    val carouselMovies: StateFlow<List<CarouselItem>> = _carouselMovies.asStateFlow()

    private suspend fun loadCarouselMovies(categoryId: Int, serverId: Int) {
        try {
            val movies = movieDao.selectMovieByCategoryAndServer(categoryId, serverId)

            val carouselItems = movies.map { movie ->
                val backdropUrl = movie.streamId?.let { streamId ->
                    val raw = movieDetailDao
                        .getMovieBackdrop(streamId, serverId)
                        ?.firstOrNull()

                    parseListStringSafe(raw).firstOrNull()
                }

                CarouselItem(
                    movie = movie,
                    backdropUrl = backdropUrl
                )
            }

            _carouselMovies.value = carouselItems
        } catch (e: Exception) {
            Log.e(TAG, "loadCarouselMovies error", e)
        }
    }

    fun parseListStringSafe(raw: String?): List<String> {
        if (raw.isNullOrEmpty()) return emptyList()

        return try {
            val decoded = URLDecoder.decode(raw, "UTF-8")

            if (decoded.startsWith("[")) {
                val jsonArray = JSONArray(decoded)
                List(jsonArray.length()) { index ->
                    jsonArray.optString(index)
                }
            } else {
                listOf(decoded)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            listOf(raw)
        }
    }

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
                    resetLiveState()
                    loadAllLive(serverId)
                }
                .onFailure {
                    Log.d(TAG, "getLiveXtream: Failed")
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