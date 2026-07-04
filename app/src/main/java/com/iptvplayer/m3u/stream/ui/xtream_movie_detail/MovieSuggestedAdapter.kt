package com.iptvplayer.m3u.stream.ui.xtream_movie_detail

import com.iptvplayer.m3u.stream.databinding.ItemMovieSuggestedBinding
import com.iptvplayer.m3u.stream.model.entity.Movie
import hoang.dqm.codebase.base.adapter.BaseRecyclerViewAdapter
import hoang.dqm.codebase.utils.loadImageSketch

class MovieSuggestedAdapter: BaseRecyclerViewAdapter<Movie, ItemMovieSuggestedBinding>() {
    override fun bindData(
        binding: ItemMovieSuggestedBinding,
        item: Movie,
        position: Int
    ) {
        item.streamIcon?.let {
            binding.ivMoviePoster.loadImageSketch(it)
        }
        binding.tvMovieTitle.text = item.name
    }

    fun setOnClick(onMovieClick: ((item: Movie, position: Int) -> Unit)){
        setOnClickItemListener = onMovieClick
    }
}