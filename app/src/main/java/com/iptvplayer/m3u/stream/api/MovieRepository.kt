package com.iptvplayer.m3u.stream.api

import android.util.Log
import com.iptvplayer.m3u.stream.model.entity.LoginResponse
import com.iptvplayer.m3u.stream.model.entity.Movie
import com.iptvplayer.m3u.stream.model.entity.MovieDetailResponse
import com.iptvplayer.m3u.stream.model.entity.XtreamCategory
import com.iptvplayer.m3u.stream.utils.AppConstants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MovieRepository(private val apiService: MovieApiService) {
    
    private val movieCache = mutableMapOf<Int, List<Movie>>()
    
    suspend fun getCategories(username: String, password: String): Result<List<XtreamCategory>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getVodCategories(username, password)
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
    
    suspend fun getMoviesByCategory(username: String, password: String, categoryId: Int): Result<List<Movie>> = withContext(Dispatchers.IO) {
        try {
            movieCache[categoryId]?.let {
                return@withContext Result.success(it)
            }
            
            val response = apiService.getMoviesByCategory(username, password, "get_vod_streams", categoryId)
            if (response.isSuccessful && response.body() != null) {
                val movies = response.body()!!
                movieCache[categoryId] = movies
                Result.success(movies)
            } else {
                Result.failure(Exception("Failed to load movies"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun checkUser(
        username: String,
        password: String
    ): Result<LoginResponse> = withContext(Dispatchers.IO) {
        return@withContext try {
            val response = apiService.checkUser(username, password)

            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(
                    Exception("Incorrect username or password")
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getMovieInfo(
        username: String,
        password: String,
        vodId: Long
    ): Result<MovieDetailResponse> = withContext(
        Dispatchers.IO
    ) {
        return@withContext try {
            val response = apiService.getMovieInfo(username, password, AppConstants.GET_VOD_INFO, vodId)

            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(
                    Exception("Incorrect username or password")
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    fun clearCache() {
        movieCache.clear()
    }
}