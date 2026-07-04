package com.iptvplayer.m3u.stream.ui.live_open

import android.content.Context
import android.content.res.ColorStateList
import android.util.TypedValue
import androidx.core.content.ContextCompat
import androidx.core.graphics.toColorInt
import com.iptvplayer.m3u.stream.R
import com.iptvplayer.m3u.stream.databinding.ItemCategoryBinding
import com.iptvplayer.m3u.stream.model.entity.XtreamCategory
import hoang.dqm.codebase.base.adapter.BaseRecyclerViewAdapter

class CategoryTabAdapter : BaseRecyclerViewAdapter<XtreamCategory, ItemCategoryBinding>() {
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
        item: XtreamCategory,
        position: Int
    ) {
        binding.tvCategory.text = item.categoryName
        if (position == isPositionSelected) {
            binding.cardCategory.setBackgroundResource(R.drawable.tab_corner)
            binding.tvCategory.setTextColor("#ffffff".toColorInt())
        } else {
            binding.cardCategory.setBackgroundResource(R.drawable.shape_category)
            binding.tvCategory.setTextColor("#64748B".toColorInt())
        }
    }

    fun setOnClickItemAdapter(listener: (item: XtreamCategory, position: Int) -> Unit) {
        setOnClickItemListener = listener
    }
    fun Context.getAttrColor(attr: Int): Int {
        val typedValue = TypedValue()
        theme.resolveAttribute(attr, typedValue, true)
        return typedValue.data
    }
}