# Fix App Open Ad Logic

## Vấn đề

App Open Ad đang được show cả khi:
1. ✅ Comeback từ background (đúng - đây là mục đích)
2. ❌ Mở app lần đầu / cold start (sai - phải show ads trong Splash)
3. ❌ Mở lại app sau khi kill (sai - phải show ads trong Splash)

### Nguyên nhân

`ProcessLifecycleOwner.onResume()` được trigger khi app process chuyển sang foreground, **bất kể là cold start hay warm start**.

```kotlin
// AdsManager.kt - TRƯỚC KHI FIX
override fun onResume(owner: LifecycleOwner) {
    super.onResume(owner)
    if (checkIsShowingFullScreenAd()) {
        return
    }
    
    val currentActivity = activityLifecycleCallbacks.currentActivity as? AppCompatActivity ?: return
    
    if (disableAppOpenAdActivities.any { it.name == currentActivity.javaClass.name }) {
        return
    }
    
    if (appOpenLoadingAdsDialog?.isShowing == true) {
        return
    }
    
    // ❌ Luôn show App Open Ad, kể cả cold start
    showAppOpenAdWithLoading(currentActivity)
}
```

## Giải pháp

Thêm flag `hasCompletedSplash` để phân biệt:
- **Cold start** (chưa qua splash): `hasCompletedSplash = false` → KHÔNG show App Open Ad
- **Warm start** (đã qua splash, comeback từ background): `hasCompletedSplash = true` → SHOW App Open Ad

### 1. Thêm flag trong AdsManager

```kotlin
// AdsManager.kt
class AdsManager private constructor(
    private val application: Application,
    private val premiumManager: IPremiumManager
) : DefaultLifecycleObserver {
    // ... existing code ...
    
    // Flag để track xem app đã qua splash chưa
    // true = đã qua splash, có thể show App Open Ad khi comeback
    // false = chưa qua splash (cold start), không show App Open Ad
    private var hasCompletedSplash = AtomicBoolean(false)
    
    // ... existing code ...
}
```

### 2. Check flag trong onResume()

```kotlin
// AdsManager.kt
override fun onResume(owner: LifecycleOwner) {
    super.onResume(owner)
    
    // ✅ QUAN TRỌNG: Chỉ show App Open Ad khi đã qua splash
    // Cold start (lần đầu mở app): hasCompletedSplash = false -> không show
    // Warm start (comeback từ background): hasCompletedSplash = true -> show
    if (!hasCompletedSplash.get()) {
        return
    }
    
    if (checkIsShowingFullScreenAd()) {
        return
    }

    val currentActivity = activityLifecycleCallbacks.currentActivity as? AppCompatActivity ?: return

    if (disableAppOpenAdActivities.any { it.name == currentActivity.javaClass.name }) {
        return
    }

    if (appOpenLoadingAdsDialog?.isShowing == true) {
        return
    }

    showAppOpenAdWithLoading(currentActivity)
}
```

### 3. Thêm public method để set flag

```kotlin
// AdsManager.kt
/**
 * Đánh dấu rằng app đã hoàn thành splash screen
 * Sau khi gọi hàm này, App Open Ad sẽ được show khi comeback từ background
 * 
 * GỌI HÀM NÀY TRONG SPLASH FRAGMENT SAU KHI NAVIGATE ĐẾN HOME
 */
fun markSplashCompleted() {
    hasCompletedSplash.set(true)
}

/**
 * Reset flag splash (dùng cho testing hoặc logout)
 */
fun resetSplashFlag() {
    hasCompletedSplash.set(false)
}
```

### 4. Gọi markSplashCompleted() trong SplashFragment

```kotlin
// SplashFragment.kt
override fun openHome() {
    Log.d("SplashFragment", "=== Opening home screen ===")
    adsManager.preloadInterstitialManagement.show(
        requireActivity(), getString(R.string.ads_inter_splash), true, null, onAdClosed = {
            Log.d("SplashFragment", "Ad closed, navigating to ${if (isFirst()) "intro" else "home"}")
            
            // ✅ QUAN TRỌNG: Đánh dấu đã hoàn thành splash
            // Từ giờ App Open Ad sẽ được show khi comeback từ background
            adsManager.markSplashCompleted()
            
            if (isFirst()){
                navigate(R.id.introFragment, isPop = true)
                saveFirst(false)
            }else {
                navigate(R.id.homeFragment, isPop = true)
            }
        })
}
```

## Luồng hoạt động sau khi fix

### Scenario 1: Cold Start (Mở app lần đầu hoặc sau khi kill)

```
1. User mở app
2. MainApplication.onCreate()
   - AdsManager.initialize()
   - hasCompletedSplash = false
3. ProcessLifecycleOwner.onResume() được gọi
   - AdsManager.onResume() check hasCompletedSplash = false
   - ❌ RETURN - không show App Open Ad
4. SplashFragment hiển thị
   - Gather consent
   - Fetch config
   - Preload ads
   - Show Interstitial Ad
5. Navigate to Home
   - ✅ adsManager.markSplashCompleted()
   - hasCompletedSplash = true
6. User sử dụng app bình thường
```

### Scenario 2: Warm Start (Comeback từ background)

```
1. User đang ở Home, nhấn Home button (app vào background)
2. User mở lại app
3. ProcessLifecycleOwner.onResume() được gọi
   - AdsManager.onResume() check hasCompletedSplash = true
   - ✅ PASS - tiếp tục check các điều kiện khác
   - checkIsShowingFullScreenAd() = false
   - currentActivity không trong disableAppOpenAdActivities
   - appOpenLoadingAdsDialog không đang show
   - ✅ showAppOpenAdWithLoading() - SHOW App Open Ad
4. User xem App Open Ad
5. Quay lại màn hình trước đó
```

### Scenario 3: Kill app và mở lại

```
1. User force kill app (swipe away từ recent apps)
2. User mở lại app
3. MainApplication.onCreate()
   - AdsManager.initialize()
   - hasCompletedSplash = false (reset về false vì app bị kill)
4. ProcessLifecycleOwner.onResume() được gọi
   - AdsManager.onResume() check hasCompletedSplash = false
   - ❌ RETURN - không show App Open Ad
5. SplashFragment hiển thị (giống Scenario 1)
   - Show Interstitial Ad trong splash
   - markSplashCompleted()
```

## Testing

### Test Case 1: Cold Start
1. Force kill app
2. Mở app
3. ✅ Expected: Không show App Open Ad, show Interstitial trong Splash

### Test Case 2: Warm Start
1. Mở app, đợi vào Home
2. Nhấn Home button (app vào background)
3. Mở lại app
4. ✅ Expected: Show App Open Ad

### Test Case 3: Kill và mở lại
1. Mở app, đợi vào Home
2. Force kill app (swipe away)
3. Mở lại app
4. ✅ Expected: Không show App Open Ad, show Interstitial trong Splash

### Test Case 4: Multiple comeback
1. Mở app, đợi vào Home
2. Nhấn Home button
3. Mở lại app → ✅ Show App Open Ad
4. Nhấn Home button lại
5. Mở lại app → ✅ Show App Open Ad (nếu đủ time interval)

## Notes

- Flag `hasCompletedSplash` là **in-memory**, khi app bị kill thì reset về `false`
- Không cần persist flag này vào SharedPreferences vì đó chính là behavior mong muốn
- Nếu cần disable App Open Ad cho một số activities, thêm vào `disableAppOpenAdActivities` trong `MainApplication.configure()`
- Time interval giữa các lần show App Open Ad được control bởi `timeIntervalShowInterVsOpen`

## Related Files

- `monetization/src/main/java/tpt/dev/monetization/ads/AdsManager.kt`
- `app/src/main/java/com/silverlabtech/iptv/smartplayer/ui/splash/SplashFragment.kt`
- `app/src/main/java/com/silverlabtech/iptv/smartplayer/MainApplication.kt`
