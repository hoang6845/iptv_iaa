package com.iptvplayer.m3u.stream.ui.profile_general

import android.widget.ArrayAdapter
import com.iptvplayer.m3u.stream.R
import com.iptvplayer.m3u.stream.databinding.FragmentProfileGeneralBinding
import com.iptvplayer.m3u.stream.ui.local.AppSharePref
import hoang.dqm.codebase.base.activity.BaseFragment
import hoang.dqm.codebase.base.activity.onBackPressed
import hoang.dqm.codebase.base.activity.popBackStack


class ProfileGeneralFragment : BaseFragment<FragmentProfileGeneralBinding, ProfileGeneralViewModel>() {
    private val serverId: Int by lazy {
        arguments?.getInt("serverId")?: 0
    }
    override fun initView() {
        adjustInsetsForBottomNavigation(binding.toolBar)

        setUpAdapter()
        setUpAutoUpdate()
    }

    override fun initListener() {
        binding.btnBack.setOnClickListener {
            popBackStack()
        }
        onBackPressed {
            popBackStack()
        }
    }

    override fun initData() {

    }

    fun setUpAdapter() {

        val options = listOf("20s", "30s", "40s", "50s")
        val adapter = ArrayAdapter(requireContext(), R.layout.item_dropdown, options)
        val appSharePref = AppSharePref(requireContext())

        val savedTime = appSharePref.getTimeNextEpisode(serverId)

        binding.acvAutoplay.apply {
            setAdapter(adapter)
            setText("${savedTime}s", false)
        }

        binding.acvAutoplay.setOnItemClickListener { _, _, position, _ ->
            val time = options[position].filter { it.isDigit() }.toLong()
            appSharePref.setTimeNextEpisode(serverId, time)
        }

        binding.acvAutoplay.apply {
            setAdapter(adapter)
            setText("${savedTime}s", false)
            threshold = 0

            setOnTouchListener { v, event ->
                if (!isPopupShowing) {
                    showDropDown()
                }
                false
            }

            setOnFocusChangeListener { v, hasFocus ->
                if (hasFocus && !isPopupShowing) {
                    showDropDown()
                }
            }
        }

        binding.root.setOnClickListener {
            binding.acvAutoplay.clearFocus()
        }
    }

    fun setUpAutoUpdate() {
        val appSharePref = AppSharePref(requireContext())
        binding.btnToggleMusic.isChecked = appSharePref.getIsAutoUpdate(serverId)
        binding.btnToggleMusic.setOnCheckedChangeListener { _, isChecked ->
            appSharePref.setIsAutoUpdate(serverId, isChecked)
        }
    }
}