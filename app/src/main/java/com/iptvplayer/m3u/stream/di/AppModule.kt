package com.iptvplayer.m3u.stream.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import com.iptvplayer.m3u.stream.model.dao.AppDatabase
import com.iptvplayer.m3u.stream.model.dao.ChannelPopularDao
import com.iptvplayer.m3u.stream.model.dao.FavouriteChannelDao
import com.iptvplayer.m3u.stream.model.dao.FavouriteDao
import com.iptvplayer.m3u.stream.model.dao.LiveXtreamDao
import com.iptvplayer.m3u.stream.model.dao.MovieDao
import com.iptvplayer.m3u.stream.model.dao.MovieDetailDao
import com.iptvplayer.m3u.stream.model.dao.PlaylistDao
import com.iptvplayer.m3u.stream.model.dao.RecentChannelDao
import com.iptvplayer.m3u.stream.model.dao.SearchDao
import com.iptvplayer.m3u.stream.model.dao.SeriesDao
import com.iptvplayer.m3u.stream.model.dao.ServerDao
import com.iptvplayer.m3u.stream.model.dao.XtreamCategoryDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "IPTV"
        )
            .addMigrations(MIGRATION_1_2)
            .setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
            .build()
    }

    @Provides
    fun provideMovieDao(
        database: AppDatabase
    ): MovieDao = database.movieDao()

    @Provides
    fun provideServerDao(
        database: AppDatabase
    ): ServerDao = database.serverDao()

    @Provides
    fun provideXtreamCategoryDao(
        database: AppDatabase
    ): XtreamCategoryDao = database.xtreamCategoryDao()

    @Provides
    fun provideMovieDetailDao(
        database: AppDatabase
    ): MovieDetailDao = database.movieDetailDao()


    @Provides
    fun provideSeriesDao(
        database: AppDatabase
    ): SeriesDao = database.seriesDao()

    @Provides
    fun provideLiveXtreamDao(
        database: AppDatabase
    ): LiveXtreamDao = database.liveXtreamDao()

    @Provides
    fun provideSearchDao(
        database: AppDatabase
    ): SearchDao = database.SearchDao()

    @Provides
    fun provideFavouriteDao(
        database: AppDatabase
    ): FavouriteDao = database.favouriteDao()

    @Provides
    fun providePlaylistDao(
        database: AppDatabase
    ): PlaylistDao = database.playlistDao()

    @Provides
    fun provideFavouriteChannelDao(
        database: AppDatabase
    ): FavouriteChannelDao = database.favouriteChannelDao()

    @Provides
    fun provideChannelPopularDao(
        database: AppDatabase
    ): ChannelPopularDao = database.channelPopularDao()

    @Provides
    fun provideRecentChannelDao(
        database: AppDatabase
    ): RecentChannelDao = database.recentChannelDao()
}
