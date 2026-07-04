package com.iptvplayer.m3u.stream.model.entity

import android.os.Parcelable
import androidx.annotation.Keep
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize
import java.util.UUID

@Keep
@Parcelize
@Entity(
    indices = [Index("playlistId")]
)
data class Channel(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val logo: String?,
    val group: String?,
    val url: String,
    val isFavourite: Boolean = false,
    val playlistId: Long = 0
): Parcelable
