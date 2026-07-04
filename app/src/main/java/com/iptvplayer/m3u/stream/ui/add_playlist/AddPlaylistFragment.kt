package com.iptvplayer.m3u.stream.ui.add_playlist

import android.util.Log
import com.google.android.material.tabs.TabLayoutMediator
import com.iptvplayer.m3u.stream.R
import com.iptvplayer.m3u.stream.databinding.FragmentAddPlaylistBinding
import hoang.dqm.codebase.base.activity.BaseFragment
import hoang.dqm.codebase.base.activity.onBackPressed
import hoang.dqm.codebase.base.activity.popBackStack


class AddPlaylistFragment : BaseFragment<FragmentAddPlaylistBinding, AddPlaylistViewModel>() {
    private lateinit var adapter: AddPlaylistPagerAdapter
    private val isUpload: Boolean by lazy {
        arguments?.getBoolean("isUpload")?:false
    }
    override fun initView() {
        adjustInsetsForBottomNavigation(binding.toolBar)
        adapter = AddPlaylistPagerAdapter(childFragmentManager, viewLifecycleOwner.lifecycle)
        binding.viewPager2.adapter = adapter
        if (isUpload) {
            binding.viewPager2.post {
                binding.viewPager2.currentItem = 1
            }
        }
        binding.tabLayout.setSelectedTabIndicator(R.drawable.tab_indicator)
        TabLayoutMediator(binding.tabLayout, binding.viewPager2) { tab, position ->
            when (position) {
                0 -> tab.text = getString(R.string.text_import_link)
                1 -> tab.text = getString(R.string.text_upload_file)
            }
        }.attach()
//        binding.viewPager2.isUserInputEnabled = false
        Log.d("check navigate", "initView: $isUpload")

    }

    override fun initListener() {
        onBackPressed { popBackStack() }
        binding.btnClose.setOnClickListener {
            popBackStack()
        }
    }

    override fun initData() {
    }
}