package com.iptvplayer.m3u.stream.ui.xtream

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.graphics.toColorInt
import androidx.recyclerview.widget.RecyclerView
import com.iptvplayer.m3u.stream.R
import com.iptvplayer.m3u.stream.model.entity.Movie
import hoang.dqm.codebase.utils.loadImageSketch

class SuggestMovieHeaderAdapter(
    private val onClick: ((item: Movie) -> Unit)? = null,
    private val btnFavourite: ((item: Movie) -> Unit)? = null,
    private val btnPlay: ((item: Movie) -> Unit)? = null
) : RecyclerView.Adapter<SuggestMovieHeaderAdapter.VH>() {

    private var posterUrl: Movie? = null
    var isFavouriteMovie: Boolean = false

    fun setFavourite(value: Boolean) {
        isFavouriteMovie = value
        if (itemCount > 0)notifyItemChanged(0)
    }

    fun submit(movie: Movie?) {
        val hadItem = posterUrl != null
        posterUrl = movie
        val hasItem = posterUrl != null

        when {
            !hadItem && hasItem -> notifyItemInserted(0)
            hadItem && !hasItem -> notifyItemRemoved(0)
            hadItem && hasItem -> notifyItemChanged(0)
        }
    }

    override fun getItemCount(): Int = if (posterUrl == null) 0 else 1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.layout_suggest, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        posterUrl?.let {
            holder.bind(it, onClick, btnFavourite, btnPlay, isFavouriteMovie)
        }
    }

    class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val imgBg: ImageView = itemView.findViewById(R.id.img_bg)
        private val btnFavouriteView: ImageView = itemView.findViewById(R.id.btn_favourite)
        private val btnPlayView: TextView = itemView.findViewById(R.id.btn_play)
        private val tvName: TextView = itemView.findViewById(R.id.name_movie)
        private val tvCategory: TextView = itemView.findViewById(R.id.category)
        private val btnInfo: ImageView = itemView.findViewById(R.id.btn_info)


        fun bind(
            movie: Movie,
            onClick: ((Movie) -> Unit)?,
            btnFavourite: ((Movie) -> Unit)?,
            btnPlay: ((Movie) -> Unit)?,
            isFavouriteMovie: Boolean
        ) {

            movie.streamIcon?.let {
                imgBg.loadImageSketch(it)
            }

            tvName.text = movie.name
//            itemView.setOnClickListener {
//                onClick?.invoke(movie)
//            }

            btnFavouriteView.setOnClickListener {
                btnFavourite?.invoke(movie)
            }

            btnPlayView.setOnClickListener {
                onClick?.invoke(movie)
            }

            btnInfo.setOnClickListener {
                btnPlay?.invoke(movie)
            }

            btnFavouriteView.setImageResource(
                if (isFavouriteMovie) {
                    btnFavouriteView.imageTintList = ColorStateList.valueOf("#F64F45".toColorInt())
                    R.drawable.favourited
                } else {
                    btnFavouriteView.imageTintList = ColorStateList.valueOf("#ffffff".toColorInt())
                    R.drawable.favourite
                }
            )
        }
    }
}