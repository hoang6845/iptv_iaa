package com.iptvplayer.m3u.stream.ui.searchXtream

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.iptvplayer.m3u.stream.R
import com.iptvplayer.m3u.stream.api.LiveXtreamRepository
import com.iptvplayer.m3u.stream.api.LiveXtreamService
import com.iptvplayer.m3u.stream.databinding.FragmentSearchXtreamBinding
import com.iptvplayer.m3u.stream.model.dao.LiveXtreamDao
import com.iptvplayer.m3u.stream.model.dao.MovieDao
import com.iptvplayer.m3u.stream.model.dao.SeriesDao
import com.iptvplayer.m3u.stream.model.dao.ServerDao
import com.iptvplayer.m3u.stream.model.entity.XtreamAuth
import com.iptvplayer.m3u.stream.ui.channels.CategoryTabAdapter
import com.iptvplayer.m3u.stream.ui.sort_bottom_sheet.SortBottomSheet
import com.iptvplayer.m3u.stream.utils.AppConstants
import com.iptvplayer.m3u.stream.utils.gone
import com.iptvplayer.m3u.stream.utils.visible
import dagger.hilt.android.AndroidEntryPoint
import hoang.dqm.codebase.base.activity.BaseFragment
import hoang.dqm.codebase.base.activity.navigate
import hoang.dqm.codebase.base.activity.onBackPressed
import hoang.dqm.codebase.base.activity.popBackStack
import hoang.dqm.codebase.data.CategoryItem
import hoang.dqm.codebase.utils.collectLatestFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Inject

@AndroidEntryPoint
class SearchXtreamFragment : BaseFragment<FragmentSearchXtreamBinding, SearchXtreamViewModel>() {
    @Inject
    lateinit var liveXtreamDao: LiveXtreamDao

    @Inject
    lateinit var serverDao: ServerDao

    @Inject
    lateinit var movieDao: MovieDao

    @Inject
    lateinit var seriesDao: SeriesDao
    private var isSortDesc = false
    private val isOpenSearch: Boolean by lazy {
        arguments?.getBoolean("isOpenSearch") ?: false
    }
    private val categoryTabAdapter: CategoryTabAdapter by lazy {
        CategoryTabAdapter()
    }
    private val serverId: Int? by lazy {
        arguments?.getInt("serverId")
    }

    private val movieAdapter: MovieSearchAdapter by lazy {
        MovieSearchAdapter()
    }
    private val favouriteAdapter: FavouriteAdapter by lazy { FavouriteAdapter() }
    private var server: XtreamAuth? = null

    private val liveSearchAdapter: LiveSearchAdapter by lazy {
        LiveSearchAdapter()
    }

    override fun initView() {
        adjustInsetsForBottomNavigation(binding.toolBar)

        serverId?.let { id ->
            viewModel.setServerId(id)
            CoroutineScope(Dispatchers.IO).launch {
                server = serverDao.getServerById(id)
            }
        }
        setUpAdapter()
        if (isOpenSearch) {
            binding.root.post {
                openSearch()
                binding.titleWatchList.gone()
                binding.btnSort.gone()
            }
        }
    }

    override fun initListener() {
        binding.edtSearch.doAfterTextChanged {
            viewModel.search(it.toString())
        }
        onBackPressed {
            popBackStack()
        }
        binding.btnBack.setOnClickListener {
            popBackStack()
        }

        binding.btnSearch.setOnClickListener {
            openSearch()
        }

        binding.edtSearch.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                closeSearch()
            }
        }

        binding.root.setOnClickListener {
            binding.edtSearch.clearFocus()
        }
        binding.btnSort.setOnClickListener {
            SortBottomSheet(viewModel.getSort()).show(childFragmentManager, "sort")
        }
        childFragmentManager.setFragmentResultListener(
            "sort_bottom_sheet",
            viewLifecycleOwner
        ) { _, bundle ->
            val action = bundle.getString("action") ?: "none"
            viewModel.setSort(action)
        }
        binding.textCancel.setOnClickListener {
            binding.edtSearch.clearFocus()
        }
    }

    override fun initData() {
        lifecycleScope.launch {
            viewModel.movieResult.collectLatest {
                movieAdapter.submitData(it)
            }
        }

        lifecycleScope.launch {
            viewModel.liveResult.collectLatest {
                liveSearchAdapter.submitData(it)
            }
        }

        if (isOpenSearch) {
            // ── Logic cũ: chỉ dùng movie/live adapter, không quan tâm favourite ──
            collectLatestFlow(viewModel.showMovieSection) { show ->
                binding.titleMovie.isVisible = show
                binding.rvMovie.isVisible = show
            }
            collectLatestFlow(viewModel.showLiveSection) { show ->
                binding.titleLive.isVisible = show
                binding.rvLive.isVisible = show
            }
            // Ẩn hết phần favourite khi đang search
            binding.rvFavourite.isVisible = false
            binding.titleWatchList.isVisible = false
            binding.noContent.isVisible = false
        } else {
            // ── Logic mới: ưu tiên favourite, fallback theo tab ──
            collectLatestFlow(
                combine(viewModel.favouriteResult, viewModel.selectedCategory) { favs, cat ->
                    Pair(favs, cat)
                }
            ) { (favourites, category) ->
                val hasFavourites = favourites.isNotEmpty()

                // Favourite section
                favouriteAdapter.submitList(favourites)
                binding.rvFavourite.isVisible = hasFavourites
                binding.titleWatchList.isVisible = hasFavourites

                // Movie/Series: chỉ hiện khi không có favourite VÀ tab là movie/series
                val showMovie = !hasFavourites &&
                        (category == AppConstants.MOVIE || category == AppConstants.SERIES)
                binding.titleMovie.isVisible = showMovie
                binding.rvMovie.isVisible = showMovie

                // Live: chỉ hiện khi không có favourite VÀ tab là live
                val showLive = !hasFavourites && category == AppConstants.LIVE
                binding.titleLive.isVisible = showLive
                binding.rvLive.isVisible = showLive

                // Tab "all" + không có favourite → trang trống
                val showNoContent = !hasFavourites && category == AppConstants.ALL
                binding.noContent.isVisible = showNoContent
            }
        }

        serverId?.let { id ->
            Log.d("check live", "initData: $id")
            loadLiveXtreamIfNeeded(id)
        }
    }

    fun setUpAdapter() {
        favouriteAdapter.setOnItemClick { result, _ ->
            CoroutineScope(Dispatchers.IO).launch {
                serverId?.let {
                    when (result.type) {
                        "movie" -> {
                            val movie = movieDao.getOne(it, result.uniqueId)
                            withContext(Dispatchers.Main) {
                                movie?.streamId?.let { streamId ->
                                    showInterstitialAd {
                                        navigate(R.id.xtreamMovieDetailFragment, Bundle().apply {
                                            putInt("serverId", it)
                                            putLong("vodId", streamId)
                                        })
                                    }

                                }
                            }
                        }
                        "series" -> {
                            val series = seriesDao.getOneS(it, result.uniqueId)
                            withContext(Dispatchers.Main) {
                                series?.seriesId?.let { seriesId ->
                                    showInterstitialAd {
                                        navigate(R.id.seriesDetailFragment, Bundle().apply {
                                            putInt("serverId", it)
                                            putLong("seriesId", seriesId)
                                        })
                                    }

                                }
                            }
                        }
                        "live" -> {
                            val liveXtream = liveXtreamDao.getOne(it, result.uniqueId)
                            withContext(Dispatchers.Main) {
                                showInterstitialAd {
                                    navigate(R.id.streamFragment, Bundle().apply {
                                        putInt("streamId", liveXtream?.streamId ?: 0)
                                        putString("movieName", liveXtream?.name)
                                        putString("username", server?.username)
                                        putString("password", server?.password)
                                        putString("server", server?.server)
                                        putString("type", "live")
                                    })
                                }

                            }
                        }
                    }
                }
            }
        }

        binding.rvFavourite.adapter = favouriteAdapter
        binding.rvFavourite.layoutManager = GridLayoutManager(requireContext(), 3)

        movieAdapter.setOnItemClick { result, i ->
            CoroutineScope(Dispatchers.IO).launch {
                serverId?.let {
                    when (result.type) {
                        "movie" -> {
                            val movie = movieDao.getOne(it, result.uniqueId)
                            if (movie == null || server == null) {
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
                                    showInterstitialAd {
                                        navigate(R.id.xtreamMovieDetailFragment, bundle)

                                    }
                                }
                            }
                        }

                        "series" -> {
                            val series = seriesDao.getOneS(it, result.uniqueId)
                            if (series == null || server == null) {
                                Toast.makeText(
                                    requireContext(),
                                    "This series is not available",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            withContext(Dispatchers.Main) {
                                series?.seriesId?.let { seriesId ->
                                    val bundle = Bundle().apply {
                                        putInt("serverId", it)
                                        putLong("seriesId", seriesId)
                                    }
                                    showInterstitialAd {
                                        navigate(R.id.seriesDetailFragment, bundle)

                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        binding.rvMovie.adapter = movieAdapter
        binding.rvMovie.layoutManager = GridLayoutManager(
            requireContext(),
            3,
            GridLayoutManager.VERTICAL,
            false
        )

        liveSearchAdapter.setOnItemClick { result, i ->
            CoroutineScope(Dispatchers.IO).launch {
                serverId?.let {
                    val liveXtream = liveXtreamDao.getOne(it, result.uniqueId)
                    if (liveXtream == null || server == null) {
                        Toast.makeText(
                            requireContext(),
                            "This liveTV is not available",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    withContext(Dispatchers.Main) {
                        val bundle = Bundle().apply {
                            putInt("streamId", liveXtream?.streamId ?: 0)
                            putString("movieName", liveXtream?.name)
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
        }
        binding.rvLive.adapter = liveSearchAdapter
        binding.rvLive.layoutManager = GridLayoutManager(
            requireContext(),
            3,
            GridLayoutManager.VERTICAL,
            false
        )
        categoryTabAdapter.setList(
            listOf(
                CategoryItem(
                    type = "all",
                    value = getString(R.string.text_all)
                ),
                CategoryItem(
                    type = AppConstants.MOVIE,
                    value = getString(R.string.text_movie)
                ),
                CategoryItem(
                    type = AppConstants.SERIES,
                    value = getString(R.string.text_series)
                ),
                CategoryItem(
                    type = AppConstants.LIVE,
                    value = getString(R.string.text_live_tv)
                )
            )
        )

        binding.rvCategory.adapter = categoryTabAdapter
        binding.rvCategory.layoutManager = LinearLayoutManager(
            requireContext(),
            LinearLayoutManager.HORIZONTAL, false
        )

        categoryTabAdapter.setOnClickItemAdapter { item, position ->
            viewModel.setCategory(item.type)
            this@SearchXtreamFragment.categoryTabAdapter.setCategoriesSelected(position)
        }

    }

    private fun openSearch() {
        binding.searchContainer.apply {
            visibility = View.VISIBLE
            alpha = 0f
            translationX = 200f

            animate()
                .alpha(1f)
                .translationX(0f)
                .setDuration(250)
                .start()
        }

        binding.edtSearch.requestFocus()
        binding.textCancel.visibility = View.VISIBLE
        binding.btnSearch.gone()
        binding.btnSort.gone()
        showKeyboard(binding.edtSearch)
    }

    private fun closeSearch() {
        binding.searchContainer.animate()
            .alpha(0f)
            .translationX(200f)
            .setDuration(200)
            .withEndAction {
                if (!isAdded || isDetached || view == null) return@withEndAction
                binding.searchContainer.visibility = View.GONE
                hideKeyboard()
            }
            .start()
        binding.textCancel.visibility = View.GONE
        binding.btnSearch.visible()
        binding.btnSort.visible()

    }

    private fun showKeyboard(view: View) {
        val imm =
            requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun hideKeyboard() {
        val imm =
            requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(requireActivity().window.decorView.windowToken, 0)
    }

    private fun loadLiveXtreamIfNeeded(serverId: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            val serverAuth = serverDao.getServerById(serverId)
            if (serverAuth == null) return@launch

            val existing = liveXtreamDao.hasData(serverId)
            Log.d("check exiting", "loadLiveXtreamIfNeeded: $existing")
            if (!existing) {
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

}