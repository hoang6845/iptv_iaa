package com.iptvplayer.m3u.stream.ui.add_playlist.import_link

import com.iptvplayer.m3u.stream.databinding.ItemCategoryUrlBinding
import hoang.dqm.codebase.base.adapter.BaseRecyclerViewAdapter
import hoang.dqm.codebase.data.CategoryItemUrl

class CategoryAdapter : BaseRecyclerViewAdapter<CategoryItemUrl, ItemCategoryUrlBinding>() {
    var isEnabled = true
    private var selectedPosition: Int = -1  // -1 = chưa chọn item nào

    fun setEnabledNow(enabled: Boolean) {
        isEnabled = enabled
        notifyDataSetChanged()
    }

    fun clearSelection() {
        selectedPosition = -1
        notifyDataSetChanged()
    }

    override fun bindData(
        binding: ItemCategoryUrlBinding,
        item: CategoryItemUrl,
        position: Int
    ) {
        binding.category.text = item.value
        binding.link.text = item.url
        val alpha = when {
            !isEnabled -> 0.4f
            selectedPosition == -1 || selectedPosition == position -> 1f
            else -> 0.4f
        }
        binding.root.alpha = alpha

        binding.icon.setOnClickListener {
            copyClick?.invoke(item, position)
        }
    }

    var copyClick: ((CategoryItemUrl, Int) -> Unit)? = null

    fun onClickItem(
        listener: (CategoryItemUrl, Int) -> Unit,
        copyClick: (CategoryItemUrl, Int) -> Unit
    ) {
        setOnClickItemListener = { item, position ->
            selectedPosition = position
            notifyDataSetChanged()
            listener(item, position)
        }
        this.copyClick = copyClick
    }
}