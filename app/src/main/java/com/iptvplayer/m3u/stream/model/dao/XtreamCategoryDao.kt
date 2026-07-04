package com.iptvplayer.m3u.stream.model.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.iptvplayer.m3u.stream.model.entity.XtreamCategory

@Dao
interface XtreamCategoryDao {
    @Query("Select * from xtreamcategory")
    suspend fun getAll(): List<XtreamCategory>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOne(xtreamCategory: XtreamCategory)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(listCategory: List<XtreamCategory>)

    @Query("Select * from xtreamcategory where serverId = :serverId and type != :type order by rowid ASC")
    suspend fun selectByServerId(serverId: Int, type: String = "live"): List<XtreamCategory>

    @Query("Select * from xtreamcategory where serverId = :serverId and type = :type order by rowid ASC")
    suspend fun selectByServerIdLive(serverId: Int, type: String = "live"): List<XtreamCategory>
}
