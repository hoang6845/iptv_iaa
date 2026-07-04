# Native Ads Preload Optimization

## Vấn đề hiện tại

### Tình trạng
Mặc dù ở màn hình **SplashFragment** đã preload nhiều Native ads:

```kotlin
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
```

Nhưng ở **HomeFragment** và **ChannelsFragment** vẫn load ads rất lâu:

```kotlin
// HomeFragment.kt - line 53
loadSingleNative(binding.viewNativeAd, R.string.ads_native_home, updateTimeout = false)

// ChannelsFragment.kt - line 106
loadSingleNative(binding.viewNativeAd, R.string.ads_collapse_channel, updateTimeout = false)
```

### Nguyên nhân
- `loadSingleNative()` là method load ads **MỚI** từ đầu (gọi AdMob API)
- Các ads đã được preload trong pool **KHÔNG được sử dụng**
- Dẫn đến user phải đợi lại khi vào Home/Channels

---

## Giải pháp

### Sử dụng Extension Functions để lấy Preloaded Ads

Thay vì gọi `loadSingleNative()`, cần sử dụng:

```kotlin
// Lấy native ad đã preload (có backup)
val nativeAd = getNativeAdOrBackup(
    adKey = getString(R.string.ads_native_home),
    removeAfterGet = true  // Remove sau khi lấy để tránh show duplicate
)
```

Hoặc:

```kotlin
// Lấy native ad đã preload (không có backup)
val nativeAd = getNativeAd(
    adKey = getString(R.string.ads_native_home),
    removeAfterGet = true
)
```

### Workflow đề xuất

```kotlin
// 1. Kiểm tra xem ad đã được preload chưa
if (isNativeAdLoaded(getString(R.string.ads_native_home))) {
    
    // 2. Lấy ad từ pool (instant - không cần load)
    val nativeAd = getNativeAdOrBackup(
        adKey = getString(R.string.ads_native_home),
        removeAfterGet = true
    )
    
    if (nativeAd != null) {
        // 3. Show ad ngay lập tức
        populateNativeAdView(binding.viewNativeAd, nativeAd)
        
        // 4. Preload lại cho lần sau
        loadNativeAd(
            adKey = getString(R.string.ads_native_home),
            numberOfAds = 1
        )
    } else {
        // Fallback: Load mới nếu pool rỗng
        loadSingleNative(binding.viewNativeAd, R.string.ads_native_home)
    }
    
} else {
    // Pool chưa có -> Load mới
    loadSingleNative(binding.viewNativeAd, R.string.ads_native_home)
}
```

---

## Implementation Plan

### 1. Tạo Helper Method trong BaseFragment

Tạo helper method để xử lý logic show preloaded native ad:

```kotlin
// BaseFragment.kt
fun showPreloadedNativeOrLoad(
    nativeAdView: ViewGroup,
    @StringRes adKeyResId: Int,
    updateTimeout: Boolean = false
) {
    val adKey = getString(adKeyResId)
    
    // Kiểm tra ad đã preload chưa
    if (isNativeAdLoaded(adKey)) {
        // Lấy ad từ pool
        val nativeAd = getNativeAdOrBackup(
            adKey = adKey,
            removeAfterGet = true
        )
        
        if (nativeAd != null) {
            // Show ad ngay lập tức
            populateNativeAdView(nativeAdView, nativeAd)
            
            // Reload cho lần sau
            loadNativeAd(adKey = adKey, numberOfAds = 1)
            return
        }
    }
    
    // Fallback: Load mới
    loadSingleNative(nativeAdView, adKeyResId, updateTimeout)
}
```

### 2. Update HomeFragment

```kotlin
// HomeFragment.kt
override fun initView() {
    adjustInsetsForBottomNavigation(binding.toolBar)
    binding.viewPager.adapter = FragmentHomeAdapter(childFragmentManager, viewLifecycleOwner.lifecycle)
    // ... other setup code ...
    
    setUpRecentAdapter()
    
    // ❌ CŨ: Load mới mỗi lần
    // loadSingleNative(binding.viewNativeAd, R.string.ads_native_home, updateTimeout = false)
    
    // ✅ MỚI: Dùng preloaded ad
    showPreloadedNativeOrLoad(
        binding.viewNativeAd, 
        R.string.ads_native_home, 
        updateTimeout = false
    )
}
```

### 3. Update ChannelsFragment

```kotlin
// ChannelsFragment.kt
override fun initView() {
    adjustInsetsForBottomNavigation(binding.root)
    // ... other setup code ...
    
    setUpAdapter()
    seekBar = binding.playerView.findViewById<SeekBar>(R.id.seekBar)
    
    // ... adapter load state listener ...
    
    // ❌ CŨ: Load mới mỗi lần
    // loadSingleNative(binding.viewNativeAd, R.string.ads_collapse_channel, updateTimeout = false)
    
    // ✅ MỚI: Dùng preloaded ad
    showPreloadedNativeOrLoad(
        binding.viewNativeAd,
        R.string.ads_collapse_channel,
        updateTimeout = false
    )
}
```

---

## Lợi ích

### 1. **Hiển thị ads nhanh hơn**
- Ads đã được preload ở splash → Show ngay lập tức
- Không cần đợi load từ AdMob API
- User experience tốt hơn

### 2. **Tiết kiệm bandwidth và pin**
- Không load duplicate ads
- Sử dụng lại ads đã preload

### 3. **Tăng fill rate**
- Ads được load trước khi user vào màn hình
- Nhiều thời gian hơn để load thành công

### 4. **Giảm timeout errors**
- Preload có nhiều thời gian hơn (ở splash)
- Khi show thì ads đã sẵn sàng

---

## Testing Checklist

- [ ] **Splash Screen**
  - [ ] Verify ads được preload thành công
  - [ ] Check logs xem có bao nhiêu ads loaded
  
- [ ] **Home Fragment**
  - [ ] Ads hiển thị ngay khi vào màn hình
  - [ ] Không có loading spinner
  - [ ] Ads reload sau khi được lấy ra
  
- [ ] **Channels Fragment**
  - [ ] Ads hiển thị ngay khi vào màn hình
  - [ ] Không có loading spinner
  - [ ] Ads reload sau khi được lấy ra
  
- [ ] **Memory Management**
  - [ ] Ads cũ được destroy properly
  - [ ] Không có memory leak
  - [ ] Pool size được quản lý tốt

- [ ] **Edge Cases**
  - [ ] User vào Home quá nhanh (trước khi preload xong)
  - [ ] Network slow/offline
  - [ ] Premium user (không show ads)

---

## Performance Metrics

### Trước khi optimize:
```
Splash -> Home: 
  - Native ad load time: ~3-5 seconds
  - User sees blank space/shimmer
  
Splash -> Channels:
  - Native ad load time: ~3-5 seconds  
  - User sees blank space/shimmer
```

### Sau khi optimize:
```
Splash -> Home:
  - Native ad show time: ~0.1 second (instant)
  - User sees ad immediately
  
Splash -> Channels:
  - Native ad show time: ~0.1 second (instant)
  - User sees ad immediately
```

---

## Lưu ý quan trọng

### 1. **removeAfterGet = true**
Luôn set `removeAfterGet = true` để tránh show duplicate ads:

```kotlin
val nativeAd = getNativeAdOrBackup(
    adKey = adKey,
    removeAfterGet = true  // ← Quan trọng!
)
```

### 2. **Reload sau khi lấy**
Sau khi lấy ad từ pool, nên reload ngay để pool luôn đầy:

```kotlin
if (nativeAd != null) {
    populateNativeAdView(view, nativeAd)
    
    // Reload ngay
    loadNativeAd(adKey = adKey, numberOfAds = 1)
}
```

### 3. **Fallback strategy**
Luôn có fallback cho trường hợp pool rỗng:

```kotlin
if (nativeAd != null) {
    // Show preloaded ad
} else {
    // Fallback: Load new ad
    loadSingleNative(...)
}
```

### 4. **Premium user**
Preload manager đã tự động check premium:

```kotlin
// Trong PreloadNativeManagement.load()
if (premiumManager.isSubscribed()) {
    callback?.invoke(null)
    return
}
```

---

## Related Files

- **Preload Manager**: `monetization/src/main/java/tpt/dev/monetization/ads/preload/PreloadNativeManagement.kt`
- **Extension Functions**: `monetization/src/main/java/tpt/dev/monetization/ads/preload/FragmentExtensions.kt`
- **Example Usage**: `monetization/src/main/java/tpt/dev/monetization/ads/preload/PreloadAdsExample.kt`
- **Splash Preload**: `app/src/main/java/com/silverlabtech/iptv/smartplayer/ui/splash/SplashFragment.kt`
- **Home Fragment**: `app/src/main/java/com/silverlabtech/iptv/smartplayer/ui/home/HomeFragment.kt`
- **Channels Fragment**: `app/src/main/java/com/silverlabtech/iptv/smartplayer/ui/channels/ChannelsFragment.kt`
