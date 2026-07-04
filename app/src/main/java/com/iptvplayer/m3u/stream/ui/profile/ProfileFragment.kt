package com.iptvplayer.m3u.stream.ui.profile

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.activityViewModels
import com.iptvplayer.m3u.stream.R
import com.iptvplayer.m3u.stream.databinding.FragmentProfileBinding
import com.iptvplayer.m3u.stream.main.SharedViewModel
import com.iptvplayer.m3u.stream.model.dao.ServerDao
import dagger.hilt.android.AndroidEntryPoint
import hoang.dqm.codebase.base.activity.BaseFragment
import hoang.dqm.codebase.base.activity.navigate
import hoang.dqm.codebase.base.activity.onBackPressed
import hoang.dqm.codebase.base.activity.popBackStack
import hoang.dqm.codebase.utils.loadImageSketch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.getValue

@AndroidEntryPoint
class ProfileFragment : BaseFragment<FragmentProfileBinding, ProfileViewModel>() {
    @Inject
    lateinit var serverDao: ServerDao
    private val sharedViewModel: SharedViewModel by activityViewModels()
    private var avatar: String = "avatar/avatar_1.png"

    private val serverId: Int by lazy {
        arguments?.getInt("serverId") ?: 0
    }

    override fun initView() {
        adjustInsetsForBottomNavigation(binding.toolBar)
        CoroutineScope(Dispatchers.IO).launch {
            if (serverId == 0) return@launch
            val server = serverDao.getServerById(serverId)
            if (server == null) return@launch
            val name = server.name
            val avatar = server.urlAvatar
            withContext(Dispatchers.Main) {
                binding.avatar.loadImageSketch(avatar, isFull = false)
                this@ProfileFragment.avatar = avatar
                binding.name.text = name
            }
        }

    }

    override fun initListener() {
        binding.switchAccount.setOnClickListener {
            popBackStack(R.id.xtreamHomeFragment)
        }
        binding.btnEditProfile.setOnClickListener {
            if (serverId == 0) return@setOnClickListener
            val bundle = Bundle().apply {
                putInt("serverId", serverId)
            }
            navigate(R.id.editProfileFragment, bundle)
        }


        binding.btnWatchlist.setOnClickListener {
            val bundle = Bundle().apply {
                putInt("serverId", serverId)
                putString("avatar", avatar)
            }
            Log.d("check live", "initListener: $serverId")
            sharedViewModel.setData(bundle.getInt("serverId"))
            showInterstitialAd {
                navigate(R.id.searchXtreamFragment, bundle)

            }
        }

        binding.btnSetting.setOnClickListener {
            val bundle = Bundle().apply {
                putInt("serverId", serverId)
            }
            navigate(R.id.profileGeneralFragment, bundle)
        }

        binding.btnAccount.setOnClickListener {
            val bundle = Bundle().apply {
                putInt("serverId", serverId)
            }
            navigate(R.id.accountFragment, bundle)
        }

        binding.btnBack.setOnClickListener {
            popBackStack(R.id.xtreamFragment)
        }

        onBackPressed {
            popBackStack(R.id.xtreamFragment)
        }
    }

    override fun initData() {
    }
}