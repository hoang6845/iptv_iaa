package com.iptvplayer.m3u.stream.ui.movie_open

import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.iptvplayer.m3u.stream.api.MovieApiService
import com.iptvplayer.m3u.stream.api.MovieRepository
import com.iptvplayer.m3u.stream.model.dao.MovieDao
import com.iptvplayer.m3u.stream.model.dao.SearchDao
import com.iptvplayer.m3u.stream.model.dao.ServerDao
import com.iptvplayer.m3u.stream.model.dao.XtreamCategoryDao
import com.iptvplayer.m3u.stream.model.entity.RecommendCategory
import com.iptvplayer.m3u.stream.model.entity.SearchFtsEntity
import com.iptvplayer.m3u.stream.model.entity.SearchResult
import com.iptvplayer.m3u.stream.utils.AppConstants
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
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Inject

// ---------- State ----------
sealed class MovieUpdateState {
    object Idle : MovieUpdateState()
    data class Loading(val progress: Int) : MovieUpdateState()
    object Success : MovieUpdateState()
    data class Error(val message: String) : MovieUpdateState()
}

@HiltViewModel
class MovieOpenViewModel @Inject constructor(
    private val searchDao: SearchDao,
    private val movieDao: MovieDao,
    private val serverDao: ServerDao,
    private val categoryDao: XtreamCategoryDao,
) : BaseViewModel() {

    private val _serverId = MutableStateFlow<Int?>(null)
    private val _categoryId = MutableStateFlow<Int?>(null)
    private val _searchName = MutableStateFlow("")
    private var type: String = "movie"
    fun setType(type: String){
        this.type = type
    }
    private val _updateState = MutableStateFlow<MovieUpdateState>(MovieUpdateState.Idle)
    val updateState: StateFlow<MovieUpdateState> = _updateState.asStateFlow()

    @OptIn(FlowPreview::class)
    val searchName = _searchName
        .debounce(400)
        .distinctUntilChanged()

    fun setServerId(value: Int) {
        _serverId.value = value
    }

    fun setCategoryId(value: Int) {
        _categoryId.value = value
    }

    fun search(name: String) {
        _searchName.value = name
    }

    fun resetUpdateState() {
        _updateState.value = MovieUpdateState.Idle
    }



    fun updateMovie(serverId: Int) {
        if (_updateState.value is MovieUpdateState.Loading) return

        viewModelScope.launch {
            _updateState.value = MovieUpdateState.Loading(10)

            try {
                val server = withContext(Dispatchers.IO) {
                    serverDao.getServerById(serverId)
                }
                if (server == null) {
                    _updateState.value = MovieUpdateState.Error("Server not found")
                    return@launch
                }

                val categoryId = _categoryId.value
                if (categoryId == null) {
                    _updateState.value = MovieUpdateState.Error("Category not found")
                    return@launch
                }

                _updateState.value = MovieUpdateState.Loading(30)
                val movieRepository = MovieRepository(createApiService(server.server))

                // Force refresh: xóa cache cũ rồi gọi API
                movieRepository.clearCache()
                val result = withContext(Dispatchers.IO) {
                    movieRepository.getMoviesByCategory(
                        username = server.username,
                        password = server.password,
                        categoryId = categoryId
                    )
                }

                _updateState.value = MovieUpdateState.Loading(60)

                result
                    .onSuccess { movies ->
                        if (movies.isEmpty()) {
                            _updateState.value = MovieUpdateState.Error("No movies found from server")
                            return@launch
                        }

                        _updateState.value = MovieUpdateState.Loading(80)

                        val mapped = movies.map {
                            it.copy(server = server.id, uniqueId = it.generateUniqueId())
                        }

                        withContext(Dispatchers.IO) {
                            movieDao.insertAll(mapped)
                            val searchEntities = mapped.map { movie ->
                                SearchFtsEntity(
                                    name = movie.name,
                                    type = "movie",
                                    uniqueId = movie.uniqueId!!
                                )
                            }
                            searchDao.insertAll(searchEntities)
                        }

                        _updateState.value = MovieUpdateState.Loading(100)
                        _updateState.value = MovieUpdateState.Success
                    }
                    .onFailure { e ->
                        _updateState.value = MovieUpdateState.Error(e.message ?: "Failed to fetch movies")
                    }

            } catch (e: Exception) {
                _updateState.value = MovieUpdateState.Error(e.message ?: "Unknown error")
            }
        }
    }

    private fun createApiService(baseUrl: String): MovieApiService {
        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        return retrofit.create(MovieApiService::class.java)
    }

    private val _isVirtualRecommend  = MutableStateFlow(false)
    private val _filterCategoryId    = MutableStateFlow<Int?>(null)   // null = All
    private val _recommendCategoryIds = MutableStateFlow<List<Int>>(emptyList())

    val filterCategoryId: StateFlow<Int?> = _filterCategoryId.asStateFlow()

    private val _recommendCategories = MutableStateFlow<List<RecommendCategory>>(emptyList())
    val recommendCategories: StateFlow<List<RecommendCategory>> = _recommendCategories.asStateFlow()

    fun setIsVirtualRecommend(value: Boolean) {
        _isVirtualRecommend.value = value
    }

    fun setFilterCategory(categoryId: Int?) {
        _filterCategoryId.value = categoryId
    }

//    fun loadRecommendCategories(serverId: Int) {
//        viewModelScope.launch(Dispatchers.IO) {
//            val countPerCategory = listOf(19, 12, 16, 9, 55, 17, 11, 8)  // giống XtreamViewModel
//            val sourceCategories = categoryDao.selectByServerId(serverId)
//                .filter { it.type == AppConstants.MOVIE }
//                .sortedBy { it.categoryId }
//                .take(countPerCategory.size)
//
//            _recommendCategoryIds.value = sourceCategories.map { it.categoryId }
//
//            val items = sourceCategories.mapIndexed { index, cat ->
//                val count = movieDao.countByCategoryAndServer(cat.categoryId, serverId)
//                RecommendCategory(
//                    categoryId   = cat.categoryId,
//                    categoryName = cat.categoryName,
//                    count        = count
//                )
//            }
//            _recommendCategories.value = items
//        }
//    }

    fun loadRecommendCategories(serverId: Int) {
        viewModelScope.launch(Dispatchers.IO) {

            val sourceCategories = categoryDao.selectByServerId(serverId)
                .filter { it.type == AppConstants.MOVIE }
                .sortedBy { it.categoryId }

            _recommendCategoryIds.value = sourceCategories.map { it.categoryId }

            val items = sourceCategories.map { cat ->
                val count = movieDao.countByCategoryAndServer(cat.categoryId, serverId)
                RecommendCategory(
                    categoryId   = cat.categoryId,
                    categoryName = cat.categoryName,
                    count        = count
                )
            }

            _recommendCategories.value = items
        }
    }

    // Thêm field
    private val _sortOrder = MutableStateFlow("none")

    fun setSort(order: String) { _sortOrder.value = order }
    fun getSort(): String = _sortOrder.value

    @OptIn(ExperimentalCoroutinesApi::class)
    val movieResult: Flow<PagingData<SearchResult>> =
        combine(
            _serverId.filterNotNull(),
            _categoryId.filterNotNull(),
            searchName,
            _isVirtualRecommend,
            _filterCategoryId,
            _recommendCategoryIds,
            _sortOrder          // 👈 thêm vào
        ) { arr ->
            @Suppress("UNCHECKED_CAST")
            val serverId         = arr[0] as Int
            val categoryId       = arr[1] as Int
            val name             = arr[2] as String
            val isRecommend      = arr[3] as Boolean
            val filterCategoryId = arr[4] as Int?
            val recommendIds     = arr[5] as List<Int>
            val sort             = arr[6] as String
            arrayOf(serverId, categoryId, name, isRecommend, filterCategoryId, recommendIds, sort)
        }.flatMapLatest { arr ->
            val serverId         = arr[0] as Int
            val categoryId       = arr[1] as Int
            val name             = arr[2] as String
            val isRecommend      = arr[3] as Boolean
            val filterCategoryId = arr[4] as Int?
            val recommendIds     = arr[5] as List<Int>
            val sort             = arr[6] as String
            val safeQuery        = if (name.isBlank()) "*" else "$name*"

            Pager(PagingConfig(pageSize = 20, prefetchDistance = 10, enablePlaceholders = false)) {
                when {
                    isRecommend && filterCategoryId == null && recommendIds.isNotEmpty() -> {
                        if (safeQuery == "*")
                            searchDao.searchAllMovieByCategoriesWSort(serverId, recommendIds, sort)
                        else
                            searchDao.searchMovieByCategoriesWSort(serverId, recommendIds, safeQuery, sort)
                    }
                    isRecommend && filterCategoryId != null -> {
                        if (safeQuery == "*")
                            searchDao.searchAllMovieByCategoryWSort(serverId, filterCategoryId, sort)
                        else
                            searchDao.searchMovieByCategoryWSort(serverId, filterCategoryId, safeQuery, sort)
                    }
                    type == "series" -> {
                        if (safeQuery == "*")
                            searchDao.searchAllSeriesByCategoryWSort(serverId, categoryId, sort)
                        else
                            searchDao.searchSeriesByCategoryWSort(serverId, categoryId, safeQuery, sort)
                    }
                    else -> {
                        if (safeQuery == "*")
                            searchDao.searchAllMovieByCategoryWSort(serverId, categoryId, sort)
                        else
                            searchDao.searchMovieByCategoryWSort(serverId, categoryId, safeQuery, sort)
                    }
                }
            }.flow
        }.cachedIn(viewModelScope)
}