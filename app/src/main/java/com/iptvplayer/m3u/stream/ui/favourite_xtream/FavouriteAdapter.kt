package com.iptvplayer.m3u.stream.ui.favourite_xtream

import com.iptvplayer.m3u.stream.databinding.ItemFavouriteBinding
import com.iptvplayer.m3u.stream.model.entity.SearchResult
import hoang.dqm.codebase.base.adapter.BaseRecyclerViewAdapter
import hoang.dqm.codebase.utils.loadImageSketch

class FavouriteAdapter: BaseRecyclerViewAdapter<SearchResult, ItemFavouriteBinding>() {
    override fun bindData(
        binding: ItemFavouriteBinding,
        item: SearchResult,
        position: Int
    ) {
        val img = item.image
        if (!img.isNullOrBlank()) {
            binding.imgAvatar.loadImageSketch(img)
        }
        binding.tvChannelName.text = item.name
    }

    private var removeFavourite: ((item: SearchResult, position: Int) -> Unit)?= null

    fun setOnClickItem(listener: (item: SearchResult, position: Int) -> Unit, deleteItem: (item: SearchResult, position: Int) -> Unit){
        setOnClickItemListener = listener
        this.removeFavourite = deleteItem

    }
}