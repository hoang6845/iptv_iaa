package com.iptvplayer.m3u.stream.ui.recent_open

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.net.toUri
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.iptvplayer.m3u.stream.R
import com.iptvplayer.m3u.stream.databinding.ItemNowPlayingBinding
import com.iptvplayer.m3u.stream.model.entity.FavouriteChannel
import com.iptvplayer.m3u.stream.model.entity.RecentChannel
import com.iptvplayer.m3u.stream.utils.visible
import hoang.dqm.codebase.utils.loadImage
import hoang.dqm.codebase.utils.loadImageSketch

class RecentNowPlayingAdapter : RecyclerView.Adapter<RecentNowPlayingAdapter.VH>() {

    private var channel: RecentChannel? = null
    private var favouriteList: List<FavouriteChannel> = emptyList()
    private var onFavouriteClick: ((RecentChannel) -> Unit)? = null

    fun setOnFavouriteClick(block: (RecentChannel) -> Unit) {
        onFavouriteClick = block
    }

    fun setChannel(channel: RecentChannel?, favList: List<FavouriteChannel>) {
        val wasEmpty = itemCount == 0
        this.channel = channel
        this.favouriteList = favList
        if (wasEmpty && channel != null) {
            notifyItemInserted(0)
        } else {
            notifyDataSetChanged()
        }
    }

    inner class VH(val binding: ItemNowPlayingBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: RecentChannel) {
            val logo = item.channelIcon
            when {
                logo.isNullOrEmpty() -> {
                    val videoUri = item.url
                    if (videoUri.startsWith("content://") || videoUri.startsWith("file://")) {
                        Glide.with(binding.imgAvatar)
                            .load(videoUri.toUri())
                            .placeholder(R.drawable.img_movie_loading)
                            .error(R.drawable.img_movie_loading)
                            .centerCrop()
                            .into(binding.imgAvatar)
                    } else {
                        binding.imgAvatar.loadImage(R.drawable.img_movie_loading)
                    }
                }
                logo.startsWith("content://") || logo.startsWith("file://") -> {
                    Glide.with(binding.imgAvatar)
                        .load(logo.toUri())
                        .placeholder(R.drawable.img_movie_loading)
                        .error(R.drawable.img_movie_loading)
                        .centerCrop()
                        .into(binding.imgAvatar)
                }
                else -> binding.imgAvatar.loadImageSketch(logo, R.drawable.img_movie_loading)
            }

            binding.description.visible()
            binding.icLive.visible()
            binding.name.text = item.name

            val isFav = favouriteList.any {
                it.id == item.channelId && it.playlistId == item.playlistId
            }
            binding.favourite.setImageResource(if (isFav) R.drawable.favourite_open else R.drawable.favourite_open_default)

            binding.favourite.setOnClickListener { onFavouriteClick?.invoke(item) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = VH(
        ItemNowPlayingBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun onBindViewHolder(holder: VH, position: Int) {
        channel?.let { holder.bind(it) }
    }

    override fun getItemCount() = if (channel != null) 1 else 0
}