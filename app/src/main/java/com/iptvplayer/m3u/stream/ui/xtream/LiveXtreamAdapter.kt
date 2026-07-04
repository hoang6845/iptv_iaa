package com.iptvplayer.m3u.stream.ui.xtream

import com.iptvplayer.m3u.stream.databinding.ItemLiveXtreamHorizontalBinding
import com.iptvplayer.m3u.stream.model.entity.LiveXtream
import hoang.dqm.codebase.base.adapter.BaseRecyclerViewAdapter
import hoang.dqm.codebase.utils.loadImageSketch

class LiveXtreamAdapter : BaseRecyclerViewAdapter<LiveXtream, ItemLiveXtreamHorizontalBinding>() {
    override fun bindData(
        binding: ItemLiveXtreamHorizontalBinding,
        item: LiveXtream,
        position: Int
    ) {
        val img = item.streamIcon
        if (!img.isNullOrBlank()) {
            binding.imgAvatar.loadImageSketch(img)
        }
        binding.name.text = item.name
    }

    fun setOnClick(onMovieClick: ((item: LiveXtream, position: Int) -> Unit)) {
        setOnClickItemListener = onMovieClick
    }
}