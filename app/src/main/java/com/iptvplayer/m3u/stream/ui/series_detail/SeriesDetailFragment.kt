package com.iptvplayer.m3u.stream.ui.series_detail

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.iptvplayer.m3u.stream.R
import com.iptvplayer.m3u.stream.api.MovieRepository
import com.iptvplayer.m3u.stream.api.SeriesApiService
import com.iptvplayer.m3u.stream.api.SeriesRepository
import com.iptvplayer.m3u.stream.databinding.FragmentSeriesDetailBinding
import com.iptvplayer.m3u.stream.model.dao.SeriesDao
import com.iptvplayer.m3u.stream.model.dao.ServerDao
import com.iptvplayer.m3u.stream.model.entity.Series
import com.iptvplayer.m3u.stream.model.entity.SeriesDetailResponse
import com.iptvplayer.m3u.stream.model.entity.XtreamAuth
import com.iptvplayer.m3u.stream.ui.episodeFragment.ShareViewModel
import com.iptvplayer.m3u.stream.ui.xtream_movie_detail.StarAdapter
import com.iptvplayer.m3u.stream.utils.ErrorPlayerDialog
import dagger.hilt.android.AndroidEntryPoint
import hoang.dqm.codebase.base.activity.BaseFragment
import hoang.dqm.codebase.base.activity.navigate
import hoang.dqm.codebase.base.activity.onBackPressed
import hoang.dqm.codebase.base.activity.popBackStack
import hoang.dqm.codebase.utils.collectLatestFlow
import hoang.dqm.codebase.utils.loadImageSketch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Inject


@AndroidEntryPoint
class SeriesDetailFragment : BaseFragment<FragmentSeriesDetailBinding, SeriesDetailViewModel>() {
    @Inject
    lateinit var serverDao: ServerDao
    @Inject
    lateinit var seriesDao: SeriesDao
    private var shareViewModel: ShareViewModel? = null
    private val serverId: Int? by lazy {
        arguments?.getInt("serverId")
    }
    private val seriesId: Long? by lazy {
        arguments?.getLong("seriesId")
    }
    private val starAdapter: StarAdapter by lazy {
        StarAdapter()
    }
    private val seasonSeriesAdapter: SeasonSeriesAdapter by lazy {
        SeasonSeriesAdapter()
    }
    private var server: XtreamAuth? = null
    private var seriesSelected: Series? = null
    private var uniqueId: String? = null
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        shareViewModel =
            ViewModelProvider(requireActivity())[ShareViewModel::class.java]
    }

    override fun initView() {
        adjustInsetsForBottomNavigation(binding.btnFavourite)
        serverId?.let { serverId ->
            seriesId?.let { seriesId ->
                CoroutineScope(Dispatchers.IO).launch {
                    server = serverDao.getServerById(serverId)
                    seriesSelected = seriesDao.getOne(seriesId, serverId)
                    if (server == null || seriesSelected == null) return@launch

                    server?.let { server ->
                        val repository = SeriesRepository(createApiService(server.server))
                        Log.d("check series", "setupRecyclerView: $seriesId")

                        viewModel.getSeriesInfo(repository, server.username, server.password, seriesId)
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
                    viewModel.fetchError.collect { hasError ->
                        if (!hasError) return@collect
                        ErrorPlayerDialog(
                            activity = requireActivity(),
                            onReload = {
                                serverId?.let { id ->
                                    seriesId?.let { sid ->
                                        server?.let { s ->
                                            viewModel.resetFetchError() // reset trước khi gọi lại
                                            val repository = SeriesRepository(createApiService(s.server))
                                            viewModel.getSeriesInfo(repository, s.username, s.password, sid)
                                        }
                                    }
                                }
                            },
                            onBack = { popBackStack() }
                        ).show()
                    }
                }
                launch {
                    viewModel.isFavourite.collect { value ->
                        binding.btnFavourite.setImageResource(
                            if (value) R.drawable.ic_favourited_detail else R.drawable.ic_favourite_detail
                        )
                    }
                }
                launch {
                    viewModel.seriesInfo.collect { seriesInfo ->
                        if (seriesInfo == null) return@collect
                        showDetail(seriesInfo)
                    }
                }
            }
        }
    }

    private fun createApiService(url: String): SeriesApiService {
        val retrofit = Retrofit.Builder()
            .baseUrl(url)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        return retrofit.create(SeriesApiService::class.java)
    }

    fun showDetail(seriesInfo: SeriesDetailResponse){
        seriesSelected?.let { seriesSelected ->
            seriesSelected.uniqueId?.let { uniqueId ->
                serverId?.let { id ->
                    viewModel.loadFavourite(id, uniqueId)
                    this@SeriesDetailFragment.uniqueId = uniqueId
                }
            }
            binding.imgBg.loadImageSketch(seriesSelected.backdropPath[0])

            binding.rvStar.adapter = starAdapter
            binding.rvStar.layoutManager = GridLayoutManager(
                requireContext(),
                5,
                GridLayoutManager.VERTICAL,
                false
            )
            seriesSelected.rating?.let {
                starAdapter.setList(rating10ToStars(it.toSafeFloat()))
            }?:run {
                starAdapter.setList(rating10ToStars(0f))
            }
            binding.releaseDay.text = seriesSelected.releaseDate
            binding.duration.text = seriesSelected.episodeRunTime
            binding.genre.text = seriesSelected.genre
            binding.description.text = seriesSelected.plot
            binding.name.text = seriesSelected.name
        }
        seasonSeriesAdapter.setUpList(seriesInfo.seasons, seriesInfo.episodes)
        seasonSeriesAdapter.setOnClickItem({ episode, position ->
            server?.let { server ->
                val bundle = Bundle().apply {
                    putString("episodeId", episode.id)
                    putInt("serverId", server.id)
                    putString("episodeName", episode.title)
                    putString("containerExtension", episode.containerExtension)
                    putString("username", server.username)
                    putString("password", server.password)
                    putString("server", server.server)
                    putString("seasonSelected", episode.season.toString())
                    this@SeriesDetailFragment.seasonSeriesAdapter.dataList.find { it -> it.seasonNumber == episode.season}?.name?.let {
                        putString("seasonName", it)
                    }
                }
                shareViewModel?.setSeriesDetail(seriesInfo)
                showInterstitialAd {
                    navigate(R.id.episodeFragment, bundle)

                }
            }
        }, { season, position ->
            this@SeriesDetailFragment.seasonSeriesAdapter.toggleVisible(position)
        })
        binding.rvSeason.adapter = seasonSeriesAdapter
        binding.rvSeason.layoutManager = LinearLayoutManager(
            requireContext(),
            LinearLayoutManager.VERTICAL,
            false
        )
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