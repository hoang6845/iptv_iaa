package com.iptvplayer.m3u.stream.ui.iptv_live

import com.iptvplayer.m3u.stream.databinding.ItemChannelLiveBinding
import com.iptvplayer.m3u.stream.model.entity.ChannelPopular
import hoang.dqm.codebase.base.adapter.BaseRecyclerViewAdapter
import hoang.dqm.codebase.utils.loadImageSketch

class ChannelLiveAdapter: BaseRecyclerViewAdapter<ChannelPopular, ItemChannelLiveBinding>() {
    var currentChannelSelected: Int = 0
    fun setSelectedPosition(position: Int) {
        val oldPosition = currentChannelSelected
        currentChannelSelected = position
        notifyItemChanged(oldPosition)
        notifyItemChanged(currentChannelSelected)
    }
    override fun bindData(
        binding: ItemChannelLiveBinding,
        item: ChannelPopular,
        position: Int
    ) {
        item.logo?.let {
            binding.imgAvatar.loadImageSketch(it)
        }
//        binding.tvChannelName.text = item.name
    }

    fun setOnClickItemAdapter(listener: (item: ChannelPopular, position: Int) -> Unit) {
        setOnClickItemListener = listener
    }
}