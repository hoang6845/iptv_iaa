package com.iptvplayer.m3u.stream.ui.livextream

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.iptvplayer.m3u.stream.databinding.ItemLiveXtreamBinding
import com.iptvplayer.m3u.stream.model.entity.LiveXtream
import hoang.dqm.codebase.utils.loadImageSketch

class LiveXtreamAdapter : PagingDataAdapter<LiveXtream, LiveXtreamAdapter.ViewHolder>(DIFF_CALLBACK) {

    private var onItemClick: ((LiveXtream, Int) -> Unit)? = null

    fun setOnItemClick(listener: (LiveXtream, Int) -> Unit) {
        onItemClick = listener
    }

    companion object {
        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<LiveXtream>() {
            override fun areItemsTheSame(old: LiveXtream, new: LiveXtream): Boolean =
                old.streamId == new.streamId

            override fun areContentsTheSame(old: LiveXtream, new: LiveXtream): Boolean =
                old == new
        }
    }

    inner class ViewHolder(val binding: ItemLiveXtreamBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_ID.toInt()) {
                    getItem(position)?.let { item ->
                        onItemClick?.invoke(item, position)
                    }
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemLiveXtreamBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        getItem(position)?.let { item ->
            val img = item.streamIcon
            if (!img.isNullOrBlank()) {
                holder.binding.imgAvatar.loadImageSketch(img)
            }
            holder.binding.name.text = item.name
        }
    }
}