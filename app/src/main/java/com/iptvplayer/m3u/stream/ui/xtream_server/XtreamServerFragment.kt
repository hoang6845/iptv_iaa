package com.iptvplayer.m3u.stream.ui.xtream_server

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.core.net.toUri
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResultListener
import androidx.lifecycle.lifecycleScope
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.iptvplayer.m3u.stream.R
import com.iptvplayer.m3u.stream.api.MovieApiService
import com.iptvplayer.m3u.stream.api.MovieRepository
import com.iptvplayer.m3u.stream.databinding.DialogRewardBinding
import com.iptvplayer.m3u.stream.databinding.FragmentXtreamServerBinding
import com.iptvplayer.m3u.stream.main.SharedViewModel
import com.iptvplayer.m3u.stream.model.dao.ServerDao
import com.iptvplayer.m3u.stream.model.entity.LoginResponse
import com.iptvplayer.m3u.stream.model.entity.XtreamAccountState
import com.iptvplayer.m3u.stream.model.entity.XtreamAuth
import com.iptvplayer.m3u.stream.utils.AppConstants
import com.iptvplayer.m3u.stream.utils.MovieSyncWorker
import com.iptvplayer.m3u.stream.utils.PasscodeManagerXtream
import com.iptvplayer.m3u.stream.utils.RewardDialog
import com.iptvplayer.m3u.stream.utils.gone
import com.iptvplayer.m3u.stream.utils.visible
import dagger.hilt.android.AndroidEntryPoint
import hoang.dqm.codebase.base.activity.BaseFragment
import hoang.dqm.codebase.base.activity.navigate
import hoang.dqm.codebase.base.activity.navigateFade
import hoang.dqm.codebase.base.activity.onBackPressed
import hoang.dqm.codebase.base.activity.popBackStack
import hoang.dqm.codebase.utils.AppMonetization
import hoang.dqm.codebase.utils.ads
import hoang.dqm.codebase.utils.premium
import hoang.dqm.codebase.utils.singleClick
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Inject

@AndroidEntryPoint
class XtreamServerFragment : BaseFragment<FragmentXtreamServerBinding, XtreamServerViewModel>() {
    @Inject
    lateinit var passcodeManagerXtream: PasscodeManagerXtream
    private val sharedViewModel: SharedViewModel by activityViewModels()

    @Inject
    lateinit var serverDao: ServerDao
    private val baseUrl: String by lazy {
        arguments?.getString("baseUrl") ?: AppConstants.XTREAM_URL_TEST
    }
    private var isSettingProgrammatically = false
    override fun initView() {
        adjustInsetsForBottomNavigation(binding.toolBar)
        setUpObserver()
    }
    private var currentServerId: Int? = null
    override fun initListener() {
        onBackPressed {
            popBackStack()
        }
        binding.btnBack.setOnClickListener {
            popBackStack()
        }
        binding.btnAdd.singleClick {
            val urlServer = binding.urlServer.text.toString().trim()
            val username = binding.username.text.toString().trim()
            val password = binding.password.text.toString().trim()

            var hasError = false

            // ✅ Validate URL format trước
            val safeUrl = when {
                urlServer.isEmpty() -> {
                    binding.layoutUrlServer.error = getString(R.string.text_please_enter_server_url)
                    hasError = true
                    urlServer
                }
                !urlServer.startsWith("http://") && !urlServer.startsWith("https://") -> {
                    binding.layoutUrlServer.error = getString(R.string.text_invalid_server_url)
                    hasError = true
                    urlServer
                }
                else -> {
                    binding.layoutUrlServer.error = null
                    // Retrofit yêu cầu trailing slash
                    if (urlServer.endsWith("/")) urlServer else "$urlServer/"
                }
            }

            if (username.isEmpty()) {
                binding.layoutUsername.error = getString(R.string.text_please_enter_username)
                hasError = true
            } else {
                binding.layoutUsername.error = null
            }

            if (password.isEmpty()) {
                binding.layoutPassword.error = getString(R.string.text_please_enter_password)
                hasError = true
            } else {
                binding.layoutPassword.error = null
            }

            if (hasError) {
                binding.urlServer.setText(AppConstants.XTREAM_URL_TEST)
                binding.username.setText(AppConstants.XTREAM_USERNAME_TEST)
                binding.password.setText(AppConstants.XTREAM_PASSWORD_TEST)
                return@singleClick
            }
//            if (!AppMonetization.premium.isSubscribed()){
//                navigateFade(R.id.IAPFragment)
//                return@singleClick
//            }
            viewLifecycleOwner.lifecycleScope.launch {
                val isDuplicate = withContext(Dispatchers.IO) {
                    serverDao.countByUsernameAndServer(username, safeUrl) > 0
                }

                if (isDuplicate) {
                    Toast.makeText(requireContext(), R.string.text_this_account_already_exists, Toast.LENGTH_SHORT).show()
                    return@launch
                }

                // ✅ Wrap trong try-catch để bắt lỗi URL không hợp lệ
                val apiService = try {
                    createApiService(safeUrl)
                } catch (e: IllegalArgumentException) {
                    binding.layoutUrlServer.error = getString(R.string.text_invalid_server_url)
                    return@launch
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), getString(R.string.text_invalid_server_url), Toast.LENGTH_SHORT).show()
                    return@launch
                }



                val repository = MovieRepository(apiService)
                viewModel.checkAccount(repository, username, password, safeUrl, false)
            }
        }

        setFragmentResultListener("passcode_result") { _, bundle ->
            val passcodeSet = bundle.getBoolean("passcodeSet", false)
            if (!passcodeSet) {
                isSettingProgrammatically = true
                binding.btnToggleMusic.isChecked = false
                isSettingProgrammatically = false
            }
        }

        binding.btnToggleMusic.setOnCheckedChangeListener { _, isChecked ->
            if (isSettingProgrammatically) return@setOnCheckedChangeListener
            if (isChecked && !passcodeManagerXtream.hasPasscode()) {
                isSettingProgrammatically = true
                navigate(R.id.passcodeXtreamFragment)
            }
        }
    }

    override fun initData() {



    }

    private fun observeWork(serverId: Int) {
        WorkManager.getInstance(requireContext())
            .getWorkInfosForUniqueWorkLiveData("movie_sync_$serverId")
            .observe(viewLifecycleOwner) { workInfos ->

                val workInfo = workInfos.firstOrNull() ?: return@observe

                when (workInfo.state) {

                    WorkInfo.State.ENQUEUED -> {
                        binding.loading.visible()
                    }

                    WorkInfo.State.RUNNING -> {
                        val percent = workInfo.progress.getInt(
                            AppConstants.PROGRESS_CURRENT, 0
                        )
                        binding.progressBar.progress = percent
                    }

                    WorkInfo.State.SUCCEEDED -> {
                        binding.progressBar.progress = 100

                        val bundle = Bundle().apply {
                            putInt(
                                "serverId",
                                workInfo.outputData.getInt(AppConstants.ID_SERVER, -1)
                            )
                            putString("avatar", "avatar/avatar_1.png")
                        }
                        sharedViewModel.setData(bundle.getInt("serverId"))
                        binding.loading.gone()
//                        showInterstitialAd {
//                            navigate(R.id.xtreamFragment, bundle)
//                        }
                        navigate(R.id.xtreamFragment, bundle)
                    }

                    else -> Unit
                }
            }
    }

    fun setUpObserver() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.accountState.collect { state ->
                when (state) {

                    is XtreamAccountState.Loading -> {
                        // Có thể show loading check account nếu muốn
                    }

                    is XtreamAccountState.Success -> {
                        val login = state.data

                        // Reset ngay để tránh show RewardDialog lại nhiều lần
                        viewModel.resetAccountState()

                        val activity = activity ?: return@collect
                        viewLifecycleOwner.lifecycleScope.launch {
                            saveServerAndSync(login, login.url)
                        }

                    }

                    is XtreamAccountState.Error -> {
                        val message = state.message

                        // Reset để tránh toast lại khi Fragment resume
                        viewModel.resetAccountState()

                        if (isAdded) {
                            Toast.makeText(
                                requireContext(),
                                message,
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }

                    else -> Unit
                }
            }
        }
    }

    private suspend fun saveServerAndSync(login: LoginResponse, baseUrl: String) {
        val server = XtreamAuth(
            id = 0,
            server = baseUrl,
            username = login.userInfo.username,
            password = login.userInfo.password,
            getHost(baseUrl),
            isEnablePasscode = binding.btnToggleMusic.isChecked,
            createAt = login.userInfo.createdAt?.toLongOrNull(),
            expDate = login.userInfo.expDate?.toLongOrNull()
        )

        val serverId = serverDao.insertOne(server)
        currentServerId = serverId.toInt()

        startMovieSync(serverId.toInt())
        observeWork(serverId.toInt())
    }

    fun getHost(url: String): String? {
        val host = url.toUri().host // xxip25.top
        val name = host?.substringBefore(".")
        return name
    }

    private fun startMovieSync(serverId: Int) {
        Log.d("xtream account server", "1")

        val workRequest =
            OneTimeWorkRequestBuilder<MovieSyncWorker>()
                .setInputData(
                    workDataOf(
                        AppConstants.KEY_SERVER_ID to serverId
                    )
                )
                .addTag(MovieSyncWorker::class.java.simpleName)
                .build()

        WorkManager.getInstance(requireContext())
            .enqueueUniqueWork(
                "movie_sync_$serverId",
                ExistingWorkPolicy.REPLACE,
                workRequest
            )
        binding.loading.visible()
        binding.progressBar.max = 100
        binding.progressBar.progress = 0
    }

    private fun createApiService(url: String): MovieApiService {
        val retrofit = Retrofit.Builder()
            .baseUrl(url)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        return retrofit.create(MovieApiService::class.java)
    }
}