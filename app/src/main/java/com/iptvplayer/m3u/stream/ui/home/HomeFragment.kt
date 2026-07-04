package com.iptvplayer.m3u.stream.ui.home

import android.content.Context
import android.os.Bundle
import android.util.TypedValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.viewpager2.widget.ViewPager2
import com.iptvplayer.m3u.stream.R
import com.iptvplayer.m3u.stream.databinding.FragmentHomeBinding
import com.iptvplayer.m3u.stream.ui.local.AppSharePref
import com.iptvplayer.m3u.stream.utils.UnlockUnlimitedDialog
import com.iptvplayer.m3u.stream.utils.gone
import com.iptvplayer.m3u.stream.utils.visible
import dagger.hilt.android.AndroidEntryPoint
import hoang.dqm.codebase.base.activity.BaseFragment
import hoang.dqm.codebase.base.activity.navigate
import hoang.dqm.codebase.base.activity.navigateFade
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

@AndroidEntryPoint
class HomeFragment : BaseFragment<FragmentHomeBinding, HomeViewModel>() {
    private var currentTab = 0

    private val recentAdapter: RecentAdapter by lazy {
        RecentAdapter()
    }

    override fun initView() {
        adjustInsetsForBottomNavigation(binding.toolBar)
//        setUpAdapter()
        binding.viewPager.adapter =
            FragmentHomeAdapter(childFragmentManager, viewLifecycleOwner.lifecycle)
        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                when (position) {
                    0 -> selectTab(0)
                    1 -> selectTab(1)
                    2 -> selectTab(2)
                    3 -> selectTab(3)
                }
            }
        })
        currentTab = binding.viewPager.currentItem
        updateUiTab(binding.viewPager.currentItem)
        setUpRecentAdapter()
        observeHomeTabResult()

        // Sử dụng preloaded native ad để hiển thị nhanh hơn
        showPreloadedNativeOrLoad(binding.viewNativeAd, R.string.ads_native_home, updateTimeout = false)

    }

    private fun observeHomeTabResult() {
        findNavController()
            .currentBackStackEntry
            ?.savedStateHandle
            ?.getLiveData<Int>(KEY_HOME_TAB)
            ?.observe(viewLifecycleOwner) { tab ->
                binding.viewPager.setCurrentItem(tab, false)
                selectTab(tab)

                findNavController()
                    .currentBackStackEntry
                    ?.savedStateHandle
                    ?.remove<Int>(KEY_HOME_TAB)
            }
    }

    override fun initListener() {
        binding.icRecent.setOnClickListener {
            showInterstitialAd {
                navigate(R.id.recentOpenFragment)
            }
        }
        binding.btnTabAll.setOnClickListener {
            binding.viewPager.currentItem = 0
            currentTab = 0
        }
        binding.btnTabUrl.setOnClickListener {
            binding.viewPager.currentItem = 1
            currentTab = 1

        }
        binding.btnTabFile.setOnClickListener {
            binding.viewPager.currentItem = 2
            currentTab = 2

        }
        binding.btnTabGallery.setOnClickListener {
            binding.viewPager.currentItem = 3
            currentTab = 3

        }

        binding.guideHelp.setOnClickListener {
            navigate(R.id.howToYouFragment)
        }

        binding.banner.setOnClickListener {
            navigateFade(R.id.IAPFragment)
        }

        binding.btnPremium.setOnClickListener {
            navigateFade(R.id.IAPFragment)
        }
    }

    override fun initData() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                combine(
                    viewModel.playlists,
                    viewModel.recents
                ) { playlists, recents ->
                    Pair(playlists, recents)
                }.collect { (playlists, recents) ->
                        if (playlists.isEmpty()) {
                            binding.banner.gone()
                            binding.icRecent.gone()
                            binding.titleRecent.gone()
                        } else {
                            binding.banner.visible()
                            if (recents.isNotEmpty()){
                                recentAdapter.setList(recents)
                                binding.rvRecent.visible()
                                binding.titleRecent.visible()
                                binding.icRecent.visible()
                            }else {
                                binding.titleRecent.gone()
                                binding.icRecent.gone()
                                binding.rvRecent.gone()
                            }
                        }
                    }
            }
        }
    }

    private fun selectTab(newTab: Int) {
        val listTabBg = listOf(
            binding.btnTabAll,
            binding.btnTabUrl,
            binding.btnTabFile,
            binding.btnTabGallery
        )

        val listText = listOf(
            binding.btnTabAll,
            binding.tvUrl,
            binding.tvFile,
            binding.tvGallery
        )

        val listIcon = listOf(
            null,
            binding.iconUrl,
            binding.iconFile,
            binding.iconGallery
        )
        listTabBg[currentTab].background = null
        listText[currentTab].setTextColor(requireContext().getAttrColor(R.attr.myColorOnSurface))
        listIcon[currentTab]?.let {
            it.setColorFilter(requireContext().getAttrColor(R.attr.myColorOnSurface))
        }
        listTabBg[newTab].setBackgroundResource(R.drawable.shape_tab)
        listText[newTab].setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
        listIcon[newTab]?.let {
            it.setColorFilter(ContextCompat.getColor(requireContext(), R.color.white))
        }
        currentTab = newTab
    }

    private fun updateUiTab(tab: Int) {
        val listTabBg = listOf(
            binding.btnTabAll,
            binding.btnTabUrl,
            binding.btnTabFile,
            binding.btnTabGallery
        )

        val listText = listOf(
            binding.btnTabAll,
            binding.tvUrl,
            binding.tvFile,
            binding.tvGallery
        )
        listTabBg[tab].setBackgroundResource(R.drawable.shape_tab)
        listText[tab].setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
    }

    fun Context.getAttrColor(attr: Int): Int {
        val typedValue = TypedValue()
        theme.resolveAttribute(attr, typedValue, true)
        return typedValue.data
    }

    fun setUpRecentAdapter(){
        val appSharePref = AppSharePref(requireContext())
        recentAdapter.setOnClickItemAdapter { item, position ->

            // Nếu user free đã xem đủ 3 lần trong hôm nay
            if (!viewModel.isSubscribed && !appSharePref.canWatchChannelToday()) {
                showDailyFreeLimitDialog()
                return@setOnClickItemAdapter
            }

//            showInterstitialAd {
                val canWatch = appSharePref.tryConsumeWatchChannelToday()

                if (!canWatch) {
                    showDailyFreeLimitDialog()
                    return@setOnClickItemAdapter
                }

                val bundle = Bundle().apply {
                    putString("url", item.url)
                    putString("name", item.name)
                    putString("channelId", item.channelId)
                    putLong("playlistId", item.playlistId)
                }

                navigate(R.id.watchChannelFragment, bundle)
//            }
        }

        binding.rvRecent.adapter= recentAdapter
        binding.rvRecent.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
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

    companion object {
        const val KEY_HOME_TAB = "KEY_HOME_TAB"

        const val TAB_ALL = 0
        const val TAB_URL = 1
        const val TAB_FILE = 2
        const val TAB_GALLERY = 3
    }
}