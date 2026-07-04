package com.iptvplayer.m3u.stream.utils

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.iptvplayer.m3u.stream.api.ApiFactory
import com.iptvplayer.m3u.stream.api.ApiSeriesFactory
import com.iptvplayer.m3u.stream.model.dao.MovieDao
import com.iptvplayer.m3u.stream.model.dao.MovieDetailDao
import com.iptvplayer.m3u.stream.model.dao.SearchDao
import com.iptvplayer.m3u.stream.model.dao.SeriesDao
import com.iptvplayer.m3u.stream.model.dao.ServerDao
import com.iptvplayer.m3u.stream.model.dao.XtreamCategoryDao
import com.iptvplayer.m3u.stream.model.entity.MovieDetailEntity
import com.iptvplayer.m3u.stream.model.entity.SearchFtsEntity
import com.iptvplayer.m3u.stream.model.entity.XtreamCategory
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

@HiltWorker
class MovieSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val apiFactory: ApiFactory,
    private val apiSeriesFactory: ApiSeriesFactory,
    private val serverDao: ServerDao,
    private val categoryDao: XtreamCategoryDao,
    private val movieDao: MovieDao,
    private val seriesDao: SeriesDao,
    private val searchDao: SearchDao,
    private val movieDetailDao: MovieDetailDao
) : CoroutineWorker(context, workerParams) {
    override suspend fun doWork(): Result {
        val serverId = inputData.getInt(AppConstants.KEY_SERVER_ID, -1)
        if (serverId == -1) return Result.failure()

        val server = serverDao.getServerById(serverId) ?: return Result.failure()

        return try {
            setProgress(workDataOf(AppConstants.PROGRESS_CURRENT to 0, AppConstants.PROGRESS_TOTAL to 100))

            val api = apiFactory.create(server.server)
            val apiSeries = apiSeriesFactory.create(server.server)

            // ── STEP 1: Fetch categories SONG SONG ───────────────────────────
            val (vodCatResponse, seriesCatResponse) = coroutineScope {
                val vod    = async { api.getVodCategories(server.username, server.password) }
                val series = async { apiSeries.getSeriesCategories(server.username, server.password) }
                vod.await() to series.await()
            }

            if (!vodCatResponse.isSuccessful || !seriesCatResponse.isSuccessful) return Result.failure()

            categoryDao.insertAll(
                vodCatResponse.body().orEmpty().map {
                    XtreamCategory(serverId = server.id, categoryId = it.categoryId,
                        categoryName = it.categoryName, type = AppConstants.MOVIE)
                } + seriesCatResponse.body().orEmpty().map {
                    XtreamCategory(serverId = server.id, categoryId = it.categoryId,
                        categoryName = it.categoryName, type = AppConstants.SERIES)
                }
            )

            setProgress(workDataOf(AppConstants.PROGRESS_CURRENT to 10, AppConstants.PROGRESS_TOTAL to 100))
            if (isStopped) return Result.failure()

            // ── STEP 2: Fetch Movies + Series SONG SONG ──────────────────────
            val (moviesResponse, seriesResponse) = coroutineScope {
                val movies = async { api.getMovies(server.username, server.password) }
                val series = async { apiSeries.getAllSeries(server.username, server.password) }
                movies.await() to series.await()
            }

            if (!moviesResponse.isSuccessful || !seriesResponse.isSuccessful) return Result.failure()

            val movies     = moviesResponse.body().orEmpty()
            val seriesList = seriesResponse.body().orEmpty()

            setProgress(workDataOf(AppConstants.PROGRESS_CURRENT to 20, AppConstants.PROGRESS_TOTAL to 100))
            if (isStopped) return Result.failure()

            val allMovieSearch = mutableListOf<SearchFtsEntity>()
            val allSeriesSearch = mutableListOf<SearchFtsEntity>()

            coroutineScope {
                val movieJob = async {
                    val totalMovies = movies.size
                    movies.chunked(AppConstants.CHUNK_SIZE).forEachIndexed { index, chunk ->
                        if (isStopped) return@async
                        val mapped = chunk.map { it.copy(server = server.id, uniqueId = it.generateUniqueId()) }
                        movieDao.insertAll(mapped)
                        synchronized(allMovieSearch) {
                            mapped.mapTo(allMovieSearch) {
                                SearchFtsEntity(name = it.name, type = "movie", uniqueId = it.uniqueId!!)
                            }
                        }
                        val percent = 20 + ((index + 1) * AppConstants.CHUNK_SIZE)
                            .coerceAtMost(totalMovies) * 40 / totalMovies
                        setProgress(workDataOf(AppConstants.PROGRESS_CURRENT to percent, AppConstants.PROGRESS_TOTAL to 100))
                    }
                }

                val seriesJob = async {
                    val totalSeries = seriesList.size
                    seriesList.chunked(AppConstants.CHUNK_SIZE).forEachIndexed { index, chunk ->
                        if (isStopped) return@async
                        val mapped = chunk.map { it.copy(server = server.id, uniqueId = it.generateUniqueId()) }
                        seriesDao.insertAll(mapped)
                        synchronized(allSeriesSearch) {
                            mapped.mapTo(allSeriesSearch) {
                                SearchFtsEntity(name = it.name, type = "series", uniqueId = it.uniqueId!!)
                            }
                        }
                    }
                }

                movieJob.await()
                seriesJob.await()
            }

            if (isStopped) return Result.failure()

            try {
                val movieCategories = vodCatResponse.body().orEmpty()
                val carouselCategory = movieCategories.getOrNull(1) // index 2 giống ViewModel

                carouselCategory?.let { category ->
                    val top10 = movieDao.selectMovieByCategoryAndServer(
                        categoryId = category.categoryId,
                        server = server.id
                    ).take(20)

                    val api = apiFactory.create(server.server)

                    coroutineScope {
                        top10.map { movie ->
                            async {
                                try {
                                    // Bỏ qua nếu đã có detail rồi
                                    val existing = movie.streamId?.let { movieDetailDao.findOne(it) }
                                    if (existing != null) return@async

                                    val response = api.getMovieInfo(
                                        username = server.username,
                                        password = server.password,
                                        vodId = movie.streamId ?: return@async
                                    )

                                    if (response.isSuccessful) {
                                        val body = response.body() ?: return@async
                                        val detail = MovieDetailEntity(
                                            server = server.id,
                                            streamId = body.movieData.streamId,
                                            name = body.movieData.name,
                                            releaseDate = body.info.releaseDate,
                                            rating = body.info.rating,
                                            genre = body.info.genre,
                                            country = body.info.country,
                                            duration = body.info.duration,
                                            durationSecs = body.info.durationSecs,
                                            coverBig = body.info.coverBig,
                                            movieImage = body.info.movieImage,
                                            backdropPath = body.info.backdropPath,
                                            youtubeTrailer = body.info.youtubeTrailer,
                                            description = body.info.description,
                                            director = body.info.director,
                                            actors = body.info.actors,
                                            age = body.info.age,
                                            containerExtension = body.movieData.containerExtension,
                                            cast = body.info.cast
                                        )
                                        movieDetailDao.insertOne(detail)
                                    }
                                } catch (e: Exception) {
                                    Log.w("MovieSyncWorker", "prefetch backdrop failed for ${movie.streamId}", e)
                                }
                            }
                        }.awaitAll()
                    }
                }
            } catch (e: Exception) {
                Log.w("MovieSyncWorker", "carousel prefetch step failed", e)
                // Không return failure — đây là bước optional
            }

            // STEP 5: FTS 1 lần như cũ
            setProgress(workDataOf(AppConstants.PROGRESS_CURRENT to 90, AppConstants.PROGRESS_TOTAL to 100))
            (allMovieSearch + allSeriesSearch).chunked(5000).forEach { batch ->
                searchDao.insertAll(batch)
            }

            setProgress(workDataOf(AppConstants.PROGRESS_CURRENT to 100, AppConstants.PROGRESS_TOTAL to 100))
            Result.success(workDataOf(AppConstants.ID_SERVER to serverId))

        } catch (e: Exception) {
            Log.e("MovieSyncWorker", "Error: ${e.message}", e)
            Result.failure()
        }
    }
}