package com.iptvplayer.m3u.stream.model.entity

import android.os.Parcelable
import androidx.annotation.Keep
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Keep
@Parcelize
@Entity
data class ChannelPopular(
    @PrimaryKey
    val id: String,
    val name: String,
    val logo: String?,
    val group: String?,
    val url: String,
    val isFavourite: Boolean = false,
    val playlistId: String?= null
): Parcelable
