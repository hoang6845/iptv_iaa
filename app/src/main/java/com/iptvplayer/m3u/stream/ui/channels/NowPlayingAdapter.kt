package com.iptvplayer.m3u.stream.ui.channels

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.iptvplayer.m3u.stream.R
import com.iptvplayer.m3u.stream.databinding.ItemNowPlayingBinding
import com.iptvplayer.m3u.stream.model.entity.Channel
import com.iptvplayer.m3u.stream.utils.visible
import hoang.dqm.codebase.utils.loadImageSketch

class NowPlayingAdapter : RecyclerView.Adapter<NowPlayingAdapter.ViewHolder>() {

    private var onFavouriteClick: ((Channel) -> Unit)? = null

    fun setOnClickItemAdapter(
        onFavourite: (Channel) -> Unit
    ) {
        this.onFavouriteClick = onFavourite
    }

    private var channel: Channel? = null

    fun setChannel(channel: Channel?) {
        this.channel = channel
        notifyDataSetChanged()
    }

    override fun getItemCount() = if (channel != null) 1 else 0

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemNowPlayingBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        channel?.let { holder.bind(it) }
    }

    inner class ViewHolder(private val binding: ItemNowPlayingBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: Channel) {
            binding.root.setBackgroundColor(
                binding.root.context.getColor(R.color.color_space)
            )
            item.logo?.let {
                binding.imgAvatar.loadImageSketch(it)
            } ?: binding.imgAvatar.setImageResource(R.drawable.img_movie_loading)
            binding.description.visible()
            binding.icLive.visible()
            binding.name.text = item.name
            binding.favourite.setImageResource(
                if (item.isFavourite) R.drawable.favourite_open else R.drawable.favourite_open_default
            )


            binding.favourite.setOnClickListener {
                onFavouriteClick?.invoke(item)
            }
        }
    }
}