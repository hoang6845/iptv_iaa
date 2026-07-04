package com.iptvplayer.m3u.stream.model.entity

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Entity
@Parcelize
data class FavouriteChannel(
    @PrimaryKey
    val id: String,
    val name: String,
    val logo: String?,
    val group: String?,
    val url: String,
    val playlistId: Long,
    val isFavourite: Boolean = true
) : Parcelable