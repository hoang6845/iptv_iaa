package com.iptvplayer.m3u.stream.api

import com.iptvplayer.m3u.stream.model.entity.LoginResponse
import com.iptvplayer.m3u.stream.model.entity.Movie
import com.iptvplayer.m3u.stream.model.entity.MovieDetailResponse
import com.iptvplayer.m3u.stream.model.entity.XtreamCategory
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface MovieApiService {

    @GET("player_api.php")
    suspend fun getVodCategories(
        @Query("username") username: String,
        @Query("password") password: String,
        @Query("action") action: String = "get_vod_categories"
    ): Response<List<XtreamCategory>>

    @GET("player_api.php")
    suspend fun getMoviesByCategory(
        @Query("username") username: String,
        @Query("password") password: String,
        @Query("action") action: String = "get_vod_streams",
        @Query("category_id") categoryId: Int
    ): Response<List<Movie>>

    @GET("player_api.php")
    suspend fun getMovies(
        @Query("username") username: String,
        @Query("password") password: String,
        @Query("action") action: String = "get_vod_streams"
    ): Response<List<Movie>>

    @GET("player_api.php")
    suspend fun checkUser(
        @Query("username") username: String,
        @Query("password") password: String,
    ): Response<LoginResponse>

    @GET("player_api.php")
    suspend fun getMovieInfo(
        @Query("username") username: String,
        @Query("password") password: String,
        @Query("action") action: String = "get_vod_info",
        @Query("vod_id") vodId: Long
    ): Response<MovieDetailResponse>
}