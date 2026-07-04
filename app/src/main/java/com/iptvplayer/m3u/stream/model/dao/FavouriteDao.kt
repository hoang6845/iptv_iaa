package com.iptvplayer.m3u.stream.model.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.iptvplayer.m3u.stream.model.entity.Favourite
import com.iptvplayer.m3u.stream.model.entity.SearchResult
import kotlinx.coroutines.flow.Flow

@Dao
interface FavouriteDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFavourite(favourite: Favourite)

    @Delete
    suspend fun removeFavourite(favourite: Favourite)

    @Query(
        """
        DELETE FROM favourites
        WHERE server = :server AND uniqueId = :uniqueId
    """
    )
    suspend fun removeFavourite(server: Int, uniqueId: String)

    @Query(
        """
        SELECT EXISTS(
            SELECT 1 FROM favourites
            WHERE server = :server AND uniqueId = :uniqueId
        )
    """
    )
    suspend fun isFavourite(server: Int, uniqueId: String): Boolean

    @Query("SELECT * FROM favourites")
    fun getAllFavourites(): Flow<List<Favourite>>

    @Query(
        """
    SELECT m.uniqueId as uniqueId, m.name as name, m.streamIcon as image, 'movie' AS type
    FROM movie m
    INNER JOIN favourites f
    ON m.uniqueId = f.uniqueId
    AND m.server = f.server
"""
    )
    fun getFavouriteMovies(): Flow<List<SearchResult>>

    @Query("""
    SELECT s.uniqueId as uniqueId, s.name as name, s.cover as image, 'series' AS type
    FROM series s
    INNER JOIN favourites f
    ON s.uniqueId = f.uniqueId
    AND s.server = f.server
""")
    fun getFavouriteSeries(): Flow<List<SearchResult>>

    @Query("""
    SELECT l.uniqueId as uniqueId, l.name as name, l.streamIcon as image, 'live' AS type
    FROM liveXtream l
    INNER JOIN favourites f
    ON l.uniqueId = f.uniqueId
    AND l.server = f.server
""")
    fun getFavouriteLives(): Flow<List<SearchResult>>
}