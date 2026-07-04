package com.iptvplayer.m3u.stream.ui.searchXtream

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.iptvplayer.m3u.stream.databinding.ItemMovieBinding
import com.iptvplayer.m3u.stream.model.entity.SearchResult
import hoang.dqm.codebase.utils.loadImageSketch

class FavouriteAdapter : ListAdapter<SearchResult, FavouriteAdapter.ViewHolder>(DIFF_CALLBACK) {

    private var onItemClick: ((SearchResult, Int) -> Unit)? = null

    fun setOnItemClick(listener: (SearchResult, Int) -> Unit) {
        onItemClick = listener
    }

    companion object {
        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<SearchResult>() {
            override fun areItemsTheSame(oldItem: SearchResult, newItem: SearchResult) =
                oldItem.uniqueId == newItem.uniqueId

            override fun areContentsTheSame(oldItem: SearchResult, newItem: SearchResult) =
                oldItem == newItem
        }
    }

    inner class ViewHolder(val binding: ItemMovieBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_ID.toInt()) {
                    onItemClick?.invoke(getItem(position), position)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(
        ItemMovieBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        holder.binding.tvMovieTitle.text = item.name
        item.image?.takeIf { it.isNotBlank() }?.let {
            holder.binding.ivMoviePoster.loadImageSketch(it)
        }
    }
}