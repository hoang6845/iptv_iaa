package com.iptvplayer.m3u.stream.ui.xtream

import android.content.Context
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class CarouselLinearLayoutManager(
    context: Context,
    private val shrinkAmount: Float = 0.25f,
    private val shrinkDistance: Float = 0.9f
) : LinearLayoutManager(context, HORIZONTAL, false) {

    init {
        initialPrefetchItemCount = 4
    }

    override fun onLayoutCompleted(state: RecyclerView.State?) {
        super.onLayoutCompleted(state)
        scaleChildren()
    }

    override fun scrollHorizontallyBy(
        dx: Int,
        recycler: RecyclerView.Recycler,
        state: RecyclerView.State
    ): Int {
        val scrolled = super.scrollHorizontallyBy(dx, recycler, state)
        scaleChildren()
        return scrolled
    }

    private fun scaleChildren() {
        val midpoint = width / 2f

        for (i in 0 until childCount) {
            val child = getChildAt(i) ?: continue

            val childMidpoint = (getDecoratedLeft(child) + getDecoratedRight(child)) / 2f
            val distance = Math.abs(midpoint - childMidpoint) / midpoint
            val ratio = Math.min(distance / shrinkDistance, 1f)

            val scale = 1f - shrinkAmount * ratio
            val alpha = 1f - 0.4f * ratio

            child.scaleX = scale
            child.scaleY = scale
            child.alpha = alpha

            val childWidth = getDecoratedMeasuredWidth(child)
            val gap = childWidth * (1f - scale) / 2f
            val direction = if (childMidpoint > midpoint) -1f else 1f
            child.translationX = gap * direction * ratio
        }
    }

    override fun getExtraLayoutSpace(state: RecyclerView.State): Int = 500
}