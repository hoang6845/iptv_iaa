package com.iptvplayer.m3u.stream.main

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.animation.doOnEnd
import androidx.core.graphics.toColorInt
import androidx.core.view.GestureDetectorCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import com.iptvplayer.m3u.stream.R
import com.iptvplayer.m3u.stream.model.entity.Channel
import com.iptvplayer.m3u.stream.model.entity.ChannelPopular
import com.iptvplayer.m3u.stream.ui.iptv_live.LiveFragment
import com.iptvplayer.m3u.stream.ui.iptv_live.LiveTvController
import com.iptvplayer.m3u.stream.ui.local.AppSharePref
import com.iptvplayer.m3u.stream.ui.single_stream.SingleStreamBottomSheet
import com.iptvplayer.m3u.stream.utils.CommonAppSharePref
import com.iptvplayer.m3u.stream.utils.ContextUtils
import com.iptvplayer.m3u.stream.utils.gone
import com.iptvplayer.m3u.stream.utils.visible
import dagger.hilt.android.AndroidEntryPoint
import hoang.dqm.codebase.base.activity.navigate
import hoang.dqm.codebase.base.activity.navigateLeft
import hoang.dqm.codebase.base.activity.navigateRight
import hoang.dqm.codebase.databinding.ActivityMainBinding
import hoang.dqm.codebase.event.subscribeEventNetwork
import hoang.dqm.codebase.service.sound.AppMusicPlayer
import hoang.dqm.codebase.ui.features.main.BaseMainActivity
import hoang.dqm.codebase.utils.AppMonetization
import hoang.dqm.codebase.utils.ads
import hoang.dqm.codebase.utils.openSettingNetWork
import hoang.dqm.codebase.utils.singleClick
import java.util.Locale
import kotlin.math.abs

@AndroidEntryPoint
class MainActivity : BaseMainActivity<ActivityMainBinding, MainViewModel>() {
    private val sharedViewModel: SharedViewModel by viewModels()

    override val graphResId: Int
        get() = R.navigation.app_nav

    private var currentMiniPlayerChannel: ChannelPopular? = null
    private var isMiniPlayerMode = false

    private var initialX = 0f
    private var initialY = 0f
    private var initialTouchX = 0f
    private var initialTouchY = 0f


    private val mapOrder = listOf(
        R.id.homeFragment,
        R.id.channelsFragment,
        R.id.xtreamHomeFragment,
        R.id.settingFragment
    )

    private var selectedIndex = 0
    var serverId: Int = 0

    private lateinit var gestureDetector: GestureDetectorCompat


    @RequiresApi(Build.VERSION_CODES.O)
    override fun initView() {
        super.initView()
        showSystemNavigationBar()
        applyBottomNavigationBarInset()
        updateStatusBarAppearance()

        navController?.addOnDestinationChangedListener { _, _, _ ->
            AppMusicPlayer.checkAndPlay()
        }

        setupTabBar()
        setupMiniPlayerGestures()

        sharedViewModel.dataServerId.observe(this) { data ->
            serverId = data
        }

        AppMonetization.ads.preloadRewardedManagement.loadWithWaterfall(
            activity = this,
            adKeys = listOf(getString(R.string.reward))
        )
    }

    private fun setupTabBar() {
        val tabs = listOf(
            binding.tabHome,
            binding.tabChannel,
            binding.tabXtream,
            binding.tabSetting
        )
        tabs.forEachIndexed { index, tab ->
            tab.setOnClickListener {
                val newIndex = index
                when {
                    newIndex > viewModel.previousIndex.value -> {
                        viewModel.setPreviousIndex(newIndex)
                        selectedIndex = newIndex
                        navigateRight(mapOrder[newIndex], isPop = true)
                    }
                    newIndex < viewModel.previousIndex.value -> {
                        viewModel.setPreviousIndex(newIndex)
                        selectedIndex = newIndex
                        navigateLeft(mapOrder[newIndex], isPop = true)
                    }
                }
                setTabSelected(newIndex)
            }
        }
    }

    private fun setTabSelected(index: Int) {
        val icons = listOf(
            binding.iconHome,
            binding.iconChannel,
            binding.iconXtream,
            binding.iconSetting
        )
        val labels = listOf(
            binding.labelHome,
            binding.labelChannel,
            binding.labelXtream,
            binding.labelSetting
        )
        val activeColor   = getColor(R.color.color_primary)
        val inactiveColor = "#889DB8".toColorInt()

        icons.forEachIndexed { i, icon ->
            icon.setColorFilter(if (i == index) activeColor else inactiveColor)
        }
        labels.forEachIndexed { i, label ->
            label.setTextColor(if (i == index) activeColor else inactiveColor)
        }
    }



    override fun initData() {
        super.initData()
        subscribeEventNetwork { online ->
            runOnUiThread {
                binding.layoutNoInternet.root.isVisible = online.not()
            }
        }
        binding.layoutNoInternet.buttonSetting.singleClick { openSettingNetWork() }

        viewModel.isLoading.observe {
            binding.loading.loadingView.isVisible = it
        }
    }

    override fun initListener() {
        super.initListener()

        binding.fabAdd1.setOnClickListener {
            val bottomSheet = AddOptionSheet()
            bottomSheet.show(supportFragmentManager, "BOTTOM_SHEET_ADD_OPTION")
        }

        supportFragmentManager.setFragmentResultListener("add_option_result", this) { _, bundle ->
            when (bundle.getString("action")) {
                "open_add_xtream"   -> {
                    showInterstitialAd {
                        navigate(R.id.xtreamServerFragment)
                    }
                }
                "import_playlist"   -> {
                    showInterstitialAd {
                        navigate(R.id.addPlaylistFragment)
                    }

                }
                "upload_m3u"        -> {
                    showInterstitialAd {
                        navigate(
                            R.id.addPlaylistFragment,
                            Bundle().apply { putBoolean("isUpload", true) })
                    }

                }
                "add_single_stream" -> {
                    val bottomSheet = SingleStreamBottomSheet()
                    bottomSheet.show(supportFragmentManager, "SINGLE_STREAM")
                }
                "gallery"           -> {
                    showInterstitialAd {
                        navigate(R.id.addGalleryFragment)
                    }

                }
            }
        }

        navController?.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.splashFragment,
                R.id.watchChannelFragment,
                R.id.passcodeXtreamFragment -> {
                    binding.bar1.gone()
                    binding.fabAdd1.gone()
                }

                R.id.homeFragment -> {
                    setTabSelected(0)
                    showBar()
                }

                R.id.channelsFragment -> {
                    setTabSelected(1)
                    showBar()
                }

                R.id.xtreamHomeFragment,
                R.id.profileFragment,
                R.id.xtreamFragment,
                R.id.searchXtreamFragment -> {
                    setTabSelected(2)
                    showBar()
                }

                R.id.settingFragment -> {
                    setTabSelected(3)
                    showBar()
                }

                R.id.xtreamServerFragment,
                R.id.xtreamMovieDetailFragment,
                R.id.liveXtreamFragment,
                R.id.editProfileFragment,
                R.id.editAvatarFragment,
                R.id.howToYouFragment,
                R.id.categoryChannelFragment,
                R.id.listChannelFragment,
                R.id.lisChannelFavouriteFragment,
                R.id.IAPFragment,
                R.id.xtreamMovieFragment,
                R.id.streamFragment,
                R.id.episodeFragment,
                R.id.addPlaylistFragment -> {
                    binding.bar1.gone()
                    binding.fabAdd1.gone()
                }
            }
        }
    }

    private fun showBar() {
        binding.bar1.visible()
        binding.fabAdd1.visible()
    }

    override fun onResume() {
        super.onResume()
        updateStatusBarAppearance()

        try {
            if (navController?.currentDestination == null) return
//            AppMusicPlayer.checkAndPlay()

        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    override fun onPause() {
        super.onPause()
        AppMusicPlayer.stop()
        AppMusicPlayer.stopFxMusicPlayer()
    }

    override fun onDestroy() {
        AppMusicPlayer.releaseBackgroundMusic()
        AppMusicPlayer.releaseFxMusic()
        PlayerManager.releasePlayer()
        super.onDestroy()
    }

    fun navigateToLiveTv(channel: Channel?) {
        channel?.id?.let { id ->
            val appSharePref = AppSharePref(this)
            val idRecent = appSharePref.listIdRecent
            idRecent.remove(id)
            idRecent.add(id)
            appSharePref.listIdRecent = idRecent
        }
        binding.liveTvContainer.visibility = View.VISIBLE
        supportFragmentManager.beginTransaction()
            .replace(hoang.dqm.codebase.R.id.liveTvContainer, LiveFragment.newInstance(channel))
            .commit()

        binding.liveTvContainer.visibility = View.VISIBLE
        resetLiveTvContainer()
    }

    fun minimizeToMiniPlayer(channel: ChannelPopular) {
        if (isMiniPlayerMode) return
        isMiniPlayerMode = true
        currentMiniPlayerChannel = channel
        binding.liveTvContainer.visible()

        binding.liveTvContainer.post {
            val container = binding.liveTvContainer
            val params = container.layoutParams as ConstraintLayout.LayoutParams

            params.startToStart = ConstraintLayout.LayoutParams.UNSET
            params.topToTop = ConstraintLayout.LayoutParams.UNSET
            params.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
            params.bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
            val screenWidth = resources.displayMetrics.widthPixels

            val startWidth = container.width
            val startHeight = container.height

            val miniWidth = (screenWidth * 0.6f).toInt()
            val miniHeight = (miniWidth * 12f / 16f).toInt()

            val endMarginEnd = (16 * resources.displayMetrics.density).toInt()
            val endBottomMargin = (216 * resources.displayMetrics.density).toInt()

            ValueAnimator.ofFloat(0f, 1f).apply {
                duration = 300

                addUpdateListener {
                    val f = it.animatedFraction

                    params.width =
                        (startWidth + (miniWidth - startWidth) * f).toInt()
                    params.height =
                        (startHeight + (miniHeight - startHeight) * f).toInt()

                    params.marginEnd = (endMarginEnd * f).toInt()
                    params.bottomMargin = (endBottomMargin * f).toInt()

                    container.layoutParams = params
                }

                doOnEnd {
                    params.width = miniWidth
                    params.height = miniHeight
                    params.marginEnd = endMarginEnd
                    params.bottomMargin = endBottomMargin

                    container.layoutParams = params
                    container.elevation = 16f
                    getLiveFragment()?.minimize()
                }
                start()
            }
        }
    }


    private fun expandFromMiniPlayer() {
        if (!isMiniPlayerMode) return
        isMiniPlayerMode = false

        val container = binding.liveTvContainer
        val params = container.layoutParams as ConstraintLayout.LayoutParams

        val startWidth = container.width
        val startHeight = container.height
        val startMarginEnd = params.marginEnd
        val startBottomMargin = params.bottomMargin

        val screenWidth = resources.displayMetrics.widthPixels
        val screenHeight = resources.displayMetrics.heightPixels

        ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 300

            addUpdateListener {
                val f = it.animatedFraction

                params.width =
                    (startWidth + (screenWidth - startWidth) * f).toInt()
                params.height =
                    (startHeight + (screenHeight - startHeight) * f).toInt()

                params.marginEnd =
                    (startMarginEnd * (1f - f)).toInt()
                params.bottomMargin =
                    (startBottomMargin * (1f - f)).toInt()

                container.layoutParams = params
            }

            doOnEnd {
                params.apply {
                    width = ViewGroup.LayoutParams.MATCH_PARENT
                    height = 0
                    marginEnd = 0
                    bottomMargin = 0

                    startToStart = ConstraintLayout.LayoutParams.PARENT_ID
                    topToTop = ConstraintLayout.LayoutParams.PARENT_ID
                    endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
                    bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
                }

                container.layoutParams = params
                container.elevation = 0f
                container.isClickable = false
                getLiveFragment()?.expand()
            }

            start()
        }
    }


    @SuppressLint("ClickableViewAccessibility")
    private fun setupMiniPlayerGestures() {
        gestureDetector =
            GestureDetectorCompat(this, object : GestureDetector.SimpleOnGestureListener() {
                override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                    if (isMiniPlayerMode) {
                        expandFromMiniPlayer()
                        return true
                    }
                    return false
                }

                override fun onDown(e: MotionEvent): Boolean {
                    return isMiniPlayerMode
                }
            })

        binding.liveTvContainer.setOnTouchListener { view, event ->
            if (!isMiniPlayerMode) return@setOnTouchListener false
            gestureDetector.onTouchEvent(event)

            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = view.translationX
                    initialY = view.translationY
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }

                MotionEvent.ACTION_MOVE -> {
                    val deltaX = event.rawX - initialTouchX
                    val deltaY = event.rawY - initialTouchY

                    view.translationX = initialX + deltaX
                    view.translationY = initialY + deltaY
                    true
                }

                MotionEvent.ACTION_UP -> {
                    val deltaX = event.rawX - initialTouchX
                    val deltaY = event.rawY - initialTouchY

                    if (abs(deltaY) > 320) {
                        dismissMiniPlayer()
                    } else if (abs(deltaX) > 10 || abs(deltaY) > 10) {
                        snapToEdge()
                    }
                    true
                }

                else -> false
            }
        }
    }

    private fun snapToEdge() {
        val screenWidth = resources.displayMetrics.widthPixels
        val params = binding.liveTvContainer.layoutParams as ConstraintLayout.LayoutParams
        val viewCenterX = binding.liveTvContainer.translationX + params.width / 2

        val targetX = if (viewCenterX < screenWidth / 2) {
            16f
        } else {
            (screenWidth - params.width - 32f)
        }

        ValueAnimator.ofFloat(binding.liveTvContainer.translationX, targetX).apply {
            duration = 200
            addUpdateListener { animator ->
                binding.liveTvContainer.translationX = animator.animatedValue as Float
            }
            start()
        }
    }

    private fun dismissMiniPlayer() {
        val screenHeight = resources.displayMetrics.heightPixels
        val targetY = screenHeight.toFloat()

        ValueAnimator.ofFloat(binding.liveTvContainer.translationY, targetY).apply {
            duration = 250
            addUpdateListener { animator ->
                binding.liveTvContainer.translationY = animator.animatedValue as Float
                binding.liveTvContainer.alpha = 1f - animator.animatedFraction * 0.5f
            }
            doOnEnd {
                PlayerManager.releasePlayer()
                resetLiveTvContainer()
                binding.liveTvContainer.gone()
            }
            start()
        }
    }

    private fun resetLiveTvContainer() {
        isMiniPlayerMode = false

        val container = binding.liveTvContainer
        val params = container.layoutParams as ConstraintLayout.LayoutParams

        params.apply {
            width = 0
            height = 0

            startToStart = ConstraintLayout.LayoutParams.PARENT_ID
            endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
            topToTop = ConstraintLayout.LayoutParams.PARENT_ID
            bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID

            marginEnd = 0
            bottomMargin = 0
        }

        container.layoutParams = params

        container.translationX = 0f
        container.translationY = 0f
        container.scaleX = 1f
        container.scaleY = 1f
        container.alpha = 1f
        container.elevation = 0f
        container.isClickable = false
        container.visibility = View.VISIBLE
    }

    private fun getLiveFragment(): LiveTvController? {
        return supportFragmentManager
            .findFragmentById(hoang.dqm.codebase.R.id.liveTvContainer) as? LiveTvController
    }

    private fun closeLiveFragment() {
        isMiniPlayerMode = false

        PlayerManager.releasePlayer()

        supportFragmentManager.beginTransaction()
            .remove(
                supportFragmentManager.findFragmentById(hoang.dqm.codebase.R.id.liveTvContainer)
                    ?: return
            )
            .commit()

        resetLiveTvContainer()
    }

    private fun isLiveFragmentShowing(): Boolean {
        return supportFragmentManager
            .findFragmentById(hoang.dqm.codebase.R.id.liveTvContainer) is LiveFragment
    }

    override fun handleBackExit() {
        Log.d("handleBackExit", "handleBackExit: begin")
        if (isLiveFragmentShowing()) {
            Log.d("handleBackExit", "handleBackExit: live")
            closeLiveFragment()
        } else {
            super.handleBackExit()
        }
    }

    override fun attachBaseContext(context: Context) {
        val appPref = CommonAppSharePref(context)
        val locale = appPref.languageCode ?: Locale.getDefault().language
        Log.d("LangDebug", "Saved languageCode = ${appPref.languageCode}")
        Log.d("LangDebug", "Device default = ${Locale.getDefault().language}")
        Log.d("LangDebug", "Final locale used = $locale ${Locale(locale)}")

        val localeUpdatedContext: ContextWrapper =
            ContextUtils.updateLocale(context, Locale(locale))

        super.attachBaseContext(localeUpdatedContext)
    }
    private fun applyBottomNavigationBarInset() {
        val originalBarBottomMargin =
            (binding.bar1.layoutParams as ConstraintLayout.LayoutParams).bottomMargin

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { _, insets ->
            val bottomInset = insets.getInsets(
                WindowInsetsCompat.Type.navigationBars()
            ).bottom

            val hasNavigationBar = bottomInset > 0

            val barParams = binding.bar1.layoutParams as ConstraintLayout.LayoutParams
            barParams.bottomMargin = if (hasNavigationBar) {
                originalBarBottomMargin + bottomInset
            } else {
                originalBarBottomMargin
            }
            binding.bar1.layoutParams = barParams

            insets
        }

        ViewCompat.requestApplyInsets(binding.main)
    }

    private fun showSystemNavigationBar() {
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.show(WindowInsetsCompat.Type.navigationBars())
    }
}