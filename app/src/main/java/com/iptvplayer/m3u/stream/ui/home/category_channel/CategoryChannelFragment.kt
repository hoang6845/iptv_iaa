package com.iptvplayer.m3u.stream.ui.home.category_channel

import android.animation.ObjectAnimator
import android.os.Bundle
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.asFlow
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import com.iptvplayer.m3u.stream.R
import com.iptvplayer.m3u.stream.databinding.FragmentCategoryChannelBinding
import com.iptvplayer.m3u.stream.model.dao.PlaylistDao
import com.iptvplayer.m3u.stream.ui.home.HomeViewModel
import dagger.hilt.android.AndroidEntryPoint
import hoang.dqm.codebase.base.activity.BaseFragment
import hoang.dqm.codebase.base.activity.navigate
import hoang.dqm.codebase.base.activity.onBackPressed
import hoang.dqm.codebase.base.activity.popBackStack
import hoang.dqm.codebase.utils.collectLatestFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@AndroidEntryPoint
class CategoryChannelFragment :
    BaseFragment<FragmentCategoryChannelBinding, CategoryChannelViewModel>() {
    private val homeViewModel: HomeViewModel by activityViewModels()

    @Inject
    lateinit var playlistDao: PlaylistDao
    var isAToZ = true
    private var refreshAnimator: ObjectAnimator? = null
    private val categoryChannelAdapter: CategoryChannelAdapter by lazy {
        CategoryChannelAdapter()
    }

    override fun initView() {
        adjustInsetsForBottomNavigation(binding.toolBar)
        setUpAdapter()
    }

    override fun initListener() {
        onBackPressed {
            popBackStack(R.id.homeFragment)
        }
        binding.imgArrow.setOnClickListener {
            popBackStack(R.id.homeFragment)
        }
        binding.tvBack.setOnClickListener {
            popBackStack(R.id.homeFragment)
        }
        binding.btnAz.setOnClickListener {
            isAToZ = !isAToZ

            val sortedList = if (isAToZ) {
                categoryChannelAdapter.dataList.sortedBy { it.type }
            } else {
                categoryChannelAdapter.dataList.sortedByDescending { it.type }
            }

            categoryChannelAdapter.setList(sortedList)
        }
        binding.btnRefresh.setOnClickListener {
            homeViewModel.playlistSelected.value?.playlist?.id?.let {
                viewModel.updatePlaylist(it)
            }
        }
    }

    override fun initData() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                combine(
                    viewModel.listCategoryChannel,
                    homeViewModel.playlistSelected.asFlow()
                ) { categoryItems, playlist ->
                    categoryItems.forEach { categoryItem ->
                        categoryItem.num =
                            playlist.channels.count { it.group?.lowercase()?.contains(categoryItem.type) == true || categoryItem.type == "all" }
                    }
                    categoryItems
                }.collect { list ->
                    val filtered = list.filter { it.num > 0 }

                    val sorted = if (isAToZ) {
                        filtered.sortedBy { it.type }
                    } else {
                        filtered.sortedByDescending { it.type }
                    }

                    categoryChannelAdapter.setList(sorted)
                }
            }
        }

        homeViewModel.playlistSelected.observe(viewLifecycleOwner) { playlist ->
            binding.namePlaylist.text = playlist.playlist.name
        }

        collectLatestFlow(viewModel.saveState) { state ->
            when (state) {
                is CategoryRefreshSaveState.Idle -> {
                    setLoadingVisible(false)
                    stopRefreshAnimation()
                }

                is CategoryRefreshSaveState.Loading -> {
                    startRefreshAnimation()
                    setLoadingVisible(true)
                    binding.tvProgress.text = "${state.progress}%"
                    binding.tvLoadingLabel.text = when {
                        state.progress <= 10 -> getString(R.string.text_connecting)
                        state.progress <= 50 -> getString(R.string.text_downloading)
                        state.progress <= 80 -> getString(R.string.text_parsing_channels)
                        else -> getString(R.string.text_saving)
                    }
                }

                is CategoryRefreshSaveState.Success -> {
                    setLoadingVisible(false)
                    stopRefreshAnimation()

                    lifecycleScope.launch {
                        val updated = withContext(Dispatchers.IO) {
                            playlistDao.getOnePlaylist(state.playlistId.toInt())
                        }
                        homeViewModel.setPlaylistSelected(updated)
                    }

                    Toast.makeText(
                        requireContext(),
                        getString(R.string.text_refresh_playlist_success),
                        Toast.LENGTH_SHORT
                    ).show()
                }

                is CategoryRefreshSaveState.Error -> {
                    setLoadingVisible(false)
                    stopRefreshAnimation()
                    Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    fun setUpAdapter() {
        categoryChannelAdapter.setOnClickAdapter { item, position ->
            showInterstitialAd {
                val bundle = Bundle().apply {
                    putString("type", item.type)
                    putString("name", item.value)
                }
                navigate(R.id.listChannelFragment, bundle)
            }
        }
        binding.rvCategory.adapter = categoryChannelAdapter
        binding.rvCategory.layoutManager =
            GridLayoutManager(requireContext(), 3, GridLayoutManager.VERTICAL, false)

    }
    private fun startRefreshAnimation() {
        refreshAnimator?.cancel()
        refreshAnimator = ObjectAnimator.ofFloat(binding.icRefresh, "rotation", 0f, 360f).apply {
            duration = 800
            repeatCount = ObjectAnimator.INFINITE
            interpolator = LinearInterpolator()
            start()
        }
    }

    private fun stopRefreshAnimation() {
        refreshAnimator?.cancel()
        refreshAnimator = null
        binding.icRefresh.rotation = 0f
    }
    private fun setLoadingVisible(visible: Boolean) {
        binding.layoutLoading.visibility = if (visible) View.VISIBLE else View.GONE
    }
}