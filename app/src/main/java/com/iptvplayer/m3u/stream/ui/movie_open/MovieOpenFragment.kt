package com.iptvplayer.m3u.stream.ui.movie_open

import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import com.iptvplayer.m3u.stream.R
import com.iptvplayer.m3u.stream.databinding.FragmentMovieOpenBinding
import com.iptvplayer.m3u.stream.model.dao.MovieDao
import com.iptvplayer.m3u.stream.model.dao.SeriesDao
import com.iptvplayer.m3u.stream.model.dao.ServerDao
import com.iptvplayer.m3u.stream.model.entity.XtreamAuth
import com.iptvplayer.m3u.stream.ui.sort_bottom_sheet.SortBottomSheet
import com.iptvplayer.m3u.stream.ui.update_movie_bottom_sheet.UpdateMovieBottomSheet
import com.iptvplayer.m3u.stream.utils.gone
import com.iptvplayer.m3u.stream.utils.visible
import dagger.hilt.android.AndroidEntryPoint
import hoang.dqm.codebase.base.activity.BaseFragment
import hoang.dqm.codebase.base.activity.navigate
import hoang.dqm.codebase.base.activity.onBackPressed
import hoang.dqm.codebase.base.activity.popBackStack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@AndroidEntryPoint
class MovieOpenFragment : BaseFragment<FragmentMovieOpenBinding, MovieOpenViewModel>() {

    @Inject
    lateinit var serverDao: ServerDao

    @Inject
    lateinit var movieDao: MovieDao

    @Inject
    lateinit var seriesDao: SeriesDao

    private var server: XtreamAuth? = null

    private val serverId: Int? by lazy {
        arguments?.getInt("serverId")
    }

    private val isVirtualRecommend: Boolean by lazy {
        arguments?.getBoolean("isVirtualRecommend") ?: false
    }

    private val categorySelected: Int? by lazy {
        arguments?.getInt("categorySelected")
    }

    private val categoryName: String? by lazy {
        arguments?.getString("categorySelectedName")
    }

    private val type: String by lazy {
        arguments?.getString("type")?:"movie"
    }

    private val movieAdapter: MovieOpenAdapter by lazy {
        MovieOpenAdapter()
    }

    override fun initView() {
        adjustInsetsForBottomNavigation(binding.toolBar)
        viewModel.setType(type)
        binding.titleMovie.text = categoryName
        serverId?.let { id ->
            viewModel.setServerId(id)
            CoroutineScope(Dispatchers.IO).launch {
                server = serverDao.getServerById(id)
            }
        }

        categorySelected?.let { id ->
            viewModel.setCategoryId(id)
        }

        setupAdapter()
        if (isVirtualRecommend) {
            viewModel.setIsVirtualRecommend(true)
            serverId?.let { viewModel.loadRecommendCategories(it) }
            binding.btnFilter.visible()
        } else {
            binding.btnFilter.gone()
        }
    }

    override fun initListener() {
        binding.btnFilter.setOnClickListener { openCategoryFilter() }
        onBackPressed {
            popBackStack()
        }
        binding.btnMore.setOnClickListener {
            val bottomSheet = UpdateMovieBottomSheet(movieAdapter.isShowName)
            bottomSheet.show(childFragmentManager, "BOTTOM_SHEET_UPDATE_MOVIE")
        }

        childFragmentManager.setFragmentResultListener(
            "update_bottom_movie_sheet",
            viewLifecycleOwner
        ) { _, bundle ->
            val action = bundle.getString("action")
            if (action == "update") {
                serverId?.let { viewModel.updateMovie(it) }
            } else if (action == "show_name"){
                movieAdapter.setShowNameNow(!movieAdapter.isShowName)
            }
        }

        binding.btnBack.setOnClickListener {
            popBackStack()
        }

        binding.btnSearch.setOnClickListener {
            openSearch()
        }

        binding.edtSearch.doAfterTextChanged {
            viewModel.search(it.toString())
        }

        binding.edtSearch.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) closeSearch()
        }

//        binding.textCancel.setOnClickListener {
//            binding.edtSearch.clearFocus()
//        }

        binding.root.setOnClickListener {
            binding.edtSearch.clearFocus()
        }

        binding.btnSort.setOnClickListener {
            SortBottomSheet(viewModel.getSort()).show(childFragmentManager, "sort")
        }

        childFragmentManager.setFragmentResultListener("sort_bottom_sheet", viewLifecycleOwner) { _, bundle ->
            val action = bundle.getString("action") ?: "none"
            viewModel.setSort(action)
        }
    }

    private fun openCategoryFilter() {
        val categories   = viewModel.recommendCategories.value
        val selectedId   = viewModel.filterCategoryId.value
        val totalCount   = categories.sumOf { it.count }

        CategoryFilterBottomSheet(
            categories           = categories,
            selectedCategoryId   = selectedId,
            totalCount           = totalCount,
            onSelected           = { categoryId ->
                viewModel.setFilterCategory(categoryId)
                binding.titleCategory.text = if (categoryId == null) "All"
                else categories.find { it.categoryId == categoryId }?.categoryName ?: "All"
            }
        ).show(childFragmentManager, "CATEGORY_FILTER")
    }

    override fun initData() {
        lifecycleScope.launch {
            viewModel.movieResult.collectLatest {
                movieAdapter.submitData(it)
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.updateState.collect { state ->
                    when (state) {
                        is MovieUpdateState.Idle -> {
                            binding.layoutLoading.visibility = View.GONE
                        }
                        is MovieUpdateState.Loading -> {
                            binding.layoutLoading.visibility = View.VISIBLE
                            binding.tvProgress.text = "${state.progress}%"
                            binding.tvLoadingLabel.text = when {
                                state.progress <= 10 -> getString(R.string.text_connecting)
                                state.progress <= 30 -> getString(R.string.text_loading_current_data)
                                state.progress <= 60 -> getString(R.string.text_downloading)
                                state.progress <= 80 -> getString(R.string.text_merge_movie)
                                else -> getString(R.string.text_saving)
                            }
                        }
                        is MovieUpdateState.Success -> {
                            binding.layoutLoading.visibility = View.GONE
                            Toast.makeText(
                                requireContext(),
                                getString(R.string.text_update_success),
                                Toast.LENGTH_SHORT
                            ).show()
                            viewModel.resetUpdateState()
                        }
                        is MovieUpdateState.Error -> {
                            binding.layoutLoading.visibility = View.GONE
                            viewModel.resetUpdateState()
                        }
                    }
                }
            }
        }
    }
    private fun setupAdapter() {
        movieAdapter.setOnItemClick { result, _ ->
            CoroutineScope(Dispatchers.IO).launch {
                serverId?.let { id ->
                    when (result.type) {
                        "movie" -> {
                            val movie = movieDao.getOne(id, result.uniqueId)
                            if (movie == null || server == null) {
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(requireContext(),
                                        getString(R.string.text_this_movie_is_not_available), Toast.LENGTH_SHORT).show()
                                }
                                return@launch
                            }
                            withContext(Dispatchers.Main) {
                                movie.streamId?.let { streamId ->
                                    val bundle = Bundle().apply {
                                        putInt("serverId", id)
                                        putLong("vodId", streamId)
                                    }
                                    navigate(R.id.xtreamMovieDetailFragment, bundle)
                                }
                            }
                        }
                        "series" -> {
                            val series = seriesDao.getOneS(id, result.uniqueId)
                            if (series == null || server == null) {
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(requireContext(),
                                        getString(R.string.text_this_series_is_not_available), Toast.LENGTH_SHORT).show()
                                }
                                return@launch
                            }
                            withContext(Dispatchers.Main) {
                                series.seriesId?.let { seriesId ->
                                    val bundle = Bundle().apply {
                                        putInt("serverId", id)
                                        putLong("seriesId", seriesId)
                                    }
                                    navigate(R.id.seriesDetailFragment, bundle)
                                }
                            }
                        }
                    }
                }
            }
        }

        binding.rvMovie.adapter = movieAdapter
        binding.rvMovie.layoutManager = GridLayoutManager(
            requireContext(), 3, GridLayoutManager.VERTICAL, false
        )
    }

    private fun openSearch() {
        binding.searchContainer.apply {
            visibility = View.VISIBLE
            alpha = 0f
            translationX = 200f
            animate().alpha(1f).translationX(0f).setDuration(250).start()
        }
        binding.edtSearch.requestFocus()
//        binding.textCancel.visibility = View.VISIBLE
        binding.btnSearch.gone()
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
            }.start()
//        binding.textCancel.visibility = View.GONE
        binding.btnSearch.visible()
    }

    private fun showKeyboard(view: View) {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun hideKeyboard() {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(requireActivity().window.decorView.windowToken, 0)
    }
}