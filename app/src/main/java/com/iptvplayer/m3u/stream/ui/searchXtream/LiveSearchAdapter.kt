package com.iptvplayer.m3u.stream.ui.searchXtream

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.iptvplayer.m3u.stream.databinding.ItemLiveXtreamBinding
import com.iptvplayer.m3u.stream.model.entity.SearchResult
import hoang.dqm.codebase.utils.loadImageSketch

class LiveSearchAdapter:  PagingDataAdapter<SearchResult, LiveSearchAdapter.ViewHolder>(
    DIFF_CALLBACK
) {
    private var onItemClick: ((SearchResult, Int) -> Unit)? = null

    fun setOnItemClick(listener: (SearchResult, Int) -> Unit) {
        onItemClick = listener
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

    inner class ViewHolder(val binding: ItemLiveXtreamBinding) :
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
        val binding = ItemLiveXtreamBinding.inflate(
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
                holder.binding.imgAvatar.loadImageSketch(img)
            }
            holder.binding.name.text = item.name
        }
    }
}