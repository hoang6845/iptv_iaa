package com.iptvplayer.m3u.stream.ui.favourite_xtream

import android.os.Bundle
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.iptvplayer.m3u.stream.R
import com.iptvplayer.m3u.stream.databinding.FragmentFavouriteXtreamBinding
import com.iptvplayer.m3u.stream.model.dao.LiveXtreamDao
import com.iptvplayer.m3u.stream.model.dao.MovieDao
import com.iptvplayer.m3u.stream.model.dao.SeriesDao
import com.iptvplayer.m3u.stream.model.dao.ServerDao
import com.iptvplayer.m3u.stream.model.entity.XtreamAuth
import dagger.hilt.android.AndroidEntryPoint
import hoang.dqm.codebase.base.activity.BaseFragment
import hoang.dqm.codebase.base.activity.navigate
import hoang.dqm.codebase.base.activity.onBackPressed
import hoang.dqm.codebase.base.activity.popBackStack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject


@AndroidEntryPoint
class FavouriteXtreamFragment : BaseFragment<FragmentFavouriteXtreamBinding, FavouriteXtreamViewModel>() {
    @Inject
    lateinit var liveXtreamDao: LiveXtreamDao

    @Inject
    lateinit var serverDao: ServerDao

    @Inject
    lateinit var movieDao: MovieDao

    @Inject
    lateinit var seriesDao: SeriesDao
    private var server: XtreamAuth? = null
    private val favouriteAdapter: FavouriteAdapter by lazy {
        FavouriteAdapter()
    }
    override fun initView() {
        adjustInsetsForBottomNavigation(binding.toolBar)
        setUpAdapter()
    }

    private val serverId: Int? by lazy {
        arguments?.getInt("serverId")
    }

    override fun initListener() {
        binding.btnMovie.setOnClickListener {
            selectTab(isMovie = true)
        }
        binding.btnLive.setOnClickListener {
            selectTab(isMovie = false)
        }
        onBackPressed {
            popBackStack(R.id.xtreamHomeFragment)
        }
        binding.imgArrow.setOnClickListener {
            popBackStack()
        }
    }

    override fun initData() {
        observeMovies()
    }

    fun setUpAdapter() {
        favouriteAdapter.setOnClickItem({ item, position ->
            CoroutineScope(Dispatchers.IO).launch {
                serverId?.let {
                    when (item.type) {
                        "movie" -> {
                            val movie = movieDao.getOne(it, item.uniqueId)
                            if (movie == null) {
                                Toast.makeText(
                                    requireContext(),
                                    "This movie is not available",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            withContext(Dispatchers.Main) {
                                movie?.streamId?.let { streamId ->
                                    val bundle = Bundle().apply {
                                        putInt("serverId", it)
                                        putLong("vodId", streamId)
                                    }
                                    navigate(R.id.xtreamMovieDetailFragment, bundle)
                                }
                            }
                        }

                        "series" -> {
                            val series = seriesDao.getOneS(it, item.uniqueId)
                            if (series == null) {
                                Toast.makeText(
                                    requireContext(),
                                    "This movie is not available",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            withContext(Dispatchers.Main) {
                                series?.seriesId?.let { seriesId ->
                                    val bundle = Bundle().apply {
                                        putInt("serverId", it)
                                        putLong("seriesId", seriesId)
                                    }
                                    navigate(R.id.seriesDetailFragment, bundle)
                                }
                            }
                        }

                        "live" -> {
                            val liveXtream = liveXtreamDao.getOne(it, item.uniqueId)
                            server = serverDao.getServerById(it)
                            if (liveXtream == null || server == null){
                                Toast.makeText(requireContext(), "This liveTV is not available", Toast.LENGTH_SHORT).show()
                            }
                            withContext(Dispatchers.Main){
                                val bundle = Bundle().apply {
                                    putInt("streamId", liveXtream?.streamId?:0)
                                    putString("movieName", liveXtream?.name)
                                    putString("username", server?.username)
                                    putString("password", server?.password)
                                    putString("server", server?.server)
                                    putString("type", "live")
                                }
                                navigate(R.id.streamFragment, bundle)
                            }
                        }
                    }
                }
            }

        }) { item, position ->
            serverId?.let {
                viewModel.removeFavourite(it, item.uniqueId)
            }
        }

        binding.rvFavourite.adapter = favouriteAdapter
        binding.rvFavourite.layoutManager = LinearLayoutManager(
            requireContext(),
            LinearLayoutManager.VERTICAL,
            false
        )
    }

    private fun selectTab(isMovie: Boolean) {
        if (isMovie) {
            binding.btnMovie.setBackgroundResource(R.drawable.bg_tab)
            binding.btnMovie.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
            binding.btnLive.setBackgroundResource(R.drawable.bg_tab_disable)
            binding.btnLive.setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
            observeMovies()
        } else {
            binding.btnLive.setBackgroundResource(R.drawable.bg_tab)
            binding.btnLive.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
            binding.btnMovie.setBackgroundResource(R.drawable.bg_tab_disable)
            binding.btnMovie.setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
            observeLives()
        }
    }

    private var moviesJob: Job? = null
    private var livesJob: Job? = null

    private fun observeMovies() {
        livesJob?.cancel()
        moviesJob = viewLifecycleOwner.lifecycleScope.launch {
            viewModel.favouriteMovies.collectLatest { list ->
                favouriteAdapter.setList(list)
            }
        }
    }

    private fun observeLives() {
        moviesJob?.cancel()
        livesJob = viewLifecycleOwner.lifecycleScope.launch {
            viewModel.favouriteLives.collectLatest { list ->
                favouriteAdapter.setList(list)
            }
        }
    }
}