package com.iptvplayer.m3u.stream.api

import com.iptvplayer.m3u.stream.model.entity.LiveXtream
import com.iptvplayer.m3u.stream.model.entity.XtreamCategory
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface LiveXtreamService {

    @GET("player_api.php")
    suspend fun getLiveXtream(
        @Query("username") username: String,
        @Query("password") password: String,
        @Query("action") action: String = "get_live_streams"
    ): Response<List<LiveXtream>>

    @GET("player_api.php")
    suspend fun getXtreamCategory(
        @Query("username") username: String,
        @Query("password") password: String,
        @Query("action") action: String = "get_live_categories",
    ): Response<List<XtreamCategory>>


}