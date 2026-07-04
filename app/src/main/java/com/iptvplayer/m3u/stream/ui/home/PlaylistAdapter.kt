package com.iptvplayer.m3u.stream.ui.home

import android.content.Context
import android.graphics.Color
import android.util.TypedValue
import androidx.core.text.buildSpannedString
import androidx.core.text.color
import com.iptvplayer.m3u.stream.R
import com.iptvplayer.m3u.stream.databinding.ItemPlaylistBinding
import com.iptvplayer.m3u.stream.model.entity.PlaylistWithChannels
import com.iptvplayer.m3u.stream.utils.AppConstants
import hoang.dqm.codebase.base.adapter.BaseRecyclerViewAdapter

class PlaylistAdapter: BaseRecyclerViewAdapter<PlaylistWithChannels, ItemPlaylistBinding>() {
    override fun bindData(
        binding: ItemPlaylistBinding,
        item: PlaylistWithChannels,
        position: Int
    ) {
        binding.name.text = item.playlist.name
        binding.numChannels.text = buildSpannedString {
            color(Color.parseColor("#037EB9")) {
                append("${item.channels.size}")
            }
            append(" ")
            color(context.getAttrColor(R.attr.textHint)) {
                append("channels")
            }
        }
        binding.iconType.setImageResource(
            when (item.playlist.typePlayList) {
                AppConstants.TYPE_PLAYLIST_URL -> R.drawable.ic_url
                AppConstants.TYPE_PLAYLIST_FILE -> R.drawable.ic_file_uploaded
                AppConstants.TYPE_PLAYLIST_GALLERY -> R.drawable.ic_up_video
                else -> R.drawable.ic_add_stream
            }
        )
        binding.iconMore.setOnClickListener {
            onMoreClick(item, position)
        }
    }

    private var onMoreClick: (item: PlaylistWithChannels, position: Int) -> Unit = {_,_ ->}

    fun setOnClickItemAdapter(listener: (item: PlaylistWithChannels, position: Int) -> Unit, onMoreClick: (item: PlaylistWithChannels, position: Int) -> Unit) {
        setOnClickItemListener = listener
        this.onMoreClick = onMoreClick
    }

    fun Context.getAttrColor(attr: Int): Int {
        val typedValue = TypedValue()
        theme.resolveAttribute(attr, typedValue, true)
        return typedValue.data
    }
}