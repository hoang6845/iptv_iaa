package com.iptvplayer.m3u.stream.ui.xtream

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.iptvplayer.m3u.stream.R
import com.iptvplayer.m3u.stream.model.entity.CarouselItem
import com.iptvplayer.m3u.stream.model.entity.Movie
import hoang.dqm.codebase.utils.loadImageSketch

class CarouselMovieAdapter(
    private val onClick: (Movie) -> Unit
) : RecyclerView.Adapter<CarouselMovieAdapter.VH>() {

    private val items = mutableListOf<CarouselItem>()

    fun submitList(list: List<CarouselItem>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun getItemCount() = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_carousel_movie, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(items[position], onClick)
    }

    class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imgPoster: ImageView = itemView.findViewById(R.id.img_poster)

        fun bind(item: CarouselItem, onClick: (Movie) -> Unit) {
            val imageUrl = item.backdropUrl.takeIf { !it.isNullOrBlank() }
                ?: item.movie.streamIcon.takeIf { !it.isNullOrBlank() }

            imageUrl?.let {
                imgPoster.loadImageSketch(it)
            }

            itemView.setOnClickListener { onClick(item.movie) }
        }
    }



}