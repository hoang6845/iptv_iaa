package com.iptvplayer.m3u.stream.ui.xtream_home

import android.os.Bundle
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import com.iptvplayer.m3u.stream.R
import com.iptvplayer.m3u.stream.databinding.FragmentXtreamHomeBinding
import com.iptvplayer.m3u.stream.main.SharedViewModel
import com.iptvplayer.m3u.stream.model.entity.XtreamAuth
import com.iptvplayer.m3u.stream.utils.PasscodeManagerXtream
import com.iptvplayer.m3u.stream.utils.RewardDialog
import com.iptvplayer.m3u.stream.utils.gone
import com.iptvplayer.m3u.stream.utils.visible
import dagger.hilt.android.AndroidEntryPoint
import hoang.dqm.codebase.base.activity.BaseFragment
import hoang.dqm.codebase.base.activity.navigate
import hoang.dqm.codebase.base.activity.onBackPressed
import hoang.dqm.codebase.base.activity.popBackStack
import hoang.dqm.codebase.data.ItemList
import hoang.dqm.codebase.utils.singleClick
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class XtreamHomeFragment : BaseFragment<FragmentXtreamHomeBinding, XtreamHomeViewModel>() {
    @Inject
    lateinit var passcodeManagerXtream: PasscodeManagerXtream
    private val sharedViewModel: SharedViewModel by activityViewModels()
    private val xtreamAdapter: XtreamAdapter by lazy {
        XtreamAdapter()
    }

    override fun initView() {
        adjustInsetsForBottomNavigation(binding.toolBar)
        adjustInsetsForBottomNavigation(binding.isNotHasAccount)
        setUpAdapter()
        binding.textHowtoUpload.paint.isUnderlineText = true

    }

    override fun initListener() {
        binding.textHowtoUpload.setOnClickListener {
            val bundle = Bundle().apply {
                putInt("position", 1)
            }
            navigate(R.id.howToYouFragment, bundle)
        }
        binding.btnAddXtream.singleClick {
            RewardDialog(
                requireActivity(),
                {
                    showRewardedAd (onRewarded = {
                        navigate(R.id.xtreamServerFragment)

                    }, onLoadFailed = {
                        Toast.makeText(requireContext(), "Ads is loading, please try again", Toast.LENGTH_SHORT).show()
                    })


                },
                {}
            ).show()
        }

        onBackPressed {
            popBackStack()
        }

        binding.tvEditProfile.setOnClickListener {

        }
    }

    override fun initData() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.listXtreamAuth.collect { list ->
                    if (list.isEmpty()) {
                        binding.bgIsHasAccount.gone()
                        binding.isNotHasAccount.visible()
                    } else {
                        binding.bgIsHasAccount.visible()
                        binding.isNotHasAccount.gone()
                    }
                    val data = list.map {
                        ItemList.DataItem(it)
                    }
                    val difSize = 1
                    val difList = List(difSize) { ItemList.Placeholder as ItemList<XtreamAuth> }
                    val combinedList = mutableListOf<ItemList<XtreamAuth>>()
                    combinedList.addAll(difList)
                    combinedList.addAll(data)
                    xtreamAdapter.setList(combinedList)
                }
            }
        }

    }

    fun setUpAdapter() {
        xtreamAdapter.setOnClickItem({ position ->
            val item = this@XtreamHomeFragment.xtreamAdapter.dataList.get(position)
            if (item is ItemList.DataItem<XtreamAuth>) {
                val realItem = item.item
                if (realItem.isEnablePasscode) {
                    val bundle = Bundle().apply {
                        putInt("serverId", realItem.id)
                        putString("avatar", realItem.urlAvatar)
                        putString("name", realItem.name)
                    }
                    navigate(R.id.passcodeXtreamFragment, bundle)
                } else {
                    val bundle = Bundle().apply {
                        putInt("serverId", realItem.id)
                        putString("avatar", realItem.urlAvatar)
                        putString("name", realItem.name)
                    }
                    sharedViewModel.setData(bundle.getInt("serverId"))
                    navigate(R.id.xtreamFragment, bundle)
                }
            } else {
                navigate(R.id.xtreamServerFragment)
            }
        }) {
            RewardDialog(
                requireActivity(),
                {
                    showRewardedAd (onRewarded = {
                        navigate(R.id.xtreamServerFragment)

                    }, onLoadFailed = {
                        Toast.makeText(requireContext(), "Ads is loading, please try again", Toast.LENGTH_SHORT).show()
                    })


                },
                {}
            ).show()
        }

        binding.rvXtreamAdapter.adapter = xtreamAdapter
        binding.rvXtreamAdapter.layoutManager = GridLayoutManager(
            requireContext(),
            2,
            GridLayoutManager.VERTICAL,
            false
        )
    }
}