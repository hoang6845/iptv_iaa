package com.iptvplayer.m3u.stream.ui.xtream_movie_detail

import android.os.Bundle
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.iptvplayer.m3u.stream.R
import com.iptvplayer.m3u.stream.api.MovieApiService
import com.iptvplayer.m3u.stream.api.MovieRepository
import com.iptvplayer.m3u.stream.databinding.FragmentXtreamMovieDetailBinding
import com.iptvplayer.m3u.stream.model.dao.MovieDao
import com.iptvplayer.m3u.stream.model.dao.MovieDetailDao
import com.iptvplayer.m3u.stream.model.dao.ServerDao
import com.iptvplayer.m3u.stream.model.entity.MovieDetailEntity
import com.iptvplayer.m3u.stream.model.entity.XtreamAuth
import com.iptvplayer.m3u.stream.utils.ErrorPlayerDialog
import dagger.hilt.android.AndroidEntryPoint
import hoang.dqm.codebase.base.activity.BaseFragment
import hoang.dqm.codebase.base.activity.navigate
import hoang.dqm.codebase.base.activity.onBackPressed
import hoang.dqm.codebase.base.activity.popBackStack
import hoang.dqm.codebase.utils.loadImageSketch
import hoang.dqm.codebase.utils.singleClick
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Inject

@AndroidEntryPoint
class XtreamMovieDetailFragment :
    BaseFragment<FragmentXtreamMovieDetailBinding, XtreamMovieDetailViewModel>() {
    @Inject
    lateinit var serverDao: ServerDao

    @Inject
    lateinit var movieDetailDao: MovieDetailDao

    @Inject
    lateinit var movieDao: MovieDao

    private val serverId: Int? by lazy {
        arguments?.getInt("serverId")
    }
    private val vodId: Long? by lazy {
        arguments?.getLong("vodId")
    }

    private var server: XtreamAuth? = null

    private val starAdapter: StarAdapter by lazy {
        StarAdapter()
    }

    private val movieAdapter: MovieSuggestedAdapter by lazy {
        MovieSuggestedAdapter()
    }
    private val uniqueId: String? by lazy {
        arguments?.getString("uniqueId")
    }

    override fun initView() {
        adjustInsetsForBottomNavigation(binding.containerSuggestMovie)
        serverId?.let { id ->
            uniqueId?.let { uniqueId ->
                viewModel.loadFavourite(id, uniqueId)
            }
        }

        serverId?.let { serverId ->
            vodId?.let { vodId ->
                CoroutineScope(Dispatchers.IO).launch {
                    val movieDetail = movieDetailDao.findOne(vodId)
                    server = serverDao.getServerById(serverId)
                    if (movieDetail == null) {
                        if (server == null) return@launch
                        server?.let { server ->
                            val repository = MovieRepository(createApiService(server.server))
                            viewModel.getMovieInfo(
                                repository,
                                server.username,
                                server.password,
                                vodId
                            )
                            return@launch
                        }
                    }
                    withContext(Dispatchers.Main) {
                        movieDetail?.let { movieDetail ->
                            showDetail(movieDetail)
                            binding.btnPlay.singleClick {
                                val bundle = Bundle().apply {
                                    putInt("streamId", movieDetail.streamId)
                                    movieDetail.name?.let {
                                        putString("movieName", it)
                                    }
                                    putString("containerExtension", movieDetail.containerExtension)
                                    putString("type", "movie")
                                    server?.let { server ->
                                        putString("username", server.username)
                                        putString("password", server.password)
                                        putString("server", server.server)
                                    }
                                }
                                showInterstitialAd {
                                    navigate(R.id.xtreamMovieFragment, bundle)
                                }
                            }
                        }

                    }
                }
            }
        }
    }

    override fun initListener() {
        binding.btnFavourite.setOnClickListener {
            viewModel.toggleFavourite()
        }
        onBackPressed {
            popBackStack()
        }
        binding.btnClose.setOnClickListener {
            popBackStack()
        }
    }

    override fun initData() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.isFavourite.collect { value ->
                        binding.btnFavourite.setImageResource(if (value) R.drawable.ic_favourited_detail else R.drawable.ic_favourite_detail)

                    }
                }
                launch {
                    viewModel.movieInfo.collect { value ->
                        if (value == null) {
                            return@collect
                        }
                        serverId?.let {
                            val movieDetail = MovieDetailEntity(
                                server = it,
                                streamId = value.movieData.streamId,
                                name = value.movieData.name,
                                releaseDate = value.info.releaseDate,
                                rating = value.info.rating,
                                genre = value.info.genre,
                                country = value.info.country,
                                duration = value.info.duration,
                                durationSecs = value.info.durationSecs,
                                coverBig = value.info.coverBig,
                                movieImage = value.info.movieImage,
                                backdropPath = value.info.backdropPath,
                                youtubeTrailer = value.info.youtubeTrailer,
                                description = value.info.description,
                                director = value.info.director,
                                actors = value.info.actors,
                                age = value.info.age,
                                containerExtension = value.movieData.containerExtension,
                                cast = value.info.cast
                            )
                            showDetail(movieDetail)
                            movieDetailDao.insertOne(movieDetail)
                            binding.btnPlay.singleClick {
                                val bundle = Bundle().apply {
                                    putInt("streamId", movieDetail.streamId)
                                    movieDetail.name?.let {
                                        putString("movieName", it)
                                    }
                                    putString("containerExtension", movieDetail.containerExtension)
                                    putString("type", "movie")
                                    server?.let { server ->
                                        putString("username", server.username)
                                        putString("password", server.password)
                                        putString("server", server.server)
                                    }
                                }
                                showInterstitialAd {
                                    navigate(R.id.xtreamMovieFragment, bundle)

                                }
                            }
                        }

                    }
                }
                launch {
                    viewModel.fetchError.collect { hasError ->
                        if (!hasError) return@collect
                        ErrorPlayerDialog(
                            activity = requireActivity(),
                            onReload = {
                                serverId?.let { id ->
                                    vodId?.let { vid ->
                                        server?.let { s ->
                                            viewModel.resetFetchError()
                                            val repository = MovieRepository(createApiService(s.server))
                                            viewModel.getMovieInfo(repository, s.username, s.password, vid)
                                        }
                                    }
                                }
                            },
                            onBack = { popBackStack() }
                        ).show()
                    }
                }
            }
        }


    }

    private fun createApiService(url: String): MovieApiService {
        val retrofit = Retrofit.Builder()
            .baseUrl(url)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        return retrofit.create(MovieApiService::class.java)
    }

    fun showDetail(movieDetailEntity: MovieDetailEntity) {
        movieDetailEntity.backdropPath?.get(0)?.let {
            binding.imgBg.loadImageSketch(it)
        }
        binding.rvStar.adapter = starAdapter
        binding.rvStar.layoutManager = GridLayoutManager(
            requireContext(),
            5,
            GridLayoutManager.VERTICAL,
            false
        )
        movieDetailEntity.rating?.let {
            starAdapter.setList(rating10ToStars(it.toSafeFloat()))
        } ?: run {
            starAdapter.setList(rating10ToStars(0f))
        }
        binding.name.text = movieDetailEntity.name
        binding.releaseDay.text = movieDetailEntity.releaseDate
        binding.duration.text = movieDetailEntity.duration
        binding.description.text = movieDetailEntity.description
        binding.genre.text = movieDetailEntity.genre

        binding.name1.text = movieDetailEntity.name
        binding.releaseDay1.text = movieDetailEntity.releaseDate
        binding.duration1.text = movieDetailEntity.duration
        binding.description1.text = movieDetailEntity.description
        binding.genre1.text = movieDetailEntity.genre
        binding.rating.text = "${movieDetailEntity.rating?.toSafeFloat()}/10"
        binding.actor.text = "${movieDetailEntity.actors}"
        binding.cast.text = "${movieDetailEntity.cast}"

        serverId?.let { serverId ->
            CoroutineScope(Dispatchers.IO).launch {
                val listMovie = movieDao.selectRandomMovies(serverId)
                withContext(Dispatchers.Main) {
                    Log.d("check detail suggest", "showDetail: ${listMovie.size}")
                    movieAdapter.setList(listMovie)
                    movieAdapter.setOnClick { item, position ->
                        item.streamId?.let { streamId ->
                            val bundle = Bundle().apply {
                                putInt("serverId", serverId)
                                putLong("vodId", streamId)
                            }
                            showInterstitialAd {
                                navigate(R.id.xtreamMovieDetailFragment, bundle)
                            }
                        }
                    }
                    binding.rvMovie.adapter = movieAdapter
                    binding.rvMovie.layoutManager = LinearLayoutManager(
                        requireContext(),
                        LinearLayoutManager.HORIZONTAL,
                        false
                    )
                }
            }
        }
    }

    fun rating10ToStars(rating: Float): List<Boolean> {
        val stars = (rating / 2f)
            .toInt()
            .coerceIn(0, 5)

        return List(5) { index -> index < stars }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        serverId?.let { id ->
            uniqueId?.let { uniqueId ->
                viewModel.saveFavourite(id, uniqueId)
            }
        }
    }

    fun String?.toSafeFloat(default: Float = 0f): Float {
        return this?.toFloatOrNull() ?: default
    }
}