package com.iptvplayer.m3u.stream.ui.home.list_channel

import android.content.res.ColorStateList
import android.os.Bundle
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.iptvplayer.m3u.stream.R
import com.iptvplayer.m3u.stream.databinding.FragmentListChannelBinding
import com.iptvplayer.m3u.stream.model.dao.PlaylistDao
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
class ListChannelFragment : BaseFragment<FragmentListChannelBinding, ListChannelViewModel>() {
    @Inject
    lateinit var playlistDao: PlaylistDao
    private val homeViewModel: HomeViewModel by activityViewModels()

    private val name: String? by lazy {
        arguments?.getString("name")
    }

    private val isFavourite: Boolean? by lazy {
        arguments?.getBoolean("isFavourite")
    }

    private val type: String? by lazy {
        arguments?.getString("type")
    }


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
//        binding.textBack.text = name?:"Playlist"
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

    fun setUpAdapter() {
        val appSharePref = AppSharePref(requireContext())

        channelAdapter.onClickAdapter({ item, position ->
            if (!viewModel.isSubscribed && !appSharePref.canWatchChannelToday()) {
                showDailyFreeLimitDialog()
                return@onClickAdapter
            }

            viewModel.addRecentChannel(item)

//            showInterstitialAd {
                val canWatch = appSharePref.tryConsumeWatchChannelToday()

                if (!canWatch) {
                    showDailyFreeLimitDialog()
                    return@onClickAdapter
                }

                val bundle = Bundle().apply {
                    putString("url", item.url)
                    putString("name", item.name)
                    putLong("playlistId", homeViewModel.playlistSelected.value?.playlist?.id ?: 1)
                    putString("channelId", item.id)
                }

                navigate(R.id.watchChannelFragment, bundle)
//            }

        }) { item, position ->
            viewModel.toggleFavourite(item)
        }

        binding.rvLine.adapter = channelAdapter
        binding.rvLine.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)


        channelHorizontalAdapter.setOnClickListener({ item, position ->

            if (!viewModel.isSubscribed &&!appSharePref.canWatchChannelToday()) {
                showDailyFreeLimitDialog()
                return@setOnClickListener
            }

            viewModel.addRecentChannel(item)

//            showInterstitialAd {
                val canWatch = appSharePref.tryConsumeWatchChannelToday()

                if (!canWatch) {
                    showDailyFreeLimitDialog()
                    return@setOnClickListener
                }

                val bundle = Bundle().apply {
                    putString("url", item.url)
                    putString("name", item.name)
                    putLong("playlistId", homeViewModel.playlistSelected.value?.playlist?.id ?: 1)
                    putString("channelId", item.id)
                }

                navigate(R.id.watchChannelFragment, bundle)
//            }

        }) { item, position ->
            viewModel.toggleFavourite(item)
        }

        binding.rvGrid.adapter = channelHorizontalAdapter
        binding.rvGrid.layoutManager =
            GridLayoutManager(requireContext(), 3, GridLayoutManager.VERTICAL, false)

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

    fun setUpObserver(){
        homeViewModel.playlistSelected.observe(viewLifecycleOwner) { playlist1 ->
            if (playlist1 == null) return@observe
            CoroutineScope(Dispatchers.IO).launch {
                val playlist = playlistDao.getOnePlaylist(playlist1.playlist.id.toInt())
                withContext(Dispatchers.Main){
                    binding.title.text = name
                    binding.textBack.text = playlist.playlist.name
                    viewModel.setChannels(playlist.playlist.id, playlist.channels)
                }

            }
        }

        viewModel.filteredChannels.observe(viewLifecycleOwner) { channels ->
            if (type == null || type == "all") {
                channelAdapter.setList(channels)
                channelHorizontalAdapter.setList(channels)
            } else {
                type?.let { type ->
                    val listChannel = channels.filter {
                        it.group?.lowercase()?.contains(
                            type
                        ) == true
                    }
                    channelAdapter.setList(listChannel)
                    channelHorizontalAdapter.setList(listChannel)
                }
            }

        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
    }

    private fun showDailyFreeLimitDialog() {
        UnlockUnlimitedDialog(
            activity = requireActivity(),
            onConfirm = {
                 navigate(R.id.IAPFragment)
            },
            onCancel = {

            }
        ).show()
    }
}