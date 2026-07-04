package com.iptvplayer.m3u.stream.model.entity

import androidx.annotation.Keep
import androidx.room.Entity
import androidx.room.PrimaryKey
@Keep
@Entity(tableName = "MovieDetail",
    primaryKeys = ["streamId", "server"])
data class MovieDetailEntity(
    val streamId: Int,
    val server: Int,
    val name: String?,
    val releaseDate: String?,
    val rating: String?,
    val genre: String?,
    val country: String?,
    val duration: String?,
    val durationSecs: Int?,
    val coverBig: String?,
    val movieImage: String?,
    val backdropPath: List<String>?,
    val youtubeTrailer: String?,
    val description: String?,
    val director: String?,
    val actors: String?,
    val cast: String?,
    val age: String?,
    val containerExtension: String?,
)
