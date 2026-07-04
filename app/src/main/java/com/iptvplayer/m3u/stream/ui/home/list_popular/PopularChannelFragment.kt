package com.iptvplayer.m3u.stream.ui.home.list_popular

import android.content.res.ColorStateList
import android.os.Bundle
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.iptvplayer.m3u.stream.R
import com.iptvplayer.m3u.stream.databinding.FragmentPopularChannelBinding
import com.iptvplayer.m3u.stream.model.entity.Channel
import com.iptvplayer.m3u.stream.ui.home.list_channel_favourite.ChannelAdapter
import com.iptvplayer.m3u.stream.ui.home.list_channel_favourite.ChannelHorizontalAdapter
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
import kotlinx.coroutines.launch

@AndroidEntryPoint
class PopularChannelFragment : BaseFragment<FragmentPopularChannelBinding, PopularChannelViewModel>() {

    private val type: String? by lazy { arguments?.getString("type") }
    private val name: String by lazy { arguments?.getString("name") ?: "" }
    private var isGrid = true

    private val channelAdapter: ChannelAdapter by lazy { ChannelAdapter() }
    private val channelHorizontalAdapter: ChannelHorizontalAdapter by lazy { ChannelHorizontalAdapter() }

    override fun initView() {
        adjustInsetsForBottomNavigation(binding.toolBar)
        viewModel.setType(type)
        binding.title.text = name
        setUpAdapter()
        updateUi()
    }

    override fun initListener() {
        onBackPressed { popBackStack() }

        binding.edtSearch.doAfterTextChanged {
            viewModel.search(it.toString())
        }
        binding.imgArrow.setOnClickListener { popBackStack() }
        binding.textBack.setOnClickListener { popBackStack() }

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
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.channels.collect { channels ->
                    val newChannel = channels.map {
                        Channel(it.id, it.name, it.logo, it.group, it.url, it.isFavourite)
                    }
                    channelAdapter.setList(newChannel)
                    channelHorizontalAdapter.setList(newChannel)
                }
            }
        }
    }

    private fun updateUi() {
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

    private fun setUpAdapter() {
        val appSharePref = AppSharePref(requireContext())
        channelAdapter.setOnClickListener({ item, _ ->

            // Nếu user free đã xem đủ 3 lần trong hôm nay
            if (!viewModel.isSubscribed && !appSharePref.canWatchChannelToday()) {
                showDailyFreeLimitDialog()
                return@setOnClickListener
            }

            val canWatch = appSharePref.tryConsumeWatchChannelToday()

            if (!canWatch) {
                showDailyFreeLimitDialog()
                return@setOnClickListener
            }

            val bundle = Bundle().apply {
                putString("url", item.url)
                putString("name", item.name)
                putString("channelId", item.id)
            }

            navigate(R.id.watchChannelFragment, bundle)

        }) { item, _ ->
            viewModel.toggleFavourite(item)
        }

        binding.rvLine.adapter = channelAdapter
        binding.rvLine.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)

        channelHorizontalAdapter.setOnClickListener({ item, _ ->

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

            val bundle = Bundle().apply {
                putString("url", item.url)
                putString("name", item.name)
                putString("channelId", item.id)
            }

            navigate(R.id.watchChannelFragment, bundle)

        }) { item, _ ->
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
}