# Splash Ads Configuration Guide

## 📖 Tổng quan

Hệ thống Splash đã được tích hợp với **Preload Ads Management**. Ads sẽ được preload ngay sau khi fetch config xong, đảm bảo ads sẵn sàng khi user vào app.

## 🔄 Flow

```
Splash Start
    ↓
Fetch Remote Config (0 -> 80%)
    ↓
Config Fetched ✅
    ↓
Preload Ads (80% -> 100%)
    ├─> Interstitial (85%)
    ├─> Native (90%)
    └─> Banner (95%)
    ↓
All Ads Preloaded ✅ (100%)
    ↓
Open Home
```

## 🎯 Cấu hình Ad Keys

### Bước 1: Mở SplashFragment.kt

File: `app/src/main/java/com/silverlabtech/iptv/smartplayer/ui/splash/SplashFragment.kt`

### Bước 2: Update Ad Keys

Tìm các hàm sau và thay thế bằng ad keys thực tế:

#### 1. Interstitial Ads

```kotlin
private fun getInterstitialAdKeys(): List<String> {
    return listOf(
        "ca-app-pub-xxx/home-inter-high",    // High eCPM
        "ca-app-pub-xxx/home-inter-medium",  // Medium eCPM
        "ca-app-pub-xxx/home-inter-low"      // Low eCPM
    )
}

private fun getBackupInterstitialKey(): String {
    return "ca-app-pub-xxx/backup-inter"
}
```

#### 2. Native Ads

```kotlin
private fun getNativeAdKeys(): List<String> {
    return listOf(
        "ca-app-pub-xxx/list-native",
        "ca-app-pub-xxx/detail-native",
        "ca-app-pub-xxx/home-native"
    )
}

private fun getBackupNativeKey(): String {
    return "ca-app-pub-xxx/backup-native"
}
```

#### 3. Banner Ads

```kotlin
private fun getBannerAdKeys(): List<String> {
    return listOf(
        "ca-app-pub-xxx/home-banner",
        "ca-app-pub-xxx/detail-banner"
    )
}
```

## 📝 Example với Ad Keys thực tế

```kotlin
// Interstitial
private fun getInterstitialAdKeys(): List<String> {
    return listOf(
        "ca-app-pub-3940256099942544/1033173712",  // Test ad
        "ca-app-pub-3940256099942544/1033173712",  // Test ad
    )
}

private fun getBackupInterstitialKey(): String {
    return "ca-app-pub-3940256099942544/1033173712"  // Test ad
}

// Native
private fun getNativeAdKeys(): List<String> {
    return listOf(
        "ca-app-pub-3940256099942544/2247696110",  // Test ad
        "ca-app-pub-3940256099942544/2247696110",  // Test ad
    )
}

private fun getBackupNativeKey(): String {
    return "ca-app-pub-3940256099942544/2247696110"  // Test ad
}

// Banner
private fun getBannerAdKeys(): List<String> {
    return listOf(
        "ca-app-pub-3940256099942544/6300978111",  // Test ad
    )
}
```

## 🎨 Tùy chỉnh

### 1. Thay đổi số lượng ads preload

```kotlin
override fun onPreloadAds(activity: Activity, onComplete: () -> Unit) {
    // Thay đổi totalAds nếu thêm/bớt loại ads
    var loadedCount = 0
    val totalAds = 3 // Interstitial, Native, Banner
    
    // ... rest of code
}
```

### 2. Thay đổi waterfall delay

```kotlin
// Delay giữa các lần load (ms)
adsManager.preloadInterstitialManagement.loadWithWaterfall(
    activity = activity,
    adKeys = interstitialKeys,
    delayMs = 500L  // Thay đổi từ 300L -> 500L
)
```

### 3. Disable preload một loại ads

```kotlin
private fun getInterstitialAdKeys(): List<String> {
    return emptyList()  // Không preload interstitial
}
```

### 4. Thay đổi progress percentage

```kotlin
override fun onPreloadAds(activity: Activity, onComplete: () -> Unit) {
    // Update UI: đang preload ads
    updateUI(85)  // Bắt đầu từ 85%
    
    val checkComplete = {
        loadedCount++
        // Update progress từ 85 -> 100
        val progress = 85 + (loadedCount * 5)  // Mỗi ad +5%
        updateUI(progress)
        
        if (loadedCount >= totalAds) {
            onComplete()
        }
    }
}
```

## 🔧 Advanced Configuration

### 1. Load từ Remote Config

```kotlin
private fun getInterstitialAdKeys(): List<String> {
    // Lấy từ remote config
    val remoteKeys = AppRemoteConfig.getData("interstitial_keys", Array<String>::class.java)
    return remoteKeys?.toList() ?: getDefaultInterstitialKeys()
}

private fun getDefaultInterstitialKeys(): List<String> {
    return listOf(
        "ca-app-pub-xxx/default-inter"
    )
}
```

### 2. Load theo điều kiện

```kotlin
private fun getInterstitialAdKeys(): List<String> {
    return if (isFirst()) {
        // First time user: load nhiều ads
        listOf(
            "ca-app-pub-xxx/first-time-inter-1",
            "ca-app-pub-xxx/first-time-inter-2",
            "ca-app-pub-xxx/first-time-inter-3"
        )
    } else {
        // Returning user: load ít ads hơn
        listOf(
            "ca-app-pub-xxx/returning-inter"
        )
    }
}
```

### 3. Load theo country

```kotlin
private fun getInterstitialAdKeys(): List<String> {
    val country = Locale.getDefault().country
    return when (country) {
        "US" -> listOf("ca-app-pub-xxx/us-inter")
        "VN" -> listOf("ca-app-pub-xxx/vn-inter")
        else -> listOf("ca-app-pub-xxx/default-inter")
    }
}
```

## 📊 Monitoring

### 1. Check ads preload status

```kotlin
override fun onAdsPreloadComplete() {
    Log.d("SplashFragment", """
        Ads Preload Complete:
        - Interstitial: ${adsManager.preloadInterstitialManagement.isLoaded("ad-key")}
        - Native: ${adsManager.preloadNativeManagement.isLoaded("ad-key")}
        - Banner: ${adsManager.preloadBannerManagement.isLoaded("ad-key")}
    """.trimIndent())
}
```

### 2. Track preload time

```kotlin
private var preloadStartTime = 0L

override fun onPreloadAds(activity: Activity, onComplete: () -> Unit) {
    preloadStartTime = System.currentTimeMillis()
    
    // ... preload logic
    
    val originalComplete = onComplete
    onComplete = {
        val duration = System.currentTimeMillis() - preloadStartTime
        Log.d("SplashFragment", "Preload duration: ${duration}ms")
        originalComplete()
    }
}
```

## ⚠️ Lưu ý

### 1. Test Ads

Luôn test với test ad IDs trước:
- Interstitial: `ca-app-pub-3940256099942544/1033173712`
- Native: `ca-app-pub-3940256099942544/2247696110`
- Banner: `ca-app-pub-3940256099942544/6300978111`

### 2. Production Ads

Khi deploy production:
1. Thay thế tất cả test ad IDs
2. Test kỹ trên nhiều devices
3. Monitor fill rate và eCPM

### 3. Memory

Không preload quá nhiều ads:
- Interstitial: 2-3 ads
- Native: 3-5 ads
- Banner: 1-2 ads

### 4. Timeout

Nếu ads load quá lâu, có thể thêm timeout:

```kotlin
override fun onPreloadAds(activity: Activity, onComplete: () -> Unit) {
    val timeout = 10000L // 10 seconds
    
    val handler = Handler(Looper.getMainLooper())
    val timeoutRunnable = Runnable {
        Log.w("SplashFragment", "Preload timeout, opening home anyway")
        onComplete()
    }
    
    handler.postDelayed(timeoutRunnable, timeout)
    
    // ... preload logic
    
    val originalComplete = onComplete
    onComplete = {
        handler.removeCallbacks(timeoutRunnable)
        originalComplete()
    }
}
```

## 🎉 Hoàn thành

Sau khi config xong:
1. ✅ Build và run app
2. ✅ Check logs để verify ads preload
3. ✅ Test navigation flow
4. ✅ Monitor performance

## 📞 Support

Nếu có vấn đề:
1. Check logs: `adb logcat | grep "SplashFragment\|BaseSplash"`
2. Verify ad keys
3. Check internet connection
4. Review [PRELOAD_ADS_README.md](../monetization/PRELOAD_ADS_README.md)

---

**Happy coding! 🚀**
