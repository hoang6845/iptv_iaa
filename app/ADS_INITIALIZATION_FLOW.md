# Ads Initialization Flow

## Tổng quan

Flow khởi tạo ads đúng chuẩn theo Google UMP (User Messaging Platform) và AdMob best practices.

## Flow chi tiết

```
1. MainApplication.onCreate()
   ├─ AdsManager.initialize()        // Khởi tạo instance
   └─ AdsManager.configure()          // Config ad IDs
   
2. BaseSplashFragment.initView()
   └─ gatherConsentAndFetch()
      ├─ AdsManager.gatherConsent()   // ✅ Xin consent từ user (UMP)
      │  └─ Show consent dialog nếu cần
      │  └─ Callback khi xong:
      │     └─ initializeMobileAdsSdk()  // ✅ Init MobileAds (chỉ 1 lần)
      │        └─ MobileAds.initialize()
      │        └─ Load interstitial ads
      │
      └─ subscribeEventNetwork()
         └─ fetch()                    // Fetch remote config
            └─ preloadAds()            // ✅ Preload ads (sau khi init xong)
               ├─ Load interstitial waterfall
               ├─ Load native waterfall
               └─ Load banner waterfall
```

## Các bước quan trọng

### 1. MainApplication.onCreate()

```kotlin
// ✅ ĐÚNG
AdsManager.initialize(this, premiumManager)
AdsManager.getInstance().configure(
    adsConstants = AdsConstants,
    disableAppOpenAdActivities = emptyList(),
    isAllowShowOpenAd = true
)

// ❌ SAI - Không gọi MobileAds.initialize() ở đây
// MobileAds.initialize(this)  // KHÔNG!
```

**Lý do:** Phải gather consent trước khi init MobileAds theo quy định GDPR/CCPA.

### 2. BaseSplashFragment.initView()

```kotlin
// ✅ ĐÚNG - Gọi gatherConsent trước
AdsManager.getInstance().gatherConsent(activity) { error ->
    // Sau khi có consent, AdsManager tự động gọi:
    // - MobileAds.initialize()
    // - Load interstitial ads
    
    // Tiếp tục fetch config
    fetch()
}
```

### 3. Preload Ads (sau khi init xong)

```kotlin
// ✅ ĐÚNG - Preload sau khi MobileAds đã init
override fun onPreloadAds(activity: Activity, onComplete: () -> Unit) {
    // Load interstitial waterfall
    adsManager.preloadInterstitialManagement.loadWithWaterfall(...)
    
    // Load native waterfall
    adsManager.preloadNativeManagement.loadWithWaterfall(...)
    
    // Load banner waterfall
    adsManager.preloadBannerManagement.loadWithWaterfall(...)
}
```

## Timeline

```
0ms   : App start
10ms  : MainApplication.onCreate()
       ├─ AdsManager.initialize()
       └─ AdsManager.configure()
       
100ms : SplashFragment.initView()
       └─ gatherConsent() START
       
500ms : User sees consent dialog (nếu cần)
       
2s    : User accepts/declines consent
       └─ gatherConsent() COMPLETE
          └─ MobileAds.initialize() START (background thread)
          
2.5s  : MobileAds.initialize() COMPLETE
       └─ Load interstitial ads START
       
3s    : fetch() START
       └─ Firebase Remote Config
       
4s    : fetch() COMPLETE (80%)
       └─ preloadAds() START
          ├─ Load interstitial waterfall
          ├─ Load native waterfall
          └─ Load banner waterfall
          
6s    : preloadAds() COMPLETE (100%)
       └─ openHome()
```

## Lưu ý quan trọng

### ✅ DO (Nên làm)

1. **Gọi gatherConsent() trước khi load ads**
   - Tuân thủ GDPR/CCPA
   - Tránh bị reject app trên Google Play

2. **Chỉ init MobileAds 1 lần**
   - AdsManager đã handle việc này
   - Không gọi `MobileAds.initialize()` thủ công

3. **Preload ads sau khi init xong**
   - Đợi `gatherConsent()` callback
   - Đợi `MobileAds.initialize()` complete

4. **Handle timeout**
   - Consent dialog có thể bị dismiss
   - Ads có thể load fail
   - Luôn có fallback để không block user

### ❌ DON'T (Không nên làm)

1. **Không gọi MobileAds.initialize() trong Application.onCreate()**
   ```kotlin
   // ❌ SAI
   override fun onCreate() {
       MobileAds.initialize(this)  // Chưa có consent!
   }
   ```

2. **Không load ads trước khi init**
   ```kotlin
   // ❌ SAI
   override fun onCreate() {
       loadInterstitialAd()  // MobileAds chưa init!
   }
   ```

3. **Không skip consent**
   ```kotlin
   // ❌ SAI
   override fun initView() {
       fetch()  // Bỏ qua gatherConsent()
   }
   ```

## Testing

### Test với real ads

1. Thay test ad IDs trong `AdsConstants.kt`:
   ```kotlin
   override val ADMOB_INTERSTITIAL_ID = "ca-app-pub-xxxxx/yyyyy"
   ```

2. Xem log để verify flow:
   ```
   MainApplication: Initializing AdsManager...
   MainApplication: ✓ AdsManager configured
   BaseSplash: Calling gatherConsent...
   BaseSplash: ✓ Consent gathered successfully
   BaseSplash: === Fetching remote config ===
   SplashFragment: === Starting preload ads ===
   SplashFragment: ✓ Interstitial ads preloaded
   ```

### Test consent dialog

1. Clear app data để reset consent
2. Launch app → sẽ thấy consent dialog
3. Accept/Decline → ads sẽ load theo consent status

## Troubleshooting

### Ads không load

1. Check log: `MobileAds initialized`
2. Check consent: `Consent gathered successfully`
3. Check ad IDs: Đúng format `ca-app-pub-xxxxx/yyyyy`
4. Check internet connection

### Consent dialog không hiện

1. User đã accept/decline trước đó
2. Clear app data để test lại
3. Check region: Consent chỉ bắt buộc ở EU/EEA

### App bị treo ở splash

1. Check timeout mechanism (10s)
2. Check callback được gọi
3. Check `isConfigFetched` và `isAdsPreloaded` flags

## References

- [Google UMP SDK](https://developers.google.com/admob/ump/android/quick-start)
- [AdMob Best Practices](https://developers.google.com/admob/android/quick-start)
- [GDPR Compliance](https://support.google.com/admob/answer/10113207)
