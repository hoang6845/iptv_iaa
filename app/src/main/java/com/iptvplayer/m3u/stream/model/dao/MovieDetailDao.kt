package com.iptvplayer.m3u.stream.model.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy.Companion.REPLACE
import androidx.room.Query
import com.iptvplayer.m3u.stream.model.entity.MovieDetailEntity

@Dao
interface MovieDetailDao {
    @Insert(onConflict = REPLACE)
    suspend fun insertOne(movieDetailEntity: MovieDetailEntity)

    @Query("Select * from moviedetail where streamId = :streamId")
    suspend fun findOne(streamId: Long): MovieDetailEntity?

    @Query("SELECT backdropPath FROM moviedetail WHERE streamId = :streamId AND server = :serverId")
    suspend fun getMovieBackdrop(streamId: Long, serverId: Int): List<String>?
}