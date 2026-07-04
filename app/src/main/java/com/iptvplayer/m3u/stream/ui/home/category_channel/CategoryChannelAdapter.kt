package com.iptvplayer.m3u.stream.ui.home.category_channel

import com.iptvplayer.m3u.stream.databinding.ItemCategoryChannelBinding
import hoang.dqm.codebase.base.adapter.BaseRecyclerViewAdapter
import hoang.dqm.codebase.data.CategoryItemChannel
import hoang.dqm.codebase.utils.loadImageSketchNoThumb

class CategoryChannelAdapter: BaseRecyclerViewAdapter<CategoryItemChannel, ItemCategoryChannelBinding>() {
    override fun bindData(
        binding: ItemCategoryChannelBinding,
        item: CategoryItemChannel,
        position: Int
    ) {
        binding.icCategory.loadImageSketchNoThumb(toIconPath(item.type))
        binding.textChannel.text = item.value
        binding.numChannels.text = item.num.toString()
    }

    fun toIconPath(name: String): String {
        return "ic_category_iaa/ic_${name.lowercase()}.png"
    }

    fun setOnClickAdapter(listener: (item: CategoryItemChannel, position: Int) -> Unit){
        setOnClickItemListener = listener
    }

}