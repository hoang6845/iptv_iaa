package com.iptvplayer.m3u.stream.ui.profile_account

import android.content.res.ColorStateList
import com.iptvplayer.m3u.stream.R
import com.iptvplayer.m3u.stream.databinding.FragmentAccountBinding
import com.iptvplayer.m3u.stream.model.dao.ServerDao
import dagger.hilt.android.AndroidEntryPoint
import hoang.dqm.codebase.base.activity.BaseFragment
import hoang.dqm.codebase.base.activity.onBackPressed
import hoang.dqm.codebase.base.activity.popBackStack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class AccountFragment : BaseFragment<FragmentAccountBinding, AccountViewModel>() {
    @Inject
    lateinit var serverDao: ServerDao
    private val serverId: Int by lazy {
        arguments?.getInt("serverId")?: 0
    }
    override fun initView() {
        adjustInsetsForBottomNavigation(binding.toolBar)
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
        CoroutineScope(Dispatchers.IO).launch{
            val server = serverDao.getServerById(serverId)
            if (server == null) return@launch
            else {
                withContext(Dispatchers.Main){
                    binding.name.text = server.name

                    val formatter = SimpleDateFormat("MMMM, dd yyyy", Locale.ENGLISH)
                    server.createAt?.let { date ->
                        val date = Date(date * 1000) // Date cần milliseconds
                        val formatted = formatter.format(date)
                        binding.create.text = formatted
                    }
                    server.expDate?.let { date ->
                        val date = Date(date * 1000) // Date cần milliseconds
                        val formatted = formatter.format(date)
                        binding.expired.text = formatted
                        val now = System.currentTimeMillis() / 1000
                        if (now <= server.expDate ){
                            binding.status.text = getString(R.string.text_active)
                            binding.status.setTextColor(ColorStateList.valueOf(resources.getColor(hoang.dqm.codebase.R.color.green_300)))
                        } else {
                            binding.status.text = getString(R.string.text_expired)
                            binding.status.setTextColor(ColorStateList.valueOf(resources.getColor(hoang.dqm.codebase.R.color.colorRed500)))

                        }
                    }

                }
            }
        }
    }
}