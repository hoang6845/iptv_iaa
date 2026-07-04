package com.iptvplayer.m3u.stream.model.entity

import androidx.annotation.Keep
import androidx.room.Entity
@Keep
@Entity(
    tableName = "season",
    primaryKeys = ["seriesId", "seasonNumber", "server"]
)
data class SeasonEntity(
    val seriesId: Long,
    val seasonNumber: Int,
    val server: Int,
    val name: String,
    val airDate: String?,
    val episodeCount: Int,
    val voteAverage: Double?,
    val cover: String?
)

