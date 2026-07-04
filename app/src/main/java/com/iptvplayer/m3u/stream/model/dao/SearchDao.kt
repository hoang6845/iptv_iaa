package com.iptvplayer.m3u.stream.model.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.iptvplayer.m3u.stream.model.entity.SearchFtsEntity
import com.iptvplayer.m3u.stream.model.entity.SearchResult

@Dao
interface SearchDao {

    @Query(
        """
    SELECT m.uniqueId as uniqueId,
           m.name as name,
           m.streamIcon as image,
           'movie' as type
    FROM search_fts s
    JOIN movie m ON m.uniqueId = s.uniqueId
    WHERE s.name MATCH :query AND s.type = 'movie' and m.server = :serverId


    UNION ALL

    SELECT se.uniqueId as uniqueId,
           se.name as name,
           se.cover as image,
           'series' as type
    FROM search_fts s
    JOIN series se ON se.uniqueId = s.uniqueId
    WHERE s.name MATCH :query AND s.type = 'series' and se.server = :serverId
    
    """
    )
    fun searchPagingMovie(serverId: Int, query: String): PagingSource<Int, SearchResult>

    @Query(
        """
    SELECT m.uniqueId as uniqueId,
           m.name as name,
           m.streamIcon as image,
           'movie' as type
    FROM search_fts s
    JOIN movie m ON m.uniqueId = s.uniqueId
    WHERE  s.type = 'movie' and m.server = :serverId


    UNION ALL

    SELECT se.uniqueId as uniqueId,
           se.name as name,
           se.cover as image,
           'series' as type
    FROM search_fts s
    JOIN series se ON se.uniqueId = s.uniqueId
    WHERE  s.type = 'series' and se.server = :serverId
    
    """
    )
    fun searchAllPagingMovie(serverId: Int): PagingSource<Int, SearchResult>


    @Query("""
    SELECT m.uniqueId, m.name, m.streamIcon as image, 'movie' as type
    FROM search_fts s
    JOIN movie m ON m.uniqueId = s.uniqueId
    WHERE s.name MATCH :query AND s.type = 'movie' AND m.server = :serverId AND m.categoryId = :categoryId
""")
    fun searchMovieByCategory(serverId: Int, categoryId: Int, query: String): PagingSource<Int, SearchResult>

    @Query("""
    SELECT m.uniqueId, m.name, m.streamIcon as image, 'movie' as type
    FROM search_fts s
    JOIN movie m ON m.uniqueId = s.uniqueId
    WHERE s.type = 'movie' AND m.server = :serverId AND m.categoryId = :categoryId
""")
    fun searchAllMovieByCategory(serverId: Int, categoryId: Int): PagingSource<Int, SearchResult>

    @Query("""
    SELECT s.uniqueId, s.name, s.cover as image, 'series' as type
    FROM search_fts sf
    JOIN series s ON s.uniqueId = sf.uniqueId
    WHERE sf.name MATCH :query AND sf.type = 'movie' AND s.server = :serverId AND s.categoryId = :categoryId
""")
    fun searchSeriesByCategory(serverId: Int, categoryId: Int, query: String): PagingSource<Int, SearchResult>

    @Query("""
    SELECT s.uniqueId, s.name, s.cover as image, 'series' as type
    FROM search_fts sf
    JOIN series s ON s.uniqueId = s.uniqueId
   WHERE sf.type = 'movie' AND s.server = :serverId AND s.categoryId = :categoryId
""")
    fun searchAllSeriesByCategory(serverId: Int, categoryId: Int): PagingSource<Int, SearchResult>

    @Query("""
    SELECT l.uniqueId as uniqueId,
           l.name as name,
           l.streamIcon as image,
           'live' as type
    FROM search_fts s
    JOIN livextream l ON l.uniqueId = s.uniqueId
    WHERE  s.type = 'live' and l.server = :serverId
    """)
    fun searchAllPagingLive(serverId: Int): PagingSource<Int, SearchResult>

    @Query("""
    SELECT l.uniqueId as uniqueId,
           l.name as name,
           l.streamIcon as image,
           'live' as type
    FROM search_fts s
    JOIN livextream l ON l.uniqueId = s.uniqueId
    WHERE s.name MATCH :query and s.type = 'live' and l.server = :serverId
    """)
    fun searchPagingLive(serverId: Int, query: String): PagingSource<Int, SearchResult>

    @Query(
        """
    SELECT m.uniqueId as uniqueId,
           m.name as name,
           m.streamIcon as image,
           :type as type
    FROM search_fts s
    JOIN movie m ON m.uniqueId = s.uniqueId
    WHERE s.name MATCH :query AND s.type = :type and m.server = :serverId
    """
    )
    fun searchPagingMovieByType(serverId: Int, query: String, type: String): PagingSource<Int, SearchResult>

    @Query(
        """
    SELECT m.uniqueId,
           m.name,
           m.streamIcon as image,
           'movie' as type
    FROM search_fts s
    JOIN movie m ON m.uniqueId = s.uniqueId
    WHERE s.name MATCH :query
      AND s.type = 'movie' 
      AND m.server = :serverId
    """
    )
    fun searchMovie(serverId: Int, query: String): PagingSource<Int, SearchResult>

    @Query(
        """
    SELECT m.uniqueId,
           m.name,
           m.streamIcon as image,
           'movie' as type
    FROM search_fts s
    JOIN movie m ON m.uniqueId = s.uniqueId
    WHERE  s.type = 'movie' 
      AND m.server = :serverId
    """
    )
    fun searchAllMovie(serverId: Int): PagingSource<Int, SearchResult>

    @Query(
        """
    SELECT se.uniqueId as uniqueId,
           se.name as name,
           se.cover as image,
           'series' as type
    FROM search_fts s
    JOIN series se ON se.uniqueId = s.uniqueId
    WHERE s.name MATCH :query
      AND s.type = 'series' 
      AND se.server = :serverId
    """
    )
    fun searchSeries(serverId: Int, query: String): PagingSource<Int, SearchResult>

    @Query(
        """
    SELECT se.uniqueId as uniqueId,
           se.name as name,
           se.cover as image,
           'series' as type
    FROM search_fts s
    JOIN series se ON se.uniqueId = s.uniqueId
    WHERE s.type = 'series' 
      AND se.server = :serverId
    """
    )
    fun searchAllSeries(serverId: Int): PagingSource<Int, SearchResult>

    @Query("""
    SELECT m.uniqueId,
           m.name,
           m.streamIcon as image,
           'movie' as type
    FROM search_fts sf
    INNER JOIN movie m ON sf.uniqueId = m.uniqueId
    WHERE sf.type = 'movie'
      AND m.server = :serverId
      AND m.categoryId IN (:categoryIds)
    ORDER BY m.categoryId
""")
    fun searchAllMovieByCategories(
        serverId: Int,
        categoryIds: List<Int>
    ): PagingSource<Int, SearchResult>

    @Query("""
  SELECT m.uniqueId,
           m.name,
           m.streamIcon as image,
           'movie' as type
    FROM search_fts sf
    INNER JOIN movie m ON sf.uniqueId = m.uniqueId
    WHERE sf.type = 'movie'
      AND m.server = :serverId
      AND m.categoryId IN (:categoryIds)
      AND search_fts MATCH :query
    ORDER BY m.categoryId
""")
    fun searchMovieByCategories(
        serverId: Int,
        categoryIds: List<Int>,
        query: String
    ): PagingSource<Int, SearchResult>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<SearchFtsEntity>)

    @Query("DELETE FROM search_fts")
    suspend fun clear()

    @Query("""
    SELECT * FROM (
        SELECT m.uniqueId as uniqueId, m.name as name, m.streamIcon as image, 'movie' as type
        FROM search_fts s JOIN movie m ON m.uniqueId = s.uniqueId
        WHERE s.name MATCH :query AND s.type = 'movie' AND m.server = :serverId
        UNION ALL
        SELECT se.uniqueId, se.name, se.cover as image, 'series' as type
        FROM search_fts s JOIN series se ON se.uniqueId = s.uniqueId
        WHERE s.name MATCH :query AND s.type = 'series' AND se.server = :serverId
    )
    ORDER BY
        CASE WHEN :sort = 'az' THEN name END ASC,
        CASE WHEN :sort = 'za' THEN name END DESC
""")
    fun searchPagingMovieWSort(serverId: Int, query: String, sort: String): PagingSource<Int, SearchResult>

    @Query("""
    SELECT * FROM (
        SELECT m.uniqueId as uniqueId, m.name as name, m.streamIcon as image, 'movie' as type
        FROM search_fts s JOIN movie m ON m.uniqueId = s.uniqueId
        WHERE s.type = 'movie' AND m.server = :serverId
        UNION ALL
        SELECT se.uniqueId, se.name, se.cover as image, 'series' as type
        FROM search_fts s JOIN series se ON se.uniqueId = s.uniqueId
        WHERE s.type = 'series' AND se.server = :serverId
    )
    ORDER BY
        CASE WHEN :sort = 'az' THEN name END ASC,
        CASE WHEN :sort = 'za' THEN name END DESC
""")
    fun searchAllPagingMovieWSort(serverId: Int, sort: String): PagingSource<Int, SearchResult>

    @Query("""
    SELECT * FROM (
        SELECT m.uniqueId as uniqueId, m.name as name, m.streamIcon as image, 'movie' as type
        FROM search_fts s JOIN movie m ON m.uniqueId = s.uniqueId
        WHERE s.name MATCH :query AND s.type = 'movie' AND m.server = :serverId
    )
    ORDER BY
        CASE WHEN :sort = 'az' THEN name END ASC,
        CASE WHEN :sort = 'za' THEN name END DESC
""")
    fun searchMovieWSort(serverId: Int, query: String, sort: String): PagingSource<Int, SearchResult>

    @Query("""
    SELECT * FROM (
        SELECT m.uniqueId as uniqueId, m.name as name, m.streamIcon as image, 'movie' as type
        FROM search_fts s JOIN movie m ON m.uniqueId = s.uniqueId
        WHERE s.type = 'movie' AND m.server = :serverId
    )
    ORDER BY
        CASE WHEN :sort = 'az' THEN name END ASC,
        CASE WHEN :sort = 'za' THEN name END DESC
""")
    fun searchAllMovieWSort(serverId: Int, sort: String): PagingSource<Int, SearchResult>

    @Query("""
    SELECT * FROM (
        SELECT se.uniqueId as uniqueId, se.name as name, se.cover as image, 'series' as type
        FROM search_fts s JOIN series se ON se.uniqueId = s.uniqueId
        WHERE s.name MATCH :query AND s.type = 'series' AND se.server = :serverId
    )
    ORDER BY
        CASE WHEN :sort = 'az' THEN name END ASC,
        CASE WHEN :sort = 'za' THEN name END DESC
""")
    fun searchSeriesWSort(serverId: Int, query: String, sort: String): PagingSource<Int, SearchResult>

    @Query("""
    SELECT * FROM (
        SELECT se.uniqueId as uniqueId, se.name as name, se.cover as image, 'series' as type
        FROM search_fts s JOIN series se ON se.uniqueId = s.uniqueId
        WHERE s.type = 'series' AND se.server = :serverId
    )
    ORDER BY
        CASE WHEN :sort = 'az' THEN name END ASC,
        CASE WHEN :sort = 'za' THEN name END DESC
""")
    fun searchAllSeriesWSort(serverId: Int, sort: String): PagingSource<Int, SearchResult>

    @Query("""
    SELECT * FROM (
        SELECT l.uniqueId as uniqueId, l.name as name, l.streamIcon as image, 'live' as type
        FROM search_fts s JOIN livextream l ON l.uniqueId = s.uniqueId
        WHERE s.name MATCH :query AND s.type = 'live' AND l.server = :serverId
    )
    ORDER BY
        CASE WHEN :sort = 'az' THEN name END ASC,
        CASE WHEN :sort = 'za' THEN name END DESC
""")
    fun searchPagingLiveWSort(serverId: Int, query: String, sort: String): PagingSource<Int, SearchResult>

    @Query("""
    SELECT * FROM (
        SELECT l.uniqueId as uniqueId, l.name as name, l.streamIcon as image, 'live' as type
        FROM search_fts s JOIN livextream l ON l.uniqueId = s.uniqueId
        WHERE s.type = 'live' AND l.server = :serverId
    )
    ORDER BY
        CASE WHEN :sort = 'az' THEN name END ASC,
        CASE WHEN :sort = 'za' THEN name END DESC
""")
    fun searchAllPagingLiveWSort(serverId: Int, sort: String): PagingSource<Int, SearchResult>

    @Query("""
    SELECT m.uniqueId, m.name, m.streamIcon as image, 'movie' as type
    FROM search_fts s JOIN movie m ON m.uniqueId = s.uniqueId
    WHERE s.name MATCH :query AND s.type = 'movie' AND m.server = :serverId AND m.categoryId = :categoryId
    ORDER BY
        CASE WHEN :sort = 'az' THEN m.name END ASC,
        CASE WHEN :sort = 'za' THEN m.name END DESC
""")
    fun searchMovieByCategoryWSort(serverId: Int, categoryId: Int, query: String, sort: String): PagingSource<Int, SearchResult>

    @Query("""
    SELECT m.uniqueId, m.name, m.streamIcon as image, 'movie' as type
    FROM search_fts s JOIN movie m ON m.uniqueId = s.uniqueId
    WHERE s.type = 'movie' AND m.server = :serverId AND m.categoryId = :categoryId
    ORDER BY
        CASE WHEN :sort = 'az' THEN m.name END ASC,
        CASE WHEN :sort = 'za' THEN m.name END DESC
""")
    fun searchAllMovieByCategoryWSort(serverId: Int, categoryId: Int, sort: String): PagingSource<Int, SearchResult>

    @Query("""
    SELECT s.uniqueId, s.name, s.cover as image, 'series' as type
    FROM search_fts sf JOIN series s ON s.uniqueId = sf.uniqueId
    WHERE sf.name MATCH :query AND sf.type = 'series' AND s.server = :serverId AND s.categoryId = :categoryId
    ORDER BY
        CASE WHEN :sort = 'az' THEN s.name END ASC,
        CASE WHEN :sort = 'za' THEN s.name END DESC
""")
    fun searchSeriesByCategoryWSort(serverId: Int, categoryId: Int, query: String, sort: String): PagingSource<Int, SearchResult>

    @Query("""
    SELECT s.uniqueId, s.name, s.cover as image, 'series' as type
    FROM search_fts sf JOIN series s ON s.uniqueId = sf.uniqueId
    WHERE sf.type = 'series' AND s.server = :serverId AND s.categoryId = :categoryId
    ORDER BY
        CASE WHEN :sort = 'az' THEN s.name END ASC,
        CASE WHEN :sort = 'za' THEN s.name END DESC
""")
    fun searchAllSeriesByCategoryWSort(serverId: Int, categoryId: Int, sort: String): PagingSource<Int, SearchResult>

    @Query("""
    SELECT m.uniqueId, m.name, m.streamIcon as image, 'movie' as type
    FROM search_fts sf INNER JOIN movie m ON sf.uniqueId = m.uniqueId
    WHERE sf.type = 'movie' AND m.server = :serverId AND m.categoryId IN (:categoryIds)
    ORDER BY
        CASE WHEN :sort = 'az' THEN m.name END ASC,
        CASE WHEN :sort = 'za' THEN m.name END DESC
""")
    fun searchAllMovieByCategoriesWSort(serverId: Int, categoryIds: List<Int>, sort: String): PagingSource<Int, SearchResult>

    @Query("""
    SELECT m.uniqueId, m.name, m.streamIcon as image, 'movie' as type
    FROM search_fts sf INNER JOIN movie m ON sf.uniqueId = m.uniqueId
    WHERE sf.type = 'movie' AND m.server = :serverId AND m.categoryId IN (:categoryIds)
      AND search_fts MATCH :query
    ORDER BY
        CASE WHEN :sort = 'az' THEN m.name END ASC,
        CASE WHEN :sort = 'za' THEN m.name END DESC
""")
    fun searchMovieByCategoriesWSort(serverId: Int, categoryIds: List<Int>, query: String, sort: String): PagingSource<Int, SearchResult>
}