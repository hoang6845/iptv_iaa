package com.iptvplayer.m3u.stream.model.entity

data class CarouselItem(
    val movie: Movie,
    val backdropUrl: String?  // ảnh đầu tiên từ backdropPath
)