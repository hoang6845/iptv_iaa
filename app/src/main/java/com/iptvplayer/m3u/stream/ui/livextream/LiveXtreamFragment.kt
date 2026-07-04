package com.iptvplayer.m3u.stream.ui.livextream

import android.os.Bundle
import android.view.View
import androidx.core.widget.doAfterTextChanged
import androidx.paging.LoadState
import androidx.recyclerview.widget.GridLayoutManager
import com.iptvplayer.m3u.stream.R
import com.iptvplayer.m3u.stream.api.LiveXtreamRepository
import com.iptvplayer.m3u.stream.api.LiveXtreamService
import com.iptvplayer.m3u.stream.databinding.FragmentLiveXtreamBinding
import com.iptvplayer.m3u.stream.model.dao.LiveXtreamDao
import com.iptvplayer.m3u.stream.model.dao.ServerDao
import com.iptvplayer.m3u.stream.model.entity.XtreamAuth
import dagger.hilt.android.AndroidEntryPoint
import hoang.dqm.codebase.base.activity.BaseFragment
import hoang.dqm.codebase.base.activity.navigate
import hoang.dqm.codebase.base.activity.onBackPressed
import hoang.dqm.codebase.base.activity.popBackStack
import hoang.dqm.codebase.utils.collectLatestFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Inject


@AndroidEntryPoint
class LiveXtreamFragment : BaseFragment<FragmentLiveXtreamBinding, LiveXtreamViewModel>() {
    @Inject
    lateinit var liveXtreamDao: LiveXtreamDao

    @Inject
    lateinit var serverDao: ServerDao

    private val adapter: LiveXtreamAdapter by lazy {
        LiveXtreamAdapter()
    }
    private val serverId: Int? by lazy {
        arguments?.getInt("serverId")
    }
    private var server: XtreamAuth? = null

    override fun initView() {
        adjustInsetsForBottomNavigation(binding.toolBar)
//        getLiveXtream()
        setUpAdapter()
        serverId?.let { id ->
            viewModel.setServerId(id)
        }

        adapter.addLoadStateListener { loadState ->

            val isEmpty =
                loadState.refresh is LoadState.NotLoading &&
                        adapter.itemCount == 0

            binding.progress.visibility =
                if (isEmpty) View.VISIBLE else View.GONE
        }
    }

    override fun initListener() {
        binding.edtSearch.doAfterTextChanged {
            viewModel.search(it.toString())
        }
        onBackPressed {
            popBackStack(R.id.xtreamHomeFragment)
        }
        binding.imgArrow.setOnClickListener {
            popBackStack()
        }
    }

    override fun initData() {
        collectLatestFlow(viewModel.liveXtreamPaged) { pagingData ->
            adapter.submitData(pagingData)
        }

        serverId?.let { id ->
            getLiveXtream(id)
        }
    }

    fun setUpAdapter() {
        binding.rvLive.adapter = adapter
        binding.rvLive.layoutManager = GridLayoutManager(
            requireContext(),
            3,
            GridLayoutManager.VERTICAL,
            false
        )

        adapter.setOnItemClick { xtream, i ->
            val bundle = Bundle().apply {
                putInt("streamId", xtream.streamId)
                putString("movieName", xtream.name)
                putString("username", server?.username)
                putString("password", server?.password)
                putString("server", server?.server)
                putString("type", "live")
                putInt("serverId", serverId ?: 0)
                putString("uniqueId", xtream.uniqueId)
            }
            navigate(R.id.streamFragment, bundle)
        }
    }

    fun getLiveXtream(serverId: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            server = serverDao.getServerById(serverId)
            if (server == null) return@launch
            val listXtream = liveXtreamDao.getAll(serverId)
            server?.let { server ->
                if (listXtream.isEmpty()) {
                    val repository = LiveXtreamRepository(createApiService(server.server))
                    viewModel.getLiveXtream(repository, server.username, server.password, server.id)
                }
            }
        }
    }

    private fun createApiService(url: String): LiveXtreamService {
        val retrofit = Retrofit.Builder()
            .baseUrl(url)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        return retrofit.create(LiveXtreamService::class.java)
    }
}