package com.iptvplayer.m3u.stream.model.dao

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.iptvplayer.m3u.stream.model.converter.Converter
import com.iptvplayer.m3u.stream.model.entity.Channel
import com.iptvplayer.m3u.stream.model.entity.ChannelPopular
import com.iptvplayer.m3u.stream.model.entity.Favourite
import com.iptvplayer.m3u.stream.model.entity.FavouriteChannel
import com.iptvplayer.m3u.stream.model.entity.LiveXtream
import com.iptvplayer.m3u.stream.model.entity.Movie
import com.iptvplayer.m3u.stream.model.entity.MovieDetailEntity
import com.iptvplayer.m3u.stream.model.entity.PlaylistEntity
import com.iptvplayer.m3u.stream.model.entity.RecentChannel
import com.iptvplayer.m3u.stream.model.entity.SearchFtsEntity
import com.iptvplayer.m3u.stream.model.entity.Series
import com.iptvplayer.m3u.stream.model.entity.XtreamAuth
import com.iptvplayer.m3u.stream.model.entity.XtreamCategory

@Database(entities = [Movie::class, XtreamCategory::class, XtreamAuth::class, MovieDetailEntity::class, Series::class, ChannelPopular::class, LiveXtream::class, SearchFtsEntity::class, Favourite::class, PlaylistEntity::class, FavouriteChannel::class, RecentChannel::class, Channel::class], version = 1)
@TypeConverters(value = [Converter::class])
abstract class AppDatabase: RoomDatabase() {
    abstract fun movieDao(): MovieDao
    abstract fun serverDao(): ServerDao
    abstract fun xtreamCategoryDao(): XtreamCategoryDao
    abstract fun movieDetailDao():  MovieDetailDao
    abstract fun seriesDao(): SeriesDao
    abstract fun liveXtreamDao(): LiveXtreamDao
    abstract fun SearchDao(): SearchDao
    abstract fun favouriteDao(): FavouriteDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun favouriteChannelDao(): FavouriteChannelDao
    abstract fun channelPopularDao(): ChannelPopularDao
    abstract fun recentChannelDao(): RecentChannelDao
}