package com.iptvplayer.m3u.stream.ui.channels

import android.content.Context
import android.content.res.ColorStateList
import android.util.TypedValue
import androidx.core.content.ContextCompat
import androidx.core.graphics.toColorInt
import com.iptvplayer.m3u.stream.R
import com.iptvplayer.m3u.stream.databinding.ItemCategoryBinding
import hoang.dqm.codebase.base.adapter.BaseRecyclerViewAdapter
import hoang.dqm.codebase.data.CategoryItem

class CategoryTabAdapter : BaseRecyclerViewAdapter<CategoryItem, ItemCategoryBinding>() {
    private var isPositionSelected: Int = 0
    fun setCategoriesSelected(value: Int) {
        val oldValue = isPositionSelected
        isPositionSelected = value
        notifyItemChanged(oldValue)
        notifyItemChanged(isPositionSelected)
    }

    fun getCategorySelectedPosition(): Int {
        return isPositionSelected
    }

    fun getCategorySelected(): String {
        return if (dataList.isNotEmpty()) getItem(isPositionSelected).type
        else "all"
    }

    override fun bindData(
        binding: ItemCategoryBinding,
        item: CategoryItem,
        position: Int
    ) {
        binding.tvCategory.text = item.value
        if (position == isPositionSelected) {
            binding.cardCategory.setBackgroundResource(R.drawable.tab_corner)
            binding.tvCategory.setTextColor("#ffffff".toColorInt())
        } else {
            binding.cardCategory.setBackgroundResource(R.drawable.shape_category_channel)
            binding.tvCategory.setTextColor("#ffffff".toColorInt())
        }
    }

    fun setOnClickItemAdapter(listener: (item: CategoryItem, position: Int) -> Unit) {
        setOnClickItemListener = listener
    }
    fun Context.getAttrColor(attr: Int): Int {
        val typedValue = TypedValue()
        theme.resolveAttribute(attr, typedValue, true)
        return typedValue.data
    }
}