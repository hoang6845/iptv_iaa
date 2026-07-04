package com.iptvplayer.m3u.stream.model.entity

import androidx.room.Entity

@Entity(
    tableName = "favourites",
    primaryKeys = ["server", "uniqueId"]
)
data class Favourite(
    val server: Int,
    val uniqueId: String,
    val addedAt: Long = System.currentTimeMillis()
)