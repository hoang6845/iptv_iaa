package com.iptvplayer.m3u.stream.api

import android.util.Log
import com.iptvplayer.m3u.stream.model.entity.Series
import com.iptvplayer.m3u.stream.model.entity.SeriesDetailResponse
import com.iptvplayer.m3u.stream.model.entity.XtreamCategory
import com.iptvplayer.m3u.stream.utils.AppConstants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SeriesRepository(private val apiService: SeriesApiService) {
    
    private val seriesCache = mutableMapOf<Int, List<Series>>()
    
    suspend fun getCategories(username: String, password: String): Result<List<XtreamCategory>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getSeriesCategories(username, password)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val error = response.errorBody()?.string()
                Log.d(
                    "check api Xtream",
                    "Error body: $error"
                )
                Result.failure(
                    Exception("HTTP ${response.code()} - $error")
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getSeriesByCategory(username: String, password: String, categoryId: Int): Result<List<Series>> = withContext(Dispatchers.IO) {
        try {
            seriesCache[categoryId]?.let {
                return@withContext Result.success(it)
            }
            
            val response = apiService.getSeriesByCategory(username, password, "get_series", categoryId)
            if (response.isSuccessful && response.body() != null) {
                val movies = response.body()!!
                seriesCache[categoryId] = movies
                Result.success(movies)
            } else {
                Result.failure(Exception("Failed to load movies"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }


    suspend fun getSeriesInfo(
        username: String,
        password: String,
        seriesId: Long
    ): Result<SeriesDetailResponse> = withContext(Dispatchers.IO) {

        Log.d("API_DEBUG", "Request getSeriesInfo: username=$username, seriesId=$seriesId")

        return@withContext try {
            val response = apiService.getSeriesInfo(
                username,
                password,
                AppConstants.GET_SERIES_INFO,
                seriesId
            )

            Log.d("API_DEBUG", "Response code: ${response.code()}")

            if (response.isSuccessful && response.body() != null) {
                Log.d("API_DEBUG", "Response body: ${response.body()}")
                Result.success(response.body()!!)
            } else {
                Log.e("API_DEBUG", "Error response: ${response.errorBody()?.string()}")
                Result.failure(Exception("Incorrect username or password"))
            }

        } catch (e: Exception) {
            Log.e("API_DEBUG", "Exception: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    fun clearCache() {
        seriesCache.clear()
    }
}