package com.iptvplayer.m3u.stream.model.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.iptvplayer.m3u.stream.model.entity.FavouriteChannel
import kotlinx.coroutines.flow.Flow

@Dao
interface FavouriteChannelDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFavouriteChannel(favourite: FavouriteChannel)

    @Delete
    suspend fun removeFavouriteChannel(favourite: FavouriteChannel)

    @Query("SELECT * FROM favouritechannel")
    fun getAllFavouriteChannels(): Flow<List<FavouriteChannel>>
    @Query("SELECT * FROM favouritechannel")
    suspend fun getAllFavouriteChannelsSuspend(): List<FavouriteChannel>
    @Query(
        """
        SELECT EXISTS(
            SELECT 1 FROM favouritechannel
            WHERE id =:channelId and playlistId =:playlistId
        )
    """
    )
    suspend fun isFavourite(channelId: String, playlistId: Long): Boolean
}