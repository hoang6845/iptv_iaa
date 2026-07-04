package com.iptvplayer.m3u.stream.ui.xtream

import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.iptvplayer.m3u.stream.R
import com.iptvplayer.m3u.stream.databinding.ItemCategoryMovieBinding
import com.iptvplayer.m3u.stream.model.entity.CategoryMoviesState
import com.iptvplayer.m3u.stream.model.entity.LiveXtream
import com.iptvplayer.m3u.stream.model.entity.Movie
import com.iptvplayer.m3u.stream.model.entity.Series
import com.iptvplayer.m3u.stream.model.entity.XtreamCategory
import hoang.dqm.codebase.base.adapter.BaseRecyclerViewAdapter

class CategoryMovieAdapter : BaseRecyclerViewAdapter<XtreamCategory, ItemCategoryMovieBinding>() {

    private var moviesState: Map<Int, CategoryMoviesState> = emptyMap()

    fun updateMoviesState(
        categoryId: Int,
        state: CategoryMoviesState
    ) {
        moviesState = moviesState.toMutableMap().apply {
            put(categoryId, state)
        }

        val index = dataList.indexOfFirst { it.categoryId == categoryId }
        if (index != -1) notifyItemChanged(index)
    }

    private val skeletonAdapter = SkeletonShimmerAdapter()
    var currentCategoryId: Int? = null
    var hasNotifiedVisibility = false

    override fun bindData(
        binding: ItemCategoryMovieBinding,
        item: XtreamCategory,
        position: Int
    ) {
        binding.tvCategoryName.text = item.categoryName
        val state = moviesState[item.categoryId] ?: CategoryMoviesState.NotLoaded

        when (state) {
            is CategoryMoviesState.NotLoaded,
            is CategoryMoviesState.Loading -> {
                val skeleton = binding.rvMovies.adapter as? SkeletonShimmerAdapter
                    ?: SkeletonShimmerAdapter().also { binding.rvMovies.adapter = it }
                skeleton.setList(listOf(1, 2, 3, 4, 5))
            }

            is CategoryMoviesState.Loaded -> {
                val movieAdapter = binding.rvMovies.adapter as? MovieAdapter
                    ?: MovieAdapter().also { binding.rvMovies.adapter = it }

                // Set listener mỗi lần để tránh stale reference
                binding.icRecent.setOnClickListener { goOpenMovie?.invoke(item) }
                movieAdapter.setOnClick { item, pos -> onMovieClick?.invoke(item, pos) }
                movieAdapter.setList(state.movies)
            }

            is CategoryMoviesState.LoadedSeries -> {
                val seriesAdapter = binding.rvMovies.adapter as? SeriesAdapter
                    ?: SeriesAdapter().also { binding.rvMovies.adapter = it }

                binding.icRecent.setOnClickListener { goOpenSeries?.invoke(item) }
                seriesAdapter.setOnClick { item, pos -> onSeriesClick?.invoke(item, pos) }
                seriesAdapter.setList(state.series)
            }

            is CategoryMoviesState.LoadedLive -> {
                val liveAdapter = binding.rvMovies.adapter as? LiveXtreamAdapter
                    ?: LiveXtreamAdapter().also { binding.rvMovies.adapter = it }

                // ← Fix: set click đúng chỗ, đúng state
                binding.icRecent.setOnClickListener { goOpenLive?.invoke() }
                liveAdapter.setOnClick { live, pos -> onLiveClick?.invoke(live, pos) }

                if (binding.rvMovies.getTag(R.id.tag_scroll_listener_added) == null) {
                    binding.rvMovies.setTag(R.id.tag_scroll_listener_added, true)
                    binding.rvMovies.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                        override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
                            val lm = rv.layoutManager as? LinearLayoutManager ?: return
                            val lastVisible = lm.findLastVisibleItemPosition()
                            val total = lm.itemCount
                            val s = moviesState[item.categoryId] as? CategoryMoviesState.LoadedLive
                            if (s != null && s.canLoadMore && !s.isLoadingMore && lastVisible >= total - 5) {
                                onLoadMoreLive?.invoke()
                            }
                        }
                    })
                }

                liveAdapter.setList(state.lives)
            }

            is CategoryMoviesState.Error -> {
                binding.rvMovies.adapter = null
            }
        }
    }
    private var onSeriesClick: ((item: Series, position: Int) -> Unit)? = null
    private var onMovieClick: ((item: Movie, position: Int) -> Unit)? = null
    private var onLiveClick: ((LiveXtream, Int) -> Unit)? = null
    private var onCategoryVisible: ((Int) -> Unit)? = null
    private var onLoadMoreLive: (() -> Unit)? = null
    private var goOpenLive: (() -> Unit)? = null
    private var goOpenMovie: ((item: XtreamCategory) -> Unit)? = null
    private var goOpenSeries: ((item: XtreamCategory) -> Unit)? = null

    fun setListener(
        onSeriesClick: (item: Series, position: Int) -> Unit,
        onMovieClick: (item: Movie, position: Int) -> Unit,
        onLiveClick: (LiveXtream, Int) -> Unit,
        onCategoryVisible: (Int) -> Unit,
        onLoadMoreLive: (() -> Unit)? = null,
        goOpenLive: (() -> Unit)? = null,
        goOpenMovie: ((item: XtreamCategory) -> Unit)? = null,
        goOpenSeries: ((item: XtreamCategory) -> Unit)?

    ) {
        this.onSeriesClick = onSeriesClick
        this.onMovieClick = onMovieClick
        this.onLiveClick = onLiveClick
        this.onCategoryVisible = onCategoryVisible
        this.onLoadMoreLive = onLoadMoreLive
        this.goOpenLive = goOpenLive
        this.goOpenMovie = goOpenMovie
        this.goOpenSeries = goOpenSeries
    }

//    override fun onViewAttachedToWindow(holder: BaseViewHolder<ItemCategoryMovieBinding>) {
//        super.onViewAttachedToWindow(holder)
//
//        val position = holder.bindingAdapterPosition
//        if (position == RecyclerView.NO_POSITION) return
//
//        val category = getItem(position)
//        val state = moviesState[category.categoryId]
//
//        if (
//            holder.isViewVisible() &&
//            state is CategoryMoviesState.NotLoaded
//        ) {
//            onCategoryVisible?.invoke(category.categoryId)
//        }
//    }
}