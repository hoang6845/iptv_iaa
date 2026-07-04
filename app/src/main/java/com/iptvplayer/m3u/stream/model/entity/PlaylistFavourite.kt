package com.iptvplayer.m3u.stream.model.entity

data class PlaylistFavourite(
    val name: String,
    val channels: List<FavouriteChannel>,
    val typePlayList: String
)