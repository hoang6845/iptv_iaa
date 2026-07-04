package com.iptvplayer.m3u.stream.model.entity

import androidx.annotation.Keep
import androidx.room.Entity
import androidx.room.Index
import com.google.gson.annotations.SerializedName
@Keep
@Entity(
    primaryKeys = ["num", "server", "categoryId"],
    indices = [Index(value = ["uniqueId", "server"])]
)
data class LiveXtream(
    val num: Int,
    val name: String,
    @SerializedName("stream_id")
    val streamId: Int,
    @SerializedName("stream_icon")
    val streamIcon: String,
    @SerializedName("category_id")
    val categoryId: String,
    val server: Int,
    val uniqueId: String
) {
    fun generateUniqueId() = "$num-$categoryId-live"
}