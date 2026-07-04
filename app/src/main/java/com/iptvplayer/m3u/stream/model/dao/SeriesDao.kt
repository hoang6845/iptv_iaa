package com.iptvplayer.m3u.stream.model.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.iptvplayer.m3u.stream.model.entity.SearchFtsEntity
import com.iptvplayer.m3u.stream.model.entity.Series

@Dao
interface SeriesDao {
    @Query("Select * from series")
    suspend fun getAll(): List<Series>

    @Query("Select * from series where seriesId=:id and server=:serverId")
    suspend fun getOne(id: Long, serverId: Int): Series?

    @Insert
    suspend fun insertOne(movie: Series)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(series: List<Series>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSearchAll(entities: List<SearchFtsEntity>)

    @Transaction
    suspend fun insertChunkWithSearch(series: List<Series>, searchEntities: List<SearchFtsEntity>) {
        insertAll(series)
        insertSearchAll(searchEntities)
    }

    @Upsert
    suspend fun upsertOne(movie: Series)

    @Query("Select * from series where categoryId=:categoryId and server=:server")
    suspend fun selectSeriesByCategoryAndServer(categoryId: Int, server:Int): List<Series>

    @Query("Select * from series where server = :serverId and uniqueId = :uniqueId")
    suspend fun getOneS(serverId: Int, uniqueId: String): Series?
}