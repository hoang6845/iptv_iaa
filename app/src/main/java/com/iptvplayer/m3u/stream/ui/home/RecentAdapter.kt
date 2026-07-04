package com.iptvplayer.m3u.stream.ui.home

import com.iptvplayer.m3u.stream.databinding.ItemRecentChannelBinding
import com.iptvplayer.m3u.stream.model.entity.RecentChannel
import hoang.dqm.codebase.base.adapter.BaseRecyclerViewAdapter
import hoang.dqm.codebase.utils.loadImageSketch

class RecentAdapter: BaseRecyclerViewAdapter<RecentChannel, ItemRecentChannelBinding>() {
    override fun bindData(
        binding: ItemRecentChannelBinding,
        item: RecentChannel,
        position: Int
    ) {
        item.channelIcon?.let {
            binding.imgAvatar.loadImageSketch(it)
        }
    }

    fun setOnClickItemAdapter(listener: (item: RecentChannel, position: Int) -> Unit) {
        setOnClickItemListener = listener
    }
}