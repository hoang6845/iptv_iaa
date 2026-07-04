package com.iptvplayer.m3u.stream.ui.xtream

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.iptvplayer.m3u.stream.R
import com.iptvplayer.m3u.stream.model.entity.CarouselItem
import com.iptvplayer.m3u.stream.model.entity.Movie

class CarouselContainerAdapter(
    private val onClick: (Movie) -> Unit
) : RecyclerView.Adapter<CarouselContainerAdapter.VH>() {

    private var items: List<CarouselItem> = emptyList()
    private var recycledViewPool: RecyclerView.RecycledViewPool? = null

    fun submitList(list: List<CarouselItem>) {
        items = list
        notifyItemChanged(0)
    }

    override fun getItemCount() = 1 // luôn 1 item chứa carousel

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_carousel_container, parent, false)
        return VH(view, onClick)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(items)
    }

    class VH(
        itemView: View,
        private val onClick: (Movie) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        private val innerRv: RecyclerView = itemView.findViewById(R.id.rvCarousel)
        private val carouselAdapter = CarouselMovieAdapter(onClick)
        private var isSetup = false

        init {
            setupCarousel()
        }

        private fun setupCarousel() {
            if (isSetup) return
            isSetup = true

            innerRv.apply {
                layoutManager = CarouselLinearLayoutManager(
                    context = itemView.context,
                    shrinkAmount = 0.35f,
                    shrinkDistance = 0.8f
                )
                CenterSnapHelper().attachToRecyclerView(this)
                clipToPadding = false
                val paddingPx = (resources.displayMetrics.density * 24).toInt()
                setPadding(paddingPx, 0, paddingPx, 0)
                addItemDecoration(CarouselItemDecoration(spacingDp = 4))
                overScrollMode = View.OVER_SCROLL_NEVER
                adapter = carouselAdapter
            }
        }

        fun bind(items: List<CarouselItem>) {
            carouselAdapter.submitList(items)
            if (items.size > 1) {
                innerRv.post { innerRv.smoothScrollToPosition(1) }
            }
        }
    }
}