package com.iptvplayer.m3u.stream.api

import android.util.Log
import com.iptvplayer.m3u.stream.model.entity.LiveXtream
import com.iptvplayer.m3u.stream.model.entity.XtreamCategory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LiveXtreamRepository(private val apiService: LiveXtreamService) {
    suspend fun getCategories(username: String, password: String): Result<List<XtreamCategory>> =
        withContext(Dispatchers.IO) {
            try {
                val response = apiService.getXtreamCategory(username, password)
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

    suspend fun getLiveXtream(username: String, password: String): Result<List<LiveXtream>> =
        withContext(Dispatchers.IO) {
            try {
                val response = apiService.getLiveXtream(username, password)
                if (response.isSuccessful && response.body() != null) {
                    Result.success(response.body()!!)
                } else {
                    val error = response.errorBody()?.string()
                    Result.failure(
                        Exception("HTTP ${response.code()} - $error")
                    )
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
}