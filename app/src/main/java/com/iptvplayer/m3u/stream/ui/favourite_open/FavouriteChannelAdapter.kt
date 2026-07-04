package com.iptvplayer.m3u.stream.ui.favourite_open

import androidx.core.net.toUri
import com.bumptech.glide.Glide
import com.iptvplayer.m3u.stream.R
import com.iptvplayer.m3u.stream.databinding.ItemChannelBarBinding
import com.iptvplayer.m3u.stream.model.entity.FavouriteChannel
import hoang.dqm.codebase.base.adapter.BaseRecyclerViewAdapter
import hoang.dqm.codebase.utils.loadImage
import hoang.dqm.codebase.utils.loadImageSketch

class FavouriteChannelAdapter :
    BaseRecyclerViewAdapter<FavouriteChannel, ItemChannelBarBinding>() {

    private val favouriteSet = mutableSetOf<String>()

    // Gọi khi load lần đầu
    fun setFavouriteSnapshot(list: List<FavouriteChannel>) {
        favouriteSet.clear()
        list.forEach { favouriteSet.add("${it.id}_${it.playlistId}") }
    }

    // Gọi khi toggle — chỉ update set, không setList lại
    fun updateFavouriteStates(favSet: Set<String>) {
        favouriteSet.clear()
        favouriteSet.addAll(favSet)
        notifyDataSetChanged()
    }
    private fun isFav(item: FavouriteChannel) =
        favouriteSet.contains("${item.id}_${item.playlistId}")
    override fun bindData(
        binding: ItemChannelBarBinding,
        item: FavouriteChannel,
        position: Int
    ) {
        val logo = item.logo
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

        binding.name.text = item.name

        val fav = isFav(item)
        binding.favourite.setImageResource(if (fav) R.drawable.favourite_open else R.drawable.favourite_open_default)

        binding.favourite.setOnClickListener { onFavouriteClick?.invoke(item, position) }
    }

    private var onFavouriteClick: ((FavouriteChannel, Int) -> Unit)? = null

    fun setOnClickListener(
        onClick: (FavouriteChannel, Int) -> Unit,
        onFavouriteClick: (FavouriteChannel, Int) -> Unit
    ) {
        setOnClickItemListener = onClick
        this.onFavouriteClick = onFavouriteClick
    }
}