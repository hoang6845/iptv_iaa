package com.iptvplayer.m3u.stream.ui.xtream

import com.iptvplayer.m3u.stream.databinding.ItemMovieSuggestedBinding
import com.iptvplayer.m3u.stream.model.entity.Series
import hoang.dqm.codebase.base.adapter.BaseRecyclerViewAdapter
import hoang.dqm.codebase.utils.loadImageSketch

class SeriesAdapter : BaseRecyclerViewAdapter<Series, ItemMovieSuggestedBinding>() {
    override fun bindData(
        binding: ItemMovieSuggestedBinding,
        item: Series,
        position: Int
    ) {
        item.cover?.let {
            binding.ivMoviePoster.loadImageSketch(it)
        }
        binding.tvMovieTitle.text = item.name
    }

    fun setOnClick(onMovieClick: ((item: Series, position: Int) -> Unit)) {
        setOnClickItemListener = onMovieClick
    }
}