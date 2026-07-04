package com.iptvplayer.m3u.stream.ui.movie_open

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.iptvplayer.m3u.stream.databinding.ItemMovieOpenBinding
import com.iptvplayer.m3u.stream.model.entity.SearchResult
import hoang.dqm.codebase.utils.loadImageSketch


class MovieOpenAdapter :
    PagingDataAdapter<SearchResult, MovieOpenAdapter.ViewHolder>(DIFF_CALLBACK) {

    private var onItemClick: ((SearchResult, Int) -> Unit)? = null

    fun setOnItemClick(listener: (SearchResult, Int) -> Unit) {
        onItemClick = listener
    }
    var isShowName: Boolean = false
    fun setShowNameNow(show: Boolean){
        if (isShowName == show) return
        isShowName= show
        notifyDataSetChanged()
    }
    companion object {
        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<SearchResult>() {
            override fun areItemsTheSame(
                oldItem: SearchResult,
                newItem: SearchResult
            ): Boolean = oldItem.uniqueId == newItem.uniqueId

            override fun areContentsTheSame(
                oldItem: SearchResult,
                newItem: SearchResult
            ): Boolean = oldItem == newItem
        }
    }

    inner class ViewHolder(val binding: ItemMovieOpenBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    getItem(position)?.let { item ->
                        onItemClick?.invoke(item, position)
                    }
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemMovieOpenBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        getItem(position)?.let { item ->
            val img = item.image
            if (!img.isNullOrBlank()) {
                holder.binding.ivMoviePoster.loadImageSketch(img)
            }
            holder.binding.tvMovieTitle.text = item.name
            holder.binding.nameMovie.text = item.name
            holder.binding.nameMovie.isVisible = isShowName

        }
    }
}