package com.iptvplayer.m3u.stream.ui.recent_open


import android.content.res.ColorStateList
import androidx.core.graphics.toColorInt
import androidx.core.net.toUri
import com.bumptech.glide.Glide
import com.iptvplayer.m3u.stream.R
import com.iptvplayer.m3u.stream.databinding.ItemChannelBinding
import com.iptvplayer.m3u.stream.model.entity.FavouriteChannel
import com.iptvplayer.m3u.stream.model.entity.RecentChannel
import com.iptvplayer.m3u.stream.utils.gone
import hoang.dqm.codebase.base.adapter.BaseRecyclerViewAdapter
import hoang.dqm.codebase.utils.loadImage
import hoang.dqm.codebase.utils.loadImageSketch

class RecentChannelAdapter: BaseRecyclerViewAdapter<RecentChannel, ItemChannelBinding>() {
    var listFavouriteChannel: List<FavouriteChannel> = emptyList()
    fun setChannelFavourite(favList: List<FavouriteChannel>) {
        this.listFavouriteChannel = favList
        notifyDataSetChanged()
    }
    override fun bindData(
        binding: ItemChannelBinding,
        item: RecentChannel,
        position: Int
    ) {
        val logo = item.channelIcon

        when {
            logo.isNullOrEmpty() -> {
                val videoUri = item.url
                if (videoUri.startsWith("content://") || videoUri.startsWith("file://")) {
                    binding.favourite.gone()
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

        binding.name.text = item.name
        binding.favourite.setOnClickListener {
            // click favourite implements
            onFavouriteClick?.invoke(item, position)
        }
        binding.tabBottom.gone()
        binding.favourite.setImageResource(
            if (listFavouriteChannel.any { it -> it.id == item.channelId && it.playlistId == item.playlistId }) {

                R.drawable.favourite_open
            } else {
                R.drawable.favourite_open_default
            }
        )
    }

    private var onFavouriteClick: ((item: RecentChannel, position: Int) -> Unit)? = null
    private var onRemoveClick: ((item: RecentChannel, position: Int) -> Unit)? = null

    fun setOnClickListener(listener: (item: RecentChannel, position: Int) -> Unit, onFavouriteClick: (item: RecentChannel, position: Int) -> Unit, onRemoveClick: (item: RecentChannel, position: Int) -> Unit) {
        setOnClickItemListener = listener
        this.onFavouriteClick = onFavouriteClick
        this.onRemoveClick = onRemoveClick
    }
}