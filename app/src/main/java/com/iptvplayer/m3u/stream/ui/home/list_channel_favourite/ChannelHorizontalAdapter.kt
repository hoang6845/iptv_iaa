package com.iptvplayer.m3u.stream.ui.home.list_channel_favourite

import android.content.res.ColorStateList
import com.iptvplayer.m3u.stream.R
import com.iptvplayer.m3u.stream.databinding.ItemChannelHorizontalBinding
import com.iptvplayer.m3u.stream.model.entity.Channel
import hoang.dqm.codebase.base.adapter.BaseRecyclerViewAdapter
import hoang.dqm.codebase.utils.loadImage
import hoang.dqm.codebase.utils.loadImageSketch
import androidx.core.graphics.toColorInt
import androidx.core.net.toUri
import com.bumptech.glide.Glide

class ChannelHorizontalAdapter: BaseRecyclerViewAdapter<Channel, ItemChannelHorizontalBinding>() {
    val mapPlaylist = mutableMapOf<String, Long>()

    fun setMap(value: Map<String, Long>) {
        mapPlaylist.clear()
        mapPlaylist.putAll(value)
    }
    override fun bindData(
        binding: ItemChannelHorizontalBinding,
        item: Channel,
        position: Int
    ) {
        val logo = item.logo
        when {
            logo.isNullOrEmpty() -> {
                // Không có logo → thử load thumbnail từ URL video (nếu là local URI)
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
        binding.textChannel.text = item.name
        binding.favourite.setOnClickListener {
            // click favourite implements
            onFavouriteClick?.invoke(item, position)
        }
        binding.favourite.setImageResource(
            if (item.isFavourite) {
                binding.favourite.imageTintList = ColorStateList.valueOf(binding.root.context.getColor(
                    hoang.dqm.codebase.R.color.colorRed500))
                R.drawable.favourited
            }
            else {
                binding.favourite.imageTintList = ColorStateList.valueOf("#ffffff".toColorInt())
                R.drawable.favourite
            }
        )
    }

    private var onFavouriteClick: ((item: Channel, position: Int) -> Unit)? = null

    fun setOnClickListener(listener: (item: Channel, position: Int) -> Unit, onFavouriteClick: (item: Channel, position: Int) -> Unit) {
        setOnClickItemListener = listener
        this.onFavouriteClick = onFavouriteClick
    }
}