package com.iptvplayer.m3u.stream.model.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.iptvplayer.m3u.stream.model.entity.Channel
import com.iptvplayer.m3u.stream.model.entity.ChannelPopular
import kotlinx.coroutines.flow.Flow

@Dao
interface ChannelPopularDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChannels(channels: List<ChannelPopular>)

    @Query("SELECT * FROM channelpopular WHERE playlistId = :playlistId")
    fun getChannelsByPlaylist(playlistId: Long): Flow<List<ChannelPopular>>

    @Query("SELECT * FROM channelpopular")
    fun getChannels(): Flow<List<ChannelPopular>>

    @Query("SELECT * FROM channelpopular WHERE `group` = :type COLLATE NOCASE")
    fun getChannelsByType(type: String): Flow<List<ChannelPopular>>

    @Query("UPDATE channelpopular SET isFavourite = :isFavourite WHERE id = :id")
    suspend fun updateFavourite(id: String, isFavourite: Boolean)

    @Query("SELECT `group` FROM Channel WHERE `group` IS NOT NULL AND `group` != ''")
    suspend fun getAllGroupRaw(): List<String>

    @Query(
        """
    SELECT * FROM channelpopular
    WHERE (:query = '' OR name LIKE '%' || :query || '%' COLLATE NOCASE)
    AND (:group = '' OR :group = 'all' OR `group` = :group COLLATE NOCASE)
    ORDER BY name ASC
    """
    )
    fun getChannelsPaged(
        query: String,
        group: String
    ): PagingSource<Int, ChannelPopular>

    @Query(
        """
              SELECT cp.*
        FROM ChannelPopular cp
        JOIN (
            SELECT url, MIN(id) as min_id
            FROM ChannelPopular
            GROUP BY url
        ) t ON cp.id = t.min_id
        JOIN Channel c ON cp.url = c.url
        WHERE (:query = '' OR cp.name LIKE '%' || :query || '%' COLLATE NOCASE)
          AND (:group = '' OR :group = 'all' OR cp.`group` = :group COLLATE NOCASE)
        ORDER BY cp.name ASC
"""
    )
    fun getChannelsPagedQuery(
        query: String,
        group: String
    ): PagingSource<Int, ChannelPopular>

    @Query(
        """
    SELECT *
    FROM Channel
    WHERE (:query = '' OR name LIKE '%' || :query || '%' COLLATE NOCASE)
    AND (
        :group = '' OR :group = 'all'
        OR ';' || `group` || ';' LIKE '%;' || :group || ';%' COLLATE NOCASE
    )
    ORDER BY name ASC
"""
    )
    fun getChannelsPagedQueryOnly(
        query: String,
        group: String
    ): PagingSource<Int, Channel>
}