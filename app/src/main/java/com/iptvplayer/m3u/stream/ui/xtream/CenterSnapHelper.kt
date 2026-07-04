package com.iptvplayer.m3u.stream.ui.xtream

import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.recyclerview.widget.RecyclerView

class CenterSnapHelper : LinearSnapHelper() {

    private var recyclerView: RecyclerView? = null

    override fun attachToRecyclerView(recyclerView: RecyclerView?) {
        super.attachToRecyclerView(recyclerView)
        this.recyclerView = recyclerView
    }

    override fun calculateDistanceToFinalSnap(
        layoutManager: RecyclerView.LayoutManager,
        targetView: View
    ): IntArray {
        val out = IntArray(2)
        if (layoutManager.canScrollHorizontally()) {
            out[0] = distanceToCenter(layoutManager, targetView)
        }
        return out
    }

    private fun distanceToCenter(
        layoutManager: RecyclerView.LayoutManager,
        targetView: View
    ): Int {
        val childCenter = layoutManager.getDecoratedLeft(targetView) +
                layoutManager.getDecoratedMeasuredWidth(targetView) / 2
        val containerCenter = recyclerView?.width?.let { it / 2 } ?: 0
        return childCenter - containerCenter
    }

    override fun findSnapView(layoutManager: RecyclerView.LayoutManager): View? {
        if (layoutManager !is LinearLayoutManager) return null
        val first = layoutManager.findFirstVisibleItemPosition()
        val last = layoutManager.findLastVisibleItemPosition()
        if (first.toLong() == RecyclerView.NO_ID) return null

        val containerCenter = recyclerView?.width?.let { it / 2f } ?: return null
        var closestView: View? = null
        var closestDistance = Float.MAX_VALUE

        for (i in first..last) {
            val child = layoutManager.findViewByPosition(i) ?: continue
            val childCenter = layoutManager.getDecoratedLeft(child) +
                    layoutManager.getDecoratedMeasuredWidth(child) / 2f
            val distance = Math.abs(childCenter - containerCenter)
            if (distance < closestDistance) {
                closestDistance = distance
                closestView = child
            }
        }
        return closestView
    }
}