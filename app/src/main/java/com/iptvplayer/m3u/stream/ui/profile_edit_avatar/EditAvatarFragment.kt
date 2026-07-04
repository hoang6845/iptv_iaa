package com.iptvplayer.m3u.stream.ui.profile_edit_avatar

import android.os.Bundle
import androidx.fragment.app.setFragmentResult
import androidx.recyclerview.widget.GridLayoutManager
import com.iptvplayer.m3u.stream.databinding.FragmentEditAvatarBinding
import hoang.dqm.codebase.base.activity.BaseFragment
import hoang.dqm.codebase.base.activity.onBackPressed
import hoang.dqm.codebase.base.activity.popBackStack
import hoang.dqm.codebase.utils.collectLatestFlow

class EditAvatarFragment : BaseFragment<FragmentEditAvatarBinding, EditAvatarViewModel>() {
    private val adapter: AvatarAdapter by lazy {
        AvatarAdapter()
    }
    private val avatarUrl: String? by lazy {
        arguments?.getString("avatar")?:"avatar/avatar_1.png"
    }
    override fun initView() {
        adjustInsetsForBottomNavigation(binding.toolBar)
        setUpAdapter()
    }

    override fun initListener() {
        binding.btnBack.setOnClickListener {
            setFragmentResult(
                "edit_avatar_result",
                Bundle().apply {
                    putBoolean("isChange", false)
                    putString("avatarSelected", this@EditAvatarFragment.adapter.getAvatarSelected())
                }
            )
            popBackStack()
        }

        onBackPressed {
            setFragmentResult(
                "edit_avatar_result",
                Bundle().apply {
                    putBoolean("isChange", false)
                }
            )
            popBackStack()
        }

        binding.btnSelect.setOnClickListener {
            setFragmentResult(
                "edit_avatar_result",
                Bundle().apply {
                    putBoolean("isChange", true)
                    putString("avatarSelected", this@EditAvatarFragment.adapter.getAvatarSelected())
                }
            )
            popBackStack()
        }
    }

    override fun initData() {
        collectLatestFlow(viewModel.avatars){value ->
            adapter.setList(value)
            adapter.setPosition(value.indexOfFirst { avatarUrl == it.url })
        }
    }

    fun setUpAdapter(){
        adapter.onClickItem { item, position ->
            this@EditAvatarFragment.adapter.setPosition(position)
        }

        binding.rvAvatar.adapter = adapter
        binding.rvAvatar.layoutManager = GridLayoutManager(
            requireContext(),
            3,GridLayoutManager.VERTICAL,
            false
        )
    }
}