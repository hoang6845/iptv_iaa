package com.iptvplayer.m3u.stream.model.dao

import androidx.room.*
import com.iptvplayer.m3u.stream.model.entity.RecentChannel
import kotlinx.coroutines.flow.Flow

@Dao
interface RecentChannelDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecent(channel: RecentChannel)

    @Query("""
        SELECT * FROM recentchannel
        ORDER BY lastWatchTime DESC
    """)
    fun getRecentChannels(): Flow<List<RecentChannel>>

    @Query("DELETE FROM recentchannel WHERE channelId = :channelId")
    suspend fun deleteRecent(channelId: String)

    @Query("DELETE FROM recentchannel")
    suspend fun clearRecent()

    @Query("SELECT `group` FROM recentchannel WHERE `group` IS NOT NULL AND `group` != ''")
    suspend fun getAllGroupRaw(): List<String>
}