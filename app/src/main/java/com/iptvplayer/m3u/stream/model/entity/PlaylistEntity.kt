package com.iptvplayer.m3u.stream.model.entity

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize


@Entity(tableName = "playlist")
@Parcelize
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val typePlayList: String,
    val url: String?= null,
    var isPasscodeEnabled: Boolean = false
) : Parcelable