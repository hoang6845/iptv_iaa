package com.iptvplayer.m3u.stream.model.entity

import androidx.annotation.Keep
import androidx.room.Entity
import androidx.room.Index
import com.google.gson.annotations.SerializedName

@Keep
@Entity(
    primaryKeys = ["seriesId", "server", "categoryId"],
    indices = [Index(value = ["uniqueId", "server"])]
)
data class Series(
    @SerializedName("num")
    val num: Int,

    @SerializedName("name")
    val name: String,

    @SerializedName("series_id")
    val seriesId: Long,

    @SerializedName("cover")
    val cover: String,

    @SerializedName("plot")
    val plot: String,

    @SerializedName("cast")
    val cast: String,

    @SerializedName("director")
    val director: String,

    @SerializedName("genre")
    val genre: String,

    @SerializedName("releaseDate")
    val releaseDate: String,

    @SerializedName("last_modified")
    val lastModified: String,

    @SerializedName("rating")
    val rating: String,

    @SerializedName("rating_5based")
    val rating5Based: Double,

    @SerializedName("backdrop_path")
    val backdropPath: List<String>,

    @SerializedName("youtube_trailer")
    val youtubeTrailer: String,

    @SerializedName("episode_run_time")
    val episodeRunTime: String,

    @SerializedName("category_id")
    val categoryId: String,
    val server: Int,
    val uniqueId: String?=null
) {
    fun generateUniqueId() = "$num-$categoryId-series"
}