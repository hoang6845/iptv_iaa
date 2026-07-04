package com.iptvplayer.m3u.stream.ui.channels

import android.content.Context
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.iptvplayer.m3u.stream.R
import com.iptvplayer.m3u.stream.databinding.ItemChannelBarBinding
import com.iptvplayer.m3u.stream.model.entity.Channel
import com.iptvplayer.m3u.stream.utils.gone
import hoang.dqm.codebase.utils.loadImage
import hoang.dqm.codebase.utils.loadImageSketch

class ChannelsAdapter : PagingDataAdapter<Channel, ChannelsAdapter.ViewHolder>(DIFF_CALLBACK) {

    private var onClickItem: ((Channel, Int) -> Unit)? = null
    private var onFavouriteClick: ((Channel) -> Unit)? = null

    fun setOnClickItemAdapter(
        onClick: (Channel, Int) -> Unit,
        onFavourite: (Channel) -> Unit
    ) {
        this.onClickItem = onClick
        this.onFavouriteClick = onFavourite
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemChannelBarBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        getItem(position)?.let { holder.bind(it, position) }
    }

    inner class ViewHolder(private val binding: ItemChannelBarBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: Channel, position: Int) {
            binding.root.setOnClickListener {
                onClickItem?.invoke(item, position)

            }
            item.logo?.let {
            binding.imgAvatar.loadImageSketch(it)
        } ?: run {
            binding.imgAvatar.loadImage(R.drawable.img_movie_loading)
        }
        binding.name.text = item.name
        item.group?.let {
            binding.description.text = it
        }?:run {
            binding.description.gone()
        }
            binding.favourite.setImageResource(
                if (item.isFavourite) {

                    R.drawable.favourite_open
                } else {
                    R.drawable.favourite_open_default
                }
            )
            binding.favourite.setOnClickListener {
                onFavouriteClick?.invoke(item)
            }
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Channel>() {
            override fun areItemsTheSame(old: Channel, new: Channel) = old.id == new.id
            override fun areContentsTheSame(old: Channel, new: Channel) = old == new
        }
    }

    fun Context.getAttrColor(attr: Int): Int {
        val typedValue = TypedValue()
        theme.resolveAttribute(attr, typedValue, true)
        return typedValue.data
    }
}