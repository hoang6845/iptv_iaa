package com.iptvplayer.m3u.stream.ui.xtream

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

class CarouselItemDecoration(private val spacingDp: Int) : RecyclerView.ItemDecoration() {
    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        val px = (view.resources.displayMetrics.density * spacingDp).toInt()
        outRect.left = px
        outRect.right = px
    }
}