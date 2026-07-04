package com.iptvplayer.m3u.stream.model.entity

import androidx.annotation.Keep
import androidx.room.Entity

@Entity(
    primaryKeys = ["seriesId", "seasonNumber", "episodeNum", "server"]
)
@Keep
data class EpisodeEntity(
    val seriesId: Long,
    val seasonNumber: Int,
    val id: String,
    val server: Int,
    val episodeNum: Int,
    val title: String,
    val plot: String?,
    val duration: String?,
    val movieImage: String?,
    val rating: Double?,
    val containerExtension: String
)