package com.iptvplayer.m3u.stream.ui.xtream

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.iptvplayer.m3u.stream.R
import com.iptvplayer.m3u.stream.api.LiveXtreamRepository
import com.iptvplayer.m3u.stream.api.LiveXtreamService
import com.iptvplayer.m3u.stream.databinding.FragmentXtreamBinding
import com.iptvplayer.m3u.stream.model.dao.LiveXtreamDao
import com.iptvplayer.m3u.stream.model.dao.ServerDao
import com.iptvplayer.m3u.stream.model.entity.CategoryMoviesState
import com.iptvplayer.m3u.stream.utils.AppConstants
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
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Inject

@AndroidEntryPoint
class XtreamFragment : BaseFragment<FragmentXtreamBinding, XtreamViewModel>() {
    @Inject
    lateinit var serverDao: ServerDao

    @Inject
    lateinit var liveXtreamDao: LiveXtreamDao
    private val serverId: Int? by lazy {
        arguments?.getInt("serverId")
    }

    private val categoryAdapter: CategoryMovieAdapter by lazy {
        CategoryMovieAdapter()
    }

    private var avatar: String? = null

    private val carouselContainerAdapter: CarouselContainerAdapter by lazy {
        CarouselContainerAdapter { movie ->
            serverId?.let { serverId ->
                movie.streamId?.let { streamId ->
                    val bundle = Bundle().apply {
                        putInt("serverId", serverId)
                        putLong("vodId", streamId)
                        putString("uniqueId", movie.uniqueId)
                    }
                    showInterstitialAd {
                        navigate(R.id.xtreamMovieDetailFragment, bundle)

                    }
                }
            }
        }
    }

    private val concatAdapter: ConcatAdapter by lazy {
        ConcatAdapter(
            ConcatAdapter.Config.Builder()
                .setIsolateViewTypes(true)
                .build(),
            carouselContainerAdapter,
            categoryAdapter
        )
    }

    override fun initView() {
        adjustInsetsForBottomNavigation(binding.titleX)
        binding.edtSearch.isFocusable = false
        binding.edtSearch.isFocusableInTouchMode = false
        serverId?.let {
            CoroutineScope(Dispatchers.IO).launch{
                val avatar = serverDao.getServerById(it)?.urlAvatar
                withContext(Dispatchers.Main){
                    binding.btnBack.loadImageSketch(avatar?:"avatar/avatar_1.png", isFull = false)
                    viewModel.loadCategories(it)
                }
            }
        } ?: run {
            navigate(R.id.homeFragment)
        }
        setupRecyclerView()
        setupCarousel()
        setupScrollListener()
        observeViewModel()
    }

    override fun initListener() {
        onBackPressed {
            popBackStack(R.id.xtreamHomeFragment)
        }
        binding.btnBack.setOnClickListener {
            serverId?.let { serverId ->
                val bundle = Bundle().apply {
                    putInt("serverId", serverId)
                    putString("avatar", avatar)
                }
                navigate(R.id.profileFragment, bundle)
            }
        }
        binding.tvSearch.setOnClickListener {
            openSearch()
        }
        binding.edtSearch.setOnClickListener {
            openSearch()
        }
    }

    private fun openSearch() {
        serverId?.let { id ->
            val bundle = Bundle().apply {
                putInt("serverId", id)
                putBoolean("isOpenSearch", true)
            }
            navigate(R.id.searchXtreamFragment, bundle)
        }
    }

    override fun initData() {
        serverId?.let { id ->
            Log.d("check live", "initData: $id")
            loadLiveXtreamIfNeeded(id)
        }
    }


    private fun loadLiveXtreamIfNeeded(serverId: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            val serverAuth = serverDao.getServerById(serverId) ?: return@launch

            val hasData = liveXtreamDao.hasData(serverId)
            if (!hasData) {
                viewModel.setLiveLoading()
                val repository = LiveXtreamRepository(createApiService(serverAuth.server))
                viewModel.getLiveXtream(
                    repository = repository,
                    username = serverAuth.username,
                    password = serverAuth.password,
                    serverId = serverAuth.id
                )
            }
        }
    }

    private fun createApiService(url: String): LiveXtreamService {
        val retrofit = Retrofit.Builder()
            .baseUrl(url)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        return retrofit.create(LiveXtreamService::class.java)
    }

    private fun setupCarousel() {
//        binding.rvCarousel.apply {
//            layoutManager = CarouselLinearLayoutManager(
//                context = requireContext(),
//                shrinkAmount = 0.35f,
//                shrinkDistance = 0.8f
//            )
//
//            CenterSnapHelper().attachToRecyclerView(this)
//
//            clipToPadding = false
//
//            val paddingPx = (resources.displayMetrics.density * 24).toInt()
//            setPadding(paddingPx, 0, paddingPx, 0)
//
//            addItemDecoration(CarouselItemDecoration(spacingDp = 4))
//
//            overScrollMode = View.OVER_SCROLL_NEVER
//            adapter = carouselAdapter
//        }
    }

    private fun RecyclerView.paddingHorizontal(padding: Int) {
        setPadding(padding, 0, padding, 0)
    }

    private fun setupRecyclerView() {
        categoryAdapter.setListener(
            onSeriesClick = { item, _ ->
                serverId?.let { serverId ->
                    item.seriesId?.let { seriesId ->
                        val bundle = Bundle().apply {
                            putInt("serverId", serverId)
                            putLong("seriesId", seriesId)
                        }
                        showInterstitialAd {
                            Log.d("check series", "setupRecyclerView: $seriesId")
                            navigate(R.id.seriesDetailFragment, bundle)

                        }
                    }
                }
            },
            onMovieClick = { item, _ ->
                serverId?.let { serverId ->
                    item.streamId?.let { streamId ->
                        val bundle = Bundle().apply {
                            putInt("serverId", serverId)
                            putLong("vodId", streamId)
                            putString("uniqueId", item.uniqueId)
                        }
                        showInterstitialAd {
                            navigate(R.id.xtreamMovieDetailFragment, bundle)

                        }
                    }
                }
            },
            onLiveClick = { live, _ ->
                serverId?.let { serverId ->
                    CoroutineScope(Dispatchers.IO).launch {
                        val server = serverDao.getServerById(serverId)
                        if (server == null) return@launch
                        withContext(Dispatchers.Main) {
                            val bundle = Bundle().apply {
                                putInt("streamId", live.streamId ?: 0)
                                putString("movieName", live.name)
                                putString("username", server?.username)
                                putString("password", server?.password)
                                putString("server", server?.server)
                                putString("type", "live")
                            }
                            showInterstitialAd {
                                navigate(R.id.streamFragment, bundle)
                            }
                        }
                    }

                }
            },
            onCategoryVisible = { categoryId ->
                serverId?.let {
                    viewModel.loadMoviesForCategory(categoryId, it)
                }
            },
            onLoadMoreLive = {
                serverId?.let { viewModel.loadAllLive(it) }
            },
            goOpenLive = {
                CoroutineScope(Dispatchers.IO).launch {
                    val server = serverDao.getServerById(serverId ?: 0)
                    if (server == null) return@launch
                    withContext(Dispatchers.Main) {
                        val bundle = Bundle().apply {
                            putString("username", server.username)
                            putString("password", server.password)
                            putString("server", server?.server)
                            putString("type", "live")
                            putInt("serverId", serverId ?: 0)
                        }
                        showInterstitialAd {
                            navigate(R.id.liveOpenFragment, bundle)

                        }
                    }
                }
            },
            goOpenMovie = { item ->
                val bundle = Bundle().apply {
                    putInt("serverId", serverId ?: 0)
                    putInt("categorySelected", item.categoryId)
                    putString("categorySelectedName", item.categoryName)
                    putString("type", "movie")
                    putBoolean("isVirtualRecommend", item.categoryId == XtreamViewModel.RECOMMEND_VIRTUAL_CATEGORY_ID)
                }
                showInterstitialAd {
                    navigate(R.id.movieOpenFragment, bundle)

                }
            },
            goOpenSeries = { item ->
                val bundle = Bundle().apply {
                    putInt("serverId", serverId ?: 0)
                    putInt("categorySelected", item.categoryId)
                    putString("categorySelectedName", item.categoryName)
                    putString("type", "series")
                }
                showInterstitialAd {
                    navigate(R.id.movieOpenFragment, bundle)

                }
            }
        )
        binding.rvMain.apply {
            layoutManager =
                LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
            adapter = concatAdapter
            setHasFixedSize(false)
            setItemViewCacheSize(5)
        }
//        binding.rvCategories.apply {
//            layoutManager =
//                LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
//            adapter = categoryAdapter
//            setHasFixedSize(false)
//            setItemViewCacheSize(5)
//        }
    }

    private var lastMovieStates: Map<Int, CategoryMoviesState> = emptyMap()

    private fun observeViewModel() {
        collectLatestFlow(viewModel.categories) { categories ->
            categoryAdapter.setList(categories)
        }
        collectLatestFlow(viewModel.carouselMovies) { movies ->
            carouselContainerAdapter.submitList(movies.take(20))
        }
//        collectLatestFlow(viewModel.carouselMovies) { movies ->
//            carouselAdapter.submitList(movies)
//            binding.rvCarousel.post {
//                val itemCount = binding.rvCarousel.adapter?.itemCount ?: 0
//                if (itemCount > 1) {
//                    binding.rvCarousel.smoothScrollToPosition(1)
//                }
//            }
//        }
        collectLatestFlow(viewModel.categoryMoviesState) { newMap ->
            newMap.forEach { (categoryId, newState) ->
                if (lastMovieStates[categoryId] != newState) {
                    binding.rvMain.post {              // ← thay rvCategories
                        categoryAdapter.updateMoviesState(categoryId, newState)
                    }
                }
            }
            lastMovieStates = newMap
        }
//        collectLatestFlow(viewModel.categoryMoviesState) { newMap ->
//            newMap.forEach { (categoryId, newState) ->
//                if (lastMovieStates[categoryId] != newState) {
//                    binding.rvCategories.post {
//                        categoryAdapter.updateMoviesState(categoryId, newState)
//                    }
//                }
//            }
//            lastMovieStates = newMap
//        }

        collectLatestFlow(viewModel.isLoadingMovie) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
    }

    private fun setupScrollListener() {
        binding.rvMain.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                val first = layoutManager.findFirstVisibleItemPosition()
                val last = layoutManager.findLastVisibleItemPosition()
                val categories = viewModel.categories.value

                for (i in first..minOf(last + 1, categories.size + 1)) {
                    val categoryIndex = i - 1
                    if (categoryIndex >= 0 && categoryIndex < categories.size) {
                        val category = categories[categoryIndex]
                        serverId?.let {
                            when (category.type) {
                                AppConstants.MOVIE -> viewModel.loadMoviesForCategory(
                                    category.categoryId,
                                    it
                                )

                                AppConstants.SERIES -> viewModel.loadSeriesForCategory(
                                    category.categoryId,
                                    it
                                )

                                AppConstants.LIVE -> {
                                    val liveState =
                                        viewModel.categoryMoviesState.value[XtreamViewModel.LIVE_VIRTUAL_CATEGORY_ID]
                                    Log.d("check live state", "onScrolled: $liveState")
                                    if (liveState == null || liveState is CategoryMoviesState.NotLoaded) {
                                        viewModel.loadAllLive(it)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        })
    }
}