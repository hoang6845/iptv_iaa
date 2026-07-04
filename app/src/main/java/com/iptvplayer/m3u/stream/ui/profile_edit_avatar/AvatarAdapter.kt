package com.iptvplayer.m3u.stream.ui.profile_edit_avatar

import androidx.core.view.isVisible
import com.iptvplayer.m3u.stream.databinding.ItemAvatarBinding
import com.iptvplayer.m3u.stream.model.entity.Avatar
import hoang.dqm.codebase.base.adapter.BaseRecyclerViewAdapter
import hoang.dqm.codebase.utils.loadImageSketch

class AvatarAdapter: BaseRecyclerViewAdapter<Avatar, ItemAvatarBinding>() {
    var positionSelected: Int = 0
    fun setPosition(newPosition: Int){
        val old = positionSelected
        positionSelected = newPosition
        notifyItemChanged(old)
        notifyItemChanged(positionSelected)
    }
    fun getAvatarSelected(): String {
        return dataList.get(positionSelected).url
    }
    override fun bindData(
        binding: ItemAvatarBinding,
        item: Avatar,
        position: Int
    ) {
        binding.img.loadImageSketch(item.url, isFull = false)
        binding.avatarSelected.isVisible = position == positionSelected
    }

    fun onClickItem(listener: (item: Avatar, position: Int) -> Unit){
        setOnClickItemListener = listener

    }
}