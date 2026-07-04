package com.iptvplayer.m3u.stream.ui.series_detail

import androidx.recyclerview.widget.LinearLayoutManager
import com.iptvplayer.m3u.stream.databinding.ItemSeasonBinding
import com.iptvplayer.m3u.stream.model.entity.Episode
import com.iptvplayer.m3u.stream.model.entity.Season
import com.iptvplayer.m3u.stream.utils.gone
import com.iptvplayer.m3u.stream.utils.visible
import hoang.dqm.codebase.base.adapter.BaseRecyclerViewAdapter

class SeasonSeriesAdapter : BaseRecyclerViewAdapter<Season, ItemSeasonBinding>() {
    private var episodes: Map<String, List<Episode>> = emptyMap()
    private var isVisible: MutableList<Boolean> = mutableListOf()

    fun setEpisodes(value: Map<String, List<Episode>>) {
        episodes = value
    }

    fun setUpList(list: List<Season>, value: Map<String, List<Episode>>) {
        setEpisodes(value)
        setList(list)
        isVisible = MutableList(list.size) { false }
    }

    fun toggleVisible(position: Int) {
        isVisible[position] = !isVisible[position]
        notifyItemChanged(position)
    }

    override fun bindData(
        binding: ItemSeasonBinding, item: Season, position: Int
    ) {
        binding.seasonName.text = item.name
        val episodesAdapter = EpisodesAdapter()
        if (isVisible[position]){
            binding.rvMovies.visible()
            episodesAdapter.setList(episodes[item.seasonNumber.toString()] ?: emptyList())
            episodesAdapter.setOnClickItem { item, position ->
                this.onEpisodesClick?.invoke(item, position)
            }
            binding.rvMovies.adapter = episodesAdapter
            binding.rvMovies.layoutManager =
                LinearLayoutManager(binding.root.context, LinearLayoutManager.HORIZONTAL, false)

        }else{
            binding.rvMovies.gone()
        }
    }

    private var onEpisodesClick: ((item: Episode, position: Int) -> Unit)? = null
    fun setOnClickItem(onEpisodesClick: (item: Episode, position: Int) -> Unit, listener:(item: Season, position: Int) -> Unit){
        setOnClickItemListener = listener
        this.onEpisodesClick = onEpisodesClick
    }
}