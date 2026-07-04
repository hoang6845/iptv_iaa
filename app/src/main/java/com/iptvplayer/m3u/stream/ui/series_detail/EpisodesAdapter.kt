package com.iptvplayer.m3u.stream.ui.series_detail

import com.iptvplayer.m3u.stream.databinding.ItemSeriesBinding
import com.iptvplayer.m3u.stream.model.entity.Episode
import hoang.dqm.codebase.base.adapter.BaseRecyclerViewAdapter
import hoang.dqm.codebase.utils.loadImageSketch

class EpisodesAdapter: BaseRecyclerViewAdapter<Episode, ItemSeriesBinding>() {
    override fun bindData(
        binding: ItemSeriesBinding,
        item: Episode,
        position: Int
    ) {
        item.info?.movieImage?.let {
            binding.ivMoviePoster.loadImageSketch(it)
        }
        binding.tvMovieTitle.text = item.title
    }

    fun setOnClickItem(listener: (item: Episode, position: Int) -> Unit){
        setOnClickItemListener = listener
    }
}