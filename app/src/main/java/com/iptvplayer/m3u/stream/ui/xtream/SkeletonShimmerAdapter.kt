package com.iptvplayer.m3u.stream.ui.xtream

import com.iptvplayer.m3u.stream.databinding.ItemMovieSkeletonShimmerBinding
import hoang.dqm.codebase.base.adapter.BaseRecyclerViewAdapter

class SkeletonShimmerAdapter: BaseRecyclerViewAdapter<Int, ItemMovieSkeletonShimmerBinding>() {
    override fun bindData(
        binding: ItemMovieSkeletonShimmerBinding,
        item: Int,
        position: Int
    ) {
    }
}