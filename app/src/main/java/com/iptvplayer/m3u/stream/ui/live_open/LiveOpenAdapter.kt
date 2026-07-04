package com.iptvplayer.m3u.stream.ui.live_open

import android.content.Context
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.iptvplayer.m3u.stream.R
import com.iptvplayer.m3u.stream.databinding.ItemLiveOpenBinding
import com.iptvplayer.m3u.stream.model.entity.LiveXtream
import hoang.dqm.codebase.utils.loadImage
import hoang.dqm.codebase.utils.loadImageSketch

class LiveOpenAdapter : PagingDataAdapter<LiveXtream, LiveOpenAdapter.ViewHolder>(DIFF_CALLBACK) {

    private var onClickItem: ((LiveXtream, Int) -> Unit)? = null

    fun setOnClickItemAdapter(
        onClick: (LiveXtream, Int) -> Unit,
    ) {
        this.onClickItem = onClick
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemLiveOpenBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        getItem(position)?.let { holder.bind(it, position) }
    }

    inner class ViewHolder(private val binding: ItemLiveOpenBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: LiveXtream, position: Int) {
            binding.root.setOnClickListener {
                onClickItem?.invoke(item, position)

            }
            binding.name.text = item.name
            if (item.streamIcon.isNotEmpty()) {
                binding.imgAvatar.loadImageSketch(item.streamIcon)
            } else {
                binding.imgAvatar.loadImage(R.drawable.img_live_loading)
            }
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<LiveXtream>() {
            override fun areItemsTheSame(old: LiveXtream, new: LiveXtream) =
                old.streamId == new.streamId

            override fun areContentsTheSame(old: LiveXtream, new: LiveXtream) = old == new
        }
    }

    fun Context.getAttrColor(attr: Int): Int {
        val typedValue = TypedValue()
        theme.resolveAttribute(attr, typedValue, true)
        return typedValue.data
    }
}