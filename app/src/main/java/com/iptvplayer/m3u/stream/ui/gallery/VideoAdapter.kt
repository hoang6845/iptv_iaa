package com.iptvplayer.m3u.stream.ui.gallery

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.iptvplayer.m3u.stream.databinding.ItemVideoBinding
import com.iptvplayer.m3u.stream.model.entity.Channel
import androidx.core.net.toUri

class VideoAdapter(
    private val onRemove: (Channel) -> Unit
) : ListAdapter<Channel, VideoAdapter.VideoViewHolder>(DIFF) {

    inner class VideoViewHolder(private val binding: ItemVideoBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(channel: Channel) {
            Glide.with(binding.imgThumbnail)
                .load(channel.url.toUri())
                .centerCrop()
                .into(binding.imgThumbnail)

            binding.btnRemove.setOnClickListener { onRemove(channel) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoViewHolder {
        val binding = ItemVideoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VideoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: VideoViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<Channel>() {
            override fun areItemsTheSame(a: Channel, b: Channel) = a.id == b.id
            override fun areContentsTheSame(a: Channel, b: Channel) = a == b
        }
    }
}