package com.iptvplayer.m3u.stream.model.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class RecentChannel(
    @PrimaryKey(autoGenerate = false)
    val channelId: String,
    val channelIcon: String?,
    val lastWatchTime: Long,
    val url: String,
    val name: String,
    val playlistId: Long,
    val group: String?,
) {
}