package com.iptvplayer.m3u.stream.ui.xtream_home

import com.iptvplayer.m3u.stream.databinding.ItemXtreamServerBinding
import com.iptvplayer.m3u.stream.model.entity.XtreamAuth
import hoang.dqm.codebase.base.adapter.BaseRecyclerViewItemAdapter
import hoang.dqm.codebase.utils.loadImageSketch

class XtreamAdapter: BaseRecyclerViewItemAdapter<XtreamAuth, ItemXtreamServerBinding>() {

    override fun bindData(
        binding: ItemXtreamServerBinding,
        item: XtreamAuth,
        position: Int
    ) {
        binding.name.text = item.name
        binding.avatar.loadImageSketch(item.urlAvatar, isFull = false)
    }


    fun setOnClickItem(listener: (position: Int) -> Unit, placeholderCallback: () -> Unit) {
        setOnClickItemRecyclerView { pattern, position ->
            listener.invoke(position)
        }
        onPlaceholderClickListener = placeholderCallback
    }
}