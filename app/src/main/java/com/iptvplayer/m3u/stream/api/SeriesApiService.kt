package com.iptvplayer.m3u.stream.api

import com.iptvplayer.m3u.stream.model.entity.Series
import com.iptvplayer.m3u.stream.model.entity.SeriesDetailResponse
import com.iptvplayer.m3u.stream.model.entity.XtreamCategory
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface SeriesApiService {

    @GET("player_api.php")
    suspend fun getSeriesCategories(
        @Query("username") username: String,
        @Query("password") password: String,
        @Query("action") action: String = "get_series_categories"
    ): Response<List<XtreamCategory>>

    @GET("player_api.php")
    suspend fun getSeriesByCategory(
        @Query("username") username: String,
        @Query("password") password: String,
        @Query("action") action: String = "get_series",
        @Query("category_id") categoryId: Int
    ): Response<List<Series>>

    @GET("player_api.php")
    suspend fun getAllSeries(
        @Query("username") username: String,
        @Query("password") password: String,
        @Query("action") action: String = "get_series",
    ): Response<List<Series>>

    @GET("player_api.php")
    suspend fun getSeriesInfo(
        @Query("username") username: String,
        @Query("password") password: String,
        @Query("action") action: String = "get_series_info",
        @Query("series_id") seriesId: Long
    ): Response<SeriesDetailResponse>




}