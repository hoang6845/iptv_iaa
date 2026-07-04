package com.iptvplayer.m3u.stream.ui.xtream_movie_detail

import com.iptvplayer.m3u.stream.R
import com.iptvplayer.m3u.stream.databinding.ItemStarBinding
import hoang.dqm.codebase.base.adapter.BaseRecyclerViewAdapter

class StarAdapter : BaseRecyclerViewAdapter<Boolean, ItemStarBinding>() {
    override fun bindData(
        binding: ItemStarBinding,
        item: Boolean,
        position: Int
    ) {
        binding.star.setImageResource(
            if (item) R.drawable.ic_star
            else R.drawable.ic_no_star
        )
    }
}