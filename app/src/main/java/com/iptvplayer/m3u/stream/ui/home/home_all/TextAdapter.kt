package com.iptvplayer.m3u.stream.ui.home.home_all

import com.iptvplayer.m3u.stream.databinding.ItemLineGuideBinding
import hoang.dqm.codebase.base.adapter.BaseRecyclerViewAdapter

class TextAdapter: BaseRecyclerViewAdapter<String, ItemLineGuideBinding>() {
    override fun bindData(
        binding: ItemLineGuideBinding,
        item: String,
        position: Int
    ) {
        binding.textContent.text = item
    }
}