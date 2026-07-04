package com.iptvplayer.m3u.stream.model.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.iptvplayer.m3u.stream.model.entity.XtreamAuth
import kotlinx.coroutines.flow.Flow

@Dao
interface ServerDao {
    @Query("Select * from xtreamauth")
    fun getAll(): Flow<List<XtreamAuth>>

    @Insert
    suspend fun insertOne(xtreamAuth: XtreamAuth): Long

    @Insert
    suspend fun insertAll(listXtreamAuth: List<XtreamAuth>)

    @Query("Select * from xtreamauth where id =:id")
    suspend fun getServerById(id: Int): XtreamAuth?

    @Query("Select name from xtreamauth where id=:id")
    suspend fun getNameXtream(id:Int): String?

    @Query("Update xtreamauth set urlAvatar = :avatarUrl where id = :serverId")
    suspend fun updateAvatar(avatarUrl: String, serverId: Int)

    @Query(
        """
        UPDATE xtreamauth
        SET name             = :name,
            username         = :username,
            password         = :password,
            server           = :server,
            isEnablePasscode = :isEnablePasscode,
            urlAvatar = :avatar
        WHERE id = :id
        """
    )
    suspend fun updateProfile(
        id: Int,
        name: String,
        username: String,
        password: String,
        server: String,
        isEnablePasscode: Boolean,
        avatar: String
    )

    @Query("DELETE FROM xtreamauth WHERE id = :id")
    suspend fun deleteById(id: Int)

    @Query("SELECT COUNT(*) FROM xtreamauth WHERE username = :username AND server = :server")
    suspend fun countByUsernameAndServer(username: String, server: String): Int
}