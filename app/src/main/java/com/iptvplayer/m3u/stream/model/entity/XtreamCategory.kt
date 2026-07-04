package com.iptvplayer.m3u.stream.model.entity

import androidx.annotation.Keep
import androidx.room.Entity
import com.google.gson.annotations.SerializedName

@Keep
@Entity(
    primaryKeys = ["serverId", "categoryId"]
)
data class XtreamCategory(
    @SerializedName("category_id")
    val categoryId: Int,
    @SerializedName("category_name")
    val categoryName: String,
    @SerializedName("parent_id")
    val parentId: Int? = null,
    val serverId: Int,
    val type: String
) {
}

sealed class CategoryMoviesState {
    object NotLoaded : CategoryMoviesState()
    object Loading : CategoryMoviesState()
    data class Loaded(val movies: List<Movie>) : CategoryMoviesState()
    data class LoadedSeries(val series: List<Series>) : CategoryMoviesState()
    data class LoadedLive(val lives: List<LiveXtream>, val canLoadMore: Boolean = false, val isLoadingMore: Boolean = false ) : CategoryMoviesState()
    data class Error(val message: String) : CategoryMoviesState()
}
enum class SyncState {
    NOT_SYNCED,
    SYNCING,
    SYNCED,
    ERROR
}