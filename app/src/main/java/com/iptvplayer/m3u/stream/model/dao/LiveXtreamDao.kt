package com.iptvplayer.m3u.stream.model.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy.Companion.REPLACE
import androidx.room.Query
import com.iptvplayer.m3u.stream.model.entity.LiveXtream

@Dao
interface LiveXtreamDao {
    @Query("Select * from liveXtream where server=:server")
    suspend fun getAll(server: Int): List<LiveXtream>

    @Query("SELECT * FROM liveXtream WHERE server = :server LIMIT :limit OFFSET :offset")
    suspend fun getPaged(server: Int, limit: Int, offset: Int): List<LiveXtream>

    @Query(
        """SELECT * FROM liveXtream WHERE (:query = '' OR name LIKE '%' || :query || '%' COLLATE NOCASE)
        and server = :server and categoryId = :categoryId"""
    )
    fun getPagedQuery(
        server: Int, query: String, categoryId: Int
    ): PagingSource<Int, LiveXtream>

    @Query("SELECT COUNT(*) FROM liveXtream WHERE server = :server")
    suspend fun getCount(server: Int): Int

    @Query("SELECT EXISTS(SELECT 1 FROM liveXtream WHERE server = :serverId)")
    suspend fun hasData(serverId: Int): Boolean

    @Insert(onConflict = REPLACE)
    suspend fun insertAll(listMovie: List<LiveXtream>)

    @Query("SELECT * FROM livextream WHERE server = :serverId")
    fun getLiveXtreamPaged(serverId: Int): PagingSource<Int, LiveXtream>

    @Query("SELECT * FROM livextream WHERE server = :serverId and name LIKE '%' || :query || '%'")
    fun getLiveXtreamPagedBySearch(serverId: Int, query: String): PagingSource<Int, LiveXtream>

    @Query("Select * from livextream where server = :serverId and uniqueId = :uniqueId")
    suspend fun getOne(serverId: Int, uniqueId: String): LiveXtream?

    @Query("""
    SELECT * FROM livextream
    WHERE server = :server
      AND (:categoryId IS NULL OR categoryId = :categoryId)
      AND (:query = '' OR name LIKE '%' || :query || '%')
    ORDER BY
        CASE WHEN :sort = 'az' THEN name END ASC,
        CASE WHEN :sort = 'za' THEN name END DESC,
        CASE WHEN :sort = 'none' THEN num END ASC
""")
    fun getPagedQueryWSort(
        server: Int,
        query: String,
        categoryId: Int?,
        sort: String
    ): PagingSource<Int, LiveXtream>
}