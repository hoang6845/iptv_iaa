package com.iptvplayer.m3u.stream.ui.splash

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.os.bundleOf
import com.iptvplayer.m3u.stream.R
import com.iptvplayer.m3u.stream.databinding.FragmentSplashBinding
import com.iptvplayer.m3u.stream.ui.language_activity.LanguageActivity
import hoang.dqm.codebase.base.activity.navigate
import hoang.dqm.codebase.base.activity.navigateFade
import hoang.dqm.codebase.base.activity.navigateWithIntermediate
import hoang.dqm.codebase.firebase.AppRemoteConfig
import hoang.dqm.codebase.service.session.isFirst
import hoang.dqm.codebase.service.session.saveFirst
import hoang.dqm.codebase.ui.features.splash.BaseSplashFragment
import hoang.dqm.codebase.utils.AppMonetization
import hoang.dqm.codebase.utils.ads
import hoang.dqm.codebase.utils.premium
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import tpt.dev.monetization.ads.AdsManager
import kotlin.random.Random
import kotlin.time.Duration.Companion.milliseconds

class SplashFragment : BaseSplashFragment<FragmentSplashBinding, SplashViewModel>() {
    private var job: Job? = null
    private var isPaused = false
    private var isInternetAvailable = true
    private val adsManager by lazy { AdsManager.getInstance() }
    private var hasNavigated = false
    private var isOpeningLanguage = false
    private val openLanguageLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->

            isOpeningLanguage = false

            if (hasNavigated) return@registerForActivityResult

            if (result.resultCode == Activity.RESULT_OK) {
                val goToIntro =
                    result.data?.getBooleanExtra("go_to_intro", false) ?: false

                if (goToIntro) {
                    hasNavigated = true
                    navigate(R.id.introFragment, isPop = true)
                }
            }
        }
    override fun openHome() {
        // Nếu đã subscribe, navigate luôn không cần show ads
        if (AppMonetization.premium.isSubscribed()) {
            Log.d("SplashFragment", "✓ User subscribed, skip splash ad and navigate directly")
            navigateToNextScreen()
            return
        }

        val splashAdKey = getString(R.string.ads_inter_splash)

        // Kiểm tra xem ad đã load chưa
        if (!adsManager.preloadInterstitialManagement.isLoaded(splashAdKey)) {
            // Đợi tối đa 3 giây để ad load
            var waitCount = 0
            val maxWait = 50 // 30 x 100ms = 3 giây

            CoroutineScope(Dispatchers.Main).launch {
                while (waitCount < maxWait && !adsManager.preloadInterstitialManagement.isLoaded(
                        splashAdKey
                    )
                ) {
                    delay(100)
                    waitCount++
                }

                if (adsManager.preloadInterstitialManagement.isLoaded(splashAdKey)) {
                    Log.d("SplashFragment", "✓ Splash ad loaded after ${waitCount * 100}ms")
                } else {
                    Log.e("SplashFragment", "✗ Splash ad still not loaded after 3s, showing anyway")
                }

                showSplashAdAndNavigate()
            }
        } else {
            Log.d("SplashFragment", "✓ Splash ad already loaded")
            showSplashAdAndNavigate()
        }
    }

    private fun showSplashAdAndNavigate() {
        if (!adsManager.isGlobalAdsEnabled()) {
            Log.d("SplashFragment", "Global ads disabled, skip splash ad")
            navigateToNextScreen()
            return
        }

        if (!adsManager.isInterSplashEnabled()) {
            Log.d("SplashFragment", "Inter splash disabled, skip splash ad")
            navigateToNextScreen()
            return
        }

        adsManager.preloadInterstitialManagement.show(
            requireActivity(), getString(R.string.ads_inter_splash), true, null, onAdClosed = {
                Log.d(
                    "SplashFragment",
                    "Ad closed, navigating to ${if (isFirst()) "intro" else "home"}"
                )
                navigateToNextScreen()
            })
    }

    private fun navigateToNextScreen() {
        if (hasNavigated || isOpeningLanguage) return

        adsManager.markSplashCompleted()

        if (isFirst()) {
            isOpeningLanguage = true
            saveFirst(false)
            val intent = Intent(requireContext(), LanguageActivity::class.java).apply {
                putExtra("isFromSplash", true)
            }

            openLanguageLauncher.launch(intent)

        } else {
            hasNavigated = true

            if (AppMonetization.premium.isSubscribed()) {
                navigate(R.id.homeFragment, isPop = true)
            } else {
                val bundle = Bundle().apply {
                    putBoolean("isFromSplash", true)
                }

                navigate(R.id.homeFragment, bundle, isPop = true)
            }
        }
    }

    override fun initView() {
        super.initView()
        binding.ballView.setBallDrawable(R.drawable.ic_soccer_ball)
        binding.ballView.startBounce()
//        AppMonetization.premium.updateSubscribedState(true)
        if (isFirst()) {
            binding.tvLoading.text =
                resources.getStringArray(R.array.text_first_time).random()
        } else {
            binding.tvLoading.text = getString(R.string.loading_text)
        }
        setupLoading()
        checkConsentShow()
    }

    override fun isInternetConnected(isInternet: Boolean) {
        isInternetAvailable = isInternet
        isPaused = !isInternet
    }

    override fun onFetchConfigSuccess() {
        Log.d("SplashFragment", "=== Config fetched, updating to 80% ===")
        job?.cancel()

        loadRemoteConfigVariables()

        // Nếu đã subscribe, update lên 100% luôn
        if (AppMonetization.premium.isSubscribed()) {
            updateUI(100)
        } else {
            updateUI(80)
        }
    }

    /**
     * Override để check subscription status
     */
    override fun isUserSubscribed(): Boolean {
        val isSubscribed = AppMonetization.premium.isSubscribed()
        Log.d("SplashFragment", "Checking subscription: isSubscribed=$isSubscribed")
        return isSubscribed
    }

    fun loadRemoteConfigVariables() {
        // Load timing configs
        val timeDelayInterSplashVsOpen = AppRemoteConfig.getLongValue(
            AppRemoteConfig.TIME_DELAY_INTER_SPLASH_OPEN, 20000
        )
        val timeDelayInter = AppRemoteConfig.getLongValue(
            AppRemoteConfig.TIME_DELAY_SHOW_INTER, 20000
        )

        val isShowAdsOpen = AppRemoteConfig.getBooleanValue(
            AppRemoteConfig.IS_SHOW_AD_OPEN, true
        )
        val isShowAdsApp = AppRemoteConfig.getBooleanValue(
            AppRemoteConfig.IS_SHOW_ADS_APP, true
        )
        val isShowInterAfterSplash = AppRemoteConfig.getBooleanValue(
            AppRemoteConfig.IS_SHOW_INTER_SPLASH, true
        )

        // Áp dụng cấu hình timing
        AppMonetization.ads.updateTimeIntervalShowInterVsOpen(timeDelayInterSplashVsOpen.milliseconds)
        AppMonetization.ads.updateTimeIntervalShowInterstitialAd(timeDelayInter.milliseconds)

        // Áp dụng cấu hình enable/disable
        AppMonetization.ads.setGlobalAdsEnabled(isShowAdsApp)
        AppMonetization.ads.setAppOpenAdEnabled(isShowAdsOpen)
        AppMonetization.ads.setInterSplashEnabled(isShowInterAfterSplash)

        // Log để debug
        Log.d(
            "RemoteConfig", """
            ===== ADS CONFIG =====
            Time Inter ↔ Open: ${timeDelayInterSplashVsOpen}ms
            Time Inter Interval: ${timeDelayInter}ms
            Show Open Ad: $isShowAdsOpen
            Show All Ads: $isShowAdsApp
            Show Inter Splash: $isShowInterAfterSplash
            ======================
        """.trimIndent()
        )
    }

    /**
     * Override để preload ads
     */
    override fun onPreloadAds(activity: Activity, onComplete: () -> Unit) {
        Log.d("SplashFragment", "=== Starting preload ads ===")

        // Update UI: đang preload ads
        updateUI(85)

        // Đếm số ads cần load
        var loadedCount = 0
        var isCompleted = false
        val totalAds = 1 // Interstitial, Native, Banner

        // Timeout sau 10 giây để tránh bị treo
        CoroutineScope(Dispatchers.Main).launch {
            delay(10000L)
            if (!isCompleted) {
                Log.e(
                    "SplashFragment",
                    "!!! TIMEOUT: Ads preload took too long, forcing complete !!!"
                )
                isCompleted = true
                onComplete()
            }
        }

        val checkComplete = {
            loadedCount++
            Log.d("SplashFragment", ">>> Ads loaded: $loadedCount/$totalAds")

            // Update progress
            val progress = 85 + (loadedCount * 5) // 85 -> 100
            updateUI(progress)

            if (loadedCount >= totalAds && !isCompleted) {
                Log.d("SplashFragment", "=== All ads preloaded successfully ===")
                isCompleted = true
                onComplete()
            }
        }

        // 1. Preload Interstitial ads với waterfall
        val interstitialKeys = getInterstitialAdKeys()
        Log.d("SplashFragment", "Interstitial keys: $interstitialKeys")
        if (interstitialKeys.isNotEmpty()) {
            Log.d("SplashFragment", "Loading ${interstitialKeys.size} interstitial ads...")
            adsManager.preloadInterstitialManagement.loadWithWaterfall(
                activity = activity,
                adKeys = interstitialKeys,
                delayMs = 300L
            ) {
                Log.d("SplashFragment", "✓ Interstitial ads preloaded")
                checkComplete()
            }

            // Enable backup
            val backupKey = getBackupInterstitialKey()
            if (backupKey.isNotEmpty()) {
                Log.d("SplashFragment", "Enabling backup interstitial: $backupKey")
                adsManager.preloadInterstitialManagement.enableBackup(
                    activity,
                    backupKey
                )
            }
        } else {
            Log.w("SplashFragment", "⚠ No interstitial keys, skipping...")
            checkComplete()
        }

        // 2. Preload Native ads với waterfall
        val nativeKeys = getNativeAdKeys()
        Log.d("SplashFragment", "Native keys: $nativeKeys")
        if (nativeKeys.isNotEmpty()) {
            Log.d("SplashFragment", "Loading ${nativeKeys.size} native ads...")
            adsManager.preloadNativeManagement.loadWithWaterfall(
                activity = activity,
                adKeys = nativeKeys,
                delayMs = 300L
            ) {
                Log.d("SplashFragment", "✓ Native ads preloaded")
                checkComplete()
            }

            // Enable backup
            val backupKey = getBackupNativeKey()
            if (backupKey.isNotEmpty()) {
                Log.d("SplashFragment", "Enabling backup native: $backupKey")
                adsManager.preloadNativeManagement.enableBackup(
                    activity,
                    backupKey
                )
            }
        } else {
            Log.w("SplashFragment", "⚠ No native keys, skipping...")
            checkComplete()
        }

        // 3. Preload Banner ads
        val bannerKeys = getBannerAdKeys()
        Log.d("SplashFragment", "Banner keys: $bannerKeys")
        if (bannerKeys.isNotEmpty()) {
            Log.d("SplashFragment", "Loading ${bannerKeys.size} banner ads...")
            adsManager.preloadBannerManagement.loadWithWaterfall(
                activity = activity,
                adKeys = bannerKeys,
                delayMs = 300L
            ) {
                Log.d("SplashFragment", "✓ Banner ads preloaded")
                checkComplete()
            }
        } else {
            Log.w("SplashFragment", "⚠ No banner keys, skipping...")
            checkComplete()
        }
    }

    override fun onAdsPreloadComplete() {
        Log.d("SplashFragment", "=== Ads preload complete, updating to 100% ===")
        // Update UI to 100% khi ads preload xong
        updateUI(100)
    }

    /**
     * Get danh sách Interstitial ad keys theo priority
     * TODO: Lấy từ remote config hoặc constants
     */
    private fun getInterstitialAdKeys(): List<String> {
        return listOf(
            getString(R.string.ads_inter_splash),
            getString(R.string.full_back)
        )
    }

    /**
     * Get backup interstitial key
     */
    private fun getBackupInterstitialKey(): String {
        // TODO: Replace với backup ad key thực tế
        return "" // "ca-app-pub-xxx/backup-inter"
    }

    /**
     * Get danh sách Native ad keys
     */
    private fun getNativeAdKeys(): List<String> {
        return listOf(
            getString(R.string.ads_native_language_id),
            getString(R.string.ads_native_language_click),
            getString(R.string.ads_native_intro1),
            getString(R.string.ads_native_intro2),
            getString(R.string.ads_native_intro_full_id),
            getString(R.string.ads_native_home),
            getString(R.string.ads_collapse_channel),
        )
    }

    /**
     * Get backup native key
     */
    private fun getBackupNativeKey(): String {
        return "" // "ca-app-pub-xxx/backup-native"
    }

    /**
     * Get danh sách Banner ad keys
     */
    private fun getBannerAdKeys(): List<String> {
        return listOf(
            // TODO: Replace với ad keys thực tế
            // "ca-app-pub-xxx/home-banner"
        )
    }

    private fun setupLoading() {
        binding.apply {
//            tvPercentLoading.visible()
            progressBar.max = 100
            progressBar.progress = 0
            updateUI(0)
        }
        startLoading()
    }

    private suspend fun waitIfPaused() {
        while (isPaused) {
            delay(500)
        }
    }

    private fun startLoading() {
        job = CoroutineScope(Dispatchers.Main).launch {
            delay(1000L)
            var progress = 0

            // Phase 1: 0 -> 70% (loading config)
            while (progress < 60) {
                waitIfPaused()
                progress += 1
                updateUI(progress)
                delay(Random.nextLong(50, 200))
            }

            // Phase 2: 70 -> 80% (waiting for config)
            while (progress < 70) {
                waitIfPaused()
                progress += 1
                updateUI(progress)
                delay(Random.nextLong(500, 1200))
            }

            // Phase 3: 80 -> 100% sẽ được handle bởi preload ads
            while (progress < 80) {
                waitIfPaused()
                progress += 1
                updateUI(progress)
                delay(Random.nextLong(1000, 1500))
            }
        }
    }

    private fun updateUI(progress: Int) {
        if (!isAdded || isDetached || view == null) return
        binding.progressBar.setProgressCompat(progress, true)
        binding.tvPercentLoading.text = "$progress%"
    }

    override fun onDestroy() {
        super.onDestroy()
        job?.cancel()
        binding.ballView.stopBounce()
    }

    private fun checkConsentShow() {
        view?.viewTreeObserver?.addOnWindowFocusChangeListener { hasFocus ->
            isPaused = if (!hasFocus) {
                true
            } else {
                !isInternetAvailable
            }
        }
    }
}
