package com.iptvplayer.m3u.stream.ui.live_open

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import  com.iptvplayer.m3u.stream.R
import  com.iptvplayer.m3u.stream.databinding.ItemLiveNowPlayingBinding
import  com.iptvplayer.m3u.stream.model.entity.LiveXtream
import  com.iptvplayer.m3u.stream.utils.visible
import hoang.dqm.codebase.utils.loadImage
import hoang.dqm.codebase.utils.loadImageSketch

class NowPlayingAdapter : RecyclerView.Adapter<NowPlayingAdapter.ViewHolder>() {
    private var channel: LiveXtream? = null

    fun setLiveXtream(channel: LiveXtream?) {
        this.channel = channel
        notifyDataSetChanged()
    }

    override fun getItemCount() = if (channel != null) 1 else 0

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemLiveNowPlayingBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        channel?.let { holder.bind(it) }
    }

    inner class ViewHolder(private val binding: ItemLiveNowPlayingBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: LiveXtream) {
            binding.root.setBackgroundColor(
                binding.root.context.getColor(R.color.color_space)
            )
            if (item.streamIcon.isNotEmpty()) {
                binding.imgAvatar.loadImageSketch(item.streamIcon)
            } else {
                binding.imgAvatar.loadImage(R.drawable.img_live_loading)
            }
            binding.description.visible()
            binding.icLive.visible()
            binding.name.text = item.name
        }
    }
}