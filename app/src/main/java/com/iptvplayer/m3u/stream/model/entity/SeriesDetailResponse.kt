package com.iptvplayer.m3u.stream.model.entity

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

@Keep
data class SeriesDetailResponse(
    @SerializedName("seasons")
    val seasons: List<Season>,
    @SerializedName("episodes")
    val episodes: Map<String, List<Episode>>
)
@Keep
data class Season(
    @SerializedName("season_number")
    val seasonNumber: Int,
    @SerializedName("name")
    val name: String,
    @SerializedName("air_date")
    val airDate: String?,
    @SerializedName("episode_count")
    val episodeCount: Int,
    @SerializedName("vote_average")
    val voteAverage: Double?,
    @SerializedName("cover")
    val cover: String?
)

@Keep
data class Episode(
    @SerializedName("id")
    val id: String,
    @SerializedName("episode_num")
    val episodeNum: Int,
    @SerializedName("title")
    val title: String,
    @SerializedName("season")
    val season: Int,
    @SerializedName("info")
    val info: EpisodeInfo?,
    @SerializedName("container_extension")
    val containerExtension: String
)
@Keep
data class EpisodeInfo(
    @SerializedName("plot")
    val plot: String?,
    @SerializedName("duration")
    val duration: String?,
    @SerializedName("movie_image")
    val movieImage: String?,
    @SerializedName("rating")
    val rating: String?
)