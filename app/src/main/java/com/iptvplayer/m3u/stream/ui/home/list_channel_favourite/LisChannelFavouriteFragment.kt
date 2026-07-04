package com.iptvplayer.m3u.stream.ui.home.list_channel_favourite

import android.content.res.ColorStateList
import android.os.Bundle
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.iptvplayer.m3u.stream.R
import com.iptvplayer.m3u.stream.databinding.FragmentLisChannelFavouriteBinding
import com.iptvplayer.m3u.stream.model.dao.FavouriteChannelDao
import com.iptvplayer.m3u.stream.model.entity.Channel
import com.iptvplayer.m3u.stream.ui.home.HomeViewModel
import com.iptvplayer.m3u.stream.ui.local.AppSharePref
import com.iptvplayer.m3u.stream.utils.UnlockUnlimitedDialog
import com.iptvplayer.m3u.stream.utils.gone
import com.iptvplayer.m3u.stream.utils.visible
import dagger.hilt.android.AndroidEntryPoint
import hoang.dqm.codebase.base.activity.BaseFragment
import hoang.dqm.codebase.base.activity.navigate
import hoang.dqm.codebase.base.activity.onBackPressed
import hoang.dqm.codebase.base.activity.popBackStack
import hoang.dqm.codebase.utils.getColorFromRes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@AndroidEntryPoint
class LisChannelFavouriteFragment : BaseFragment<FragmentLisChannelFavouriteBinding, ListChannelFavouriteViewModel>() {
    @Inject
    lateinit var favouriteChannelDao: FavouriteChannelDao
    private val homeViewModel: HomeViewModel by activityViewModels()
    private val channelAdapter: ChannelAdapter by lazy {
        ChannelAdapter()
    }

    private var isGrid = true

    private val channelHorizontalAdapter: ChannelHorizontalAdapter by lazy {
        ChannelHorizontalAdapter()
    }

    override fun initView() {
        adjustInsetsForBottomNavigation(binding.toolBar)
        setUpAdapter()
        updateUi()
        setUpObserver()
    }

    override fun initListener() {
        onBackPressed { popBackStack() }
        binding.edtSearch.doAfterTextChanged {
            viewModel.search(it.toString())
        }
        binding.imgArrow.setOnClickListener {
            popBackStack()
        }
        binding.textBack.setOnClickListener {
            popBackStack()
        }
        binding.btnGrid.setOnClickListener {
            if (isGrid) return@setOnClickListener
            isGrid = true
            updateUi()
        }
        binding.btnLine.setOnClickListener {
            if (!isGrid) return@setOnClickListener
            isGrid = false
            updateUi()
        }
    }

    override fun initData() {

    }

    fun updateUi() {
        if (isGrid) {
            binding.btnGrid.imageTintList =
                ColorStateList.valueOf(getColorFromRes(R.color.text_color_045DCC))
            binding.btnLine.imageTintList =
                ColorStateList.valueOf(getColorFromRes(R.color.text_default_ic_889db8))
            binding.rvGrid.visible()
            binding.rvLine.gone()
        } else {
            binding.btnLine.imageTintList =
                ColorStateList.valueOf(getColorFromRes(R.color.text_color_045DCC))
            binding.btnGrid.imageTintList =
                ColorStateList.valueOf(getColorFromRes(R.color.text_default_ic_889db8))
            binding.rvLine.visible()
            binding.rvGrid.gone()
        }
    }

    fun setUpAdapter() {
        val appSharePref = AppSharePref(requireContext())
        channelAdapter.setOnClickListener({ item, position ->

            // Nếu user free đã xem đủ 3 lần trong hôm nay
            if (!viewModel.isSubscribed &&!appSharePref.canWatchChannelToday()) {
                showDailyFreeLimitDialog()
                return@setOnClickListener
            }

            val canWatch = appSharePref.tryConsumeWatchChannelToday()

            if (!canWatch) {
                showDailyFreeLimitDialog()
                return@setOnClickListener
            }

            val playlistId = this@LisChannelFavouriteFragment.channelAdapter.mapPlaylist[item.id] ?: 1L

            val bundle = Bundle().apply {
                putString("url", item.url)
                putString("name", item.name)
                putLong("playlistId", playlistId)
                putString("channelId", item.id)
            }

            viewModel.addRecentChannel(item, playlistId)

            navigate(R.id.watchChannelFragment, bundle)

        }) { item, position ->
            viewModel.toggleFavourite(item)
        }

        binding.rvLine.adapter = channelAdapter
        binding.rvLine.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)


        channelHorizontalAdapter.setOnClickListener({ item, position ->

            // Nếu user free đã xem đủ 3 lần trong hôm nay
            if (!viewModel.isSubscribed &&!appSharePref.canWatchChannelToday()) {
                showDailyFreeLimitDialog()
                return@setOnClickListener
            }

            val canWatch = appSharePref.tryConsumeWatchChannelToday()

            if (!canWatch) {
                showDailyFreeLimitDialog()
                return@setOnClickListener
            }

            val playlistId =
                this@LisChannelFavouriteFragment.channelHorizontalAdapter.mapPlaylist[item.id] ?: 1L

            val bundle = Bundle().apply {
                putString("url", item.url)
                putString("name", item.name)
                putLong("playlistId", playlistId)
                putString("channelId", item.id)
            }

            viewModel.addRecentChannel(item, playlistId)

            navigate(R.id.watchChannelFragment, bundle)

        }) { item, position ->
            viewModel.toggleFavourite(item)
        }

        binding.rvGrid.adapter = channelHorizontalAdapter
        binding.rvGrid.layoutManager =
            GridLayoutManager(requireContext(), 3, GridLayoutManager.VERTICAL, false)

    }

    private fun showDailyFreeLimitDialog() {
        UnlockUnlimitedDialog(
            activity = requireActivity(),
            onConfirm = {
                 navigate(R.id.IAPFragment)
            },
            onCancel = {
                // User chọn quay lại ngày mai
            }
        ).show()
    }

    fun setUpObserver(){
        homeViewModel.favouriteList.observe(viewLifecycleOwner) {playlistFavourite ->
            CoroutineScope(Dispatchers.IO).launch {
                val listFavourite = favouriteChannelDao.getAllFavouriteChannelsSuspend()
                withContext(Dispatchers.Main){
                    viewModel.setChannels(listFavourite)
                }
            }
        }

        viewModel.filteredChannels.observe(viewLifecycleOwner){ channels ->
            val newChannel = channels.map {
                Channel(it.id, it.name, it.logo, it.group, it.url, it.isFavourite)
            }
            val map = channels.associate { it.id to it.playlistId }

            channelAdapter.setMap(map)
            channelHorizontalAdapter.setMap(map)
            channelAdapter.setList(newChannel)
            channelHorizontalAdapter.setList(newChannel)
        }
    }
}