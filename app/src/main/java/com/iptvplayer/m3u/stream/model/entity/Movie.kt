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
data class Movie(
    val num: Int,
    val name: String,
    @SerializedName("stream_type")
    val streamType: String?,
    @SerializedName("stream_id")
    val streamId: Long?,
    @SerializedName("stream_icon")
    val streamIcon: String?,
    val rating: String?,
    @SerializedName("rating_5based")
    val rating5Base: Double?,
    val added: Long?,
    @SerializedName("category_id")
    val categoryId: Int,
    @SerializedName("container_extension")
    val containerExtension: String?,
    val server: Int,
    val uniqueId: String?= null
) {
    fun generateUniqueId() = "$num-$categoryId-movie"
}