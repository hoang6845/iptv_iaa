package com.iptvplayer.m3u.stream.model.entity

data class SeasonWithEpisodes(
    val season: SeasonEntity,
    val episodes: List<EpisodeEntity>
)