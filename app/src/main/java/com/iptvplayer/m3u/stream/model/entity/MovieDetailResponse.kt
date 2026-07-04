package com.iptvplayer.m3u.stream.model.entity

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName
@Keep
data class MovieDetailResponse(
    @SerializedName("info")
    val info: MovieInfo,

    @SerializedName("movie_data")
    val movieData: MovieData
)
@Keep
data class MovieInfo(

    @SerializedName("name")
    val name: String,

    @SerializedName("releasedate")
    val releaseDate: String?,

    @SerializedName("rating")
    val rating: String?,

    @SerializedName("genre")
    val genre: String?,

    @SerializedName("country")
    val country: String?,

    @SerializedName("duration")
    val duration: String?,

    @SerializedName("duration_secs")
    val durationSecs: Int?,

    @SerializedName("cover_big")
    val coverBig: String?,

    @SerializedName("movie_image")
    val movieImage: String?,

    @SerializedName("backdrop_path")
    val backdropPath: List<String>?,

    @SerializedName("youtube_trailer")
    val youtubeTrailer: String?,

    @SerializedName("description")
    val description: String?,

    @SerializedName("director")
    val director: String?,

    @SerializedName("actors")
    val actors: String?,

    @SerializedName("cast")
    val cast: String?,

    @SerializedName("age")
    val age: String?
)
@Keep
data class MovieData(
    @SerializedName("stream_id")
    val streamId: Int,

    @SerializedName("name")
    val name: String?,

    @SerializedName("category_id")
    val categoryId: String?,
    @SerializedName("container_extension")
    val containerExtension: String?,
)