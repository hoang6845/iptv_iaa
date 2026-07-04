package com.iptvplayer.m3u.stream.model.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.iptvplayer.m3u.stream.model.entity.Channel
import com.iptvplayer.m3u.stream.model.entity.PlaylistEntity
import com.iptvplayer.m3u.stream.model.entity.PlaylistWithChannels
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaylistDao {
    @Update
    suspend fun updatePlaylist(playlist: PlaylistEntity)

    @Query("DELETE FROM channel WHERE playlistId = :playlistId")
    suspend fun deleteChannelsByPlaylistId(playlistId: Long)

    @Transaction
    suspend fun updatePlaylistWithChannels(playlist: PlaylistEntity, channels: List<Channel>) {
        updatePlaylist(playlist)
        deleteChannelsByPlaylistId(playlist.id)
        insertChannels(channels.map { it.copy(playlistId = playlist.id) })
    }

    @Transaction
    suspend fun updateChannels(playlistId: Long, channels: List<Channel>) {
        deleteChannelsByPlaylistId(playlistId)
        insertChannels(channels.map { it.copy(playlistId = playlistId) })
    }

    @Query("UPDATE playlist SET name = :name WHERE id = :id")
    suspend fun updatePlaylistName(id: Long, name: String)

    @Query("SELECT * FROM playlist")
    suspend fun getPlaylists(): List<PlaylistEntity>

    @Query("SELECT * FROM playlist where id = :id")
    suspend fun getOnePlaylist(id: Int): PlaylistWithChannels

    @Query("DELETE FROM playlist WHERE id = :id")
    suspend fun deletePlaylist(id: Long)

    @Transaction
    @Query("SELECT * FROM playlist")
    fun getItems(): Flow<List<PlaylistWithChannels>>

    @Query("SELECT * FROM playlist where id = :id")
    fun getItems(id: Long): Flow<PlaylistWithChannels>

    @Delete
    suspend fun deletePlaylist(playlist: PlaylistEntity)

    @Query("Select * from playlist where typePlayList = :type limit 1")
    suspend fun getPlaylistByType(type: String): PlaylistWithChannels?


    @Query("""
        UPDATE playlist 
        SET isPasscodeEnabled = :isPasscodeEnabled 
        WHERE id = :playlistId
    """)
    suspend fun togglePasscode(playlistId: Long, isPasscodeEnabled: Boolean): Int

    @Query("DELETE FROM playlist WHERE id = :playlistId")
    suspend fun deleteById(playlistId: Long): Int

    @Transaction
    @Query("SELECT * FROM playlist WHERE id = :playlistId")
    fun getPlaylistWithChannels(playlistId: Long): Flow<PlaylistWithChannels>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: PlaylistEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChannels(channels: List<Channel>)

    @Transaction
    suspend fun insertPlaylistWithChannels(playlist: PlaylistEntity, channels: List<Channel>) {
        val playlistId = insertPlaylist(playlist)
        insertChannels(channels.map { it.copy(playlistId = playlistId) })
    }

    @Query("UPDATE channel SET isFavourite = :isFavourite WHERE id = :channelId and playlistId = :playlistId")
    suspend fun updateFavourite(channelId: String, isFavourite: Boolean, playlistId: Long)

    @Query("SELECT * FROM channel WHERE id = :channelId LIMIT 1")
    suspend fun getChannelById(channelId: String): Channel?

    @Query("SELECT EXISTS(SELECT 1 FROM playlist WHERE url = :url)")
    suspend fun isAlreadyExists(url: String): Boolean
}