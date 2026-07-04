package com.iptvplayer.m3u.stream

import android.content.Context
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.google.android.gms.cast.framework.CastOptions
import com.google.android.gms.cast.framework.OptionsProvider
import com.google.android.gms.cast.framework.SessionProvider
import com.iptvplayer.m3u.stream.utils.ThemeManager
import com.makeramen.roundedimageview.BuildConfig
import com.iptvplayer.m3u.stream.utils.AdsConstants
import com.iptvplayer.m3u.stream.utils.AppConstants
import hoang.dqm.codebase.utils.AppMonetization
import hoang.dqm.codebase.utils.premium
import dagger.hilt.android.HiltAndroidApp
import tpt.dev.monetization.subs.model.IAPProduct
import tpt.dev.monetization.subs.model.IAPProductType
import hoang.dqm.codebase.base.application.BaseApplication
import hoang.dqm.codebase.data.AppInfo
import hoang.dqm.codebase.firebase.SeverRemoteConfig
import hoang.dqm.codebase.utils.billing
import tpt.dev.monetization.ads.AdsManager
import javax.inject.Inject

@HiltAndroidApp
class MainApplication : BaseApplication(), Configuration.Provider, OptionsProvider {
    override fun getCastOptions(p0: Context): CastOptions {
        return CastOptions.Builder()
            .setReceiverApplicationId("CC1AD845")
            .build()
    }

    override fun getAdditionalSessionProviders(context: Context)
            : MutableList<SessionProvider>? = null

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()


    override val appInfo: AppInfo by lazy {
        AppInfo(
            appId = BuildConfig.APPLICATION_ID,
            icon = R.mipmap.ic_launcher,
            appName = ContextCompat
                .getContextForLanguage(this)
                .getString(R.string.app_name),
            rawGit = SeverRemoteConfig.DATA_BASE_URL_GITHUB,
            isDebug = false,
            policy = AppConstants.policy,
            emailFeedback = "",
            term = AppConstants.term,
        )
    }

    override fun onCreate() {
        super.onCreate()
        ThemeManager.applyTheme(this)

        instance = this
        Log.d("MainApplication", "=== App onCreate ===")
        
        // Kotpref is auto-initialized via AndroidX Startup:
        // PremiumInitializer (registered in monetization/AndroidManifest.xml)
        // declares KotprefInitializer as dependency, ensuring proper init order
        
        // 1. Apply theme

        // 2. Initialize AdsManager (chưa init MobileAds)
        Log.d("MainApplication", "Initializing AdsManager...")
        AdsManager.initialize(this, AppMonetization.premium)
        
        // 3. Configure ads với ad IDs
        Log.d("MainApplication", "Configuring AdsManager...")
        AdsManager.getInstance().configure(
            adsConstants = AdsConstants,
            disableAppOpenAdActivities = emptyList(), // Có thể thêm activities không show app open ad
            isAllowShowOpenAd = true
        )
        Log.d("MainApplication", "✓ AdsManager configured")
        
        // NOTE: Không gọi MobileAds.initialize() ở đây!
        // Phải gọi gatherConsent() trong SplashFragment trước
        // Sau khi có consent, AdsManager sẽ tự động init MobileAds

        Log.d("IAP_DEBUG", "=== Configuring Billing Manager ===")
        AppMonetization.billing.configure(
            iapProducts = listOf(
                IAPProduct(
                    productType = IAPProductType.Subscription,
                    productId = getString(R.string.billing_sub_week)
                ),
                IAPProduct(
                    productType = IAPProductType.Subscription,
                    productId = getString(R.string.billing_sub_year)
                )
            )
        )
        Log.d("IAP_DEBUG", "✓ Billing Manager configured")
    }

    companion object {
        lateinit var instance: MainApplication
            private set
    }
}