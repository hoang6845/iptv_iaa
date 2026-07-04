package com.iptvplayer.m3u.stream.model.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.iptvplayer.m3u.stream.model.entity.Movie
import com.iptvplayer.m3u.stream.model.entity.SearchFtsEntity

@Dao
interface MovieDao {
    @Query("Select * from movie")
    suspend fun getAll(): List<Movie>

    @Insert
    suspend fun insertOne(movie: Movie)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(movies: List<Movie>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSearchAll(entities: List<SearchFtsEntity>)

    @Transaction
    suspend fun insertChunkWithSearch(movies: List<Movie>, searchEntities: List<SearchFtsEntity>) {
        insertAll(movies)
        insertSearchAll(searchEntities)
    }

    @Upsert
    suspend fun upsertOne(movie: Movie)

    @Query("Select * from movie where categoryId=:categoryId and server=:server and streamIcon is not null and streamId is not null order by num DESC")
    suspend fun selectMovieByCategoryAndServer(categoryId: Int, server:Int): List<Movie>

    @Query("""
        SELECT *
        FROM movie
        WHERE server = :server
          AND streamIcon IS NOT NULL
          AND streamId IS NOT NULL
        ORDER BY RANDOM()
        LIMIT 8
    """)
    suspend fun selectRandomMovies(server: Int): List<Movie>

    @Query("Select * from movie where server=:server and streamIcon is not null and streamId is not null ORDER BY RANDOM() LIMIT 1")
    suspend fun selectOneMovieRandom(server:Int): Movie

    @Query("SELECT COUNT(*) FROM movie WHERE categoryId = :categoryId AND server = :serverId")
    suspend fun countByCategoryAndServer(categoryId: Int, serverId: Int): Int

    @Query("Select * from movie where server = :serverId and uniqueId = :uniqueId")
    suspend fun getOne(serverId: Int, uniqueId: String): Movie?
}