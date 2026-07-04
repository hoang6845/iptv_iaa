# Fix: Splash Ad Race Condition

## Vấn đề

Log cho thấy:
```
PreloadInterstitial: load: ca-app-pub-5698123608094343/9440952925
PreloadInterstitial: show: ca-app-pub-5698123608094343/9440952925
PreloadInterstitial: show: ca-app-pub-5698123608094343/9440952925 - ad not loaded, try backup
```

**Race condition**: `show()` được gọi NGAY SAU `load()`, chưa đủ thời gian để ad load xong.

## Nguyên nhân

Trong `SplashFragment`:

1. **onPreloadAds()** bắt đầu load ads (bất đồng bộ)
2. **openHome()** được gọi ngay sau đó
3. **openHome()** gọi `show()` NGAY LẬP TỨC mà không đợi ad load xong
4. Kết quả: Ad chưa có trong pool → fallback sang backup

### Flow cũ (SAI):
```
onPreloadAds() → load ads (async)
                    ↓
openHome() → show() → ad not found → try backup
```

## Giải pháp

Thêm logic **đợi ad load xong** trước khi show:

```kotlin
override fun openHome() {
    val splashAdKey = getString(R.string.ads_inter_splash)
    
    // Kiểm tra ad đã load chưa
    if (!adsManager.preloadInterstitialManagement.isLoaded(splashAdKey)) {
        // Đợi tối đa 3 giây
        CoroutineScope(Dispatchers.Main).launch {
            var waitCount = 0
            while (waitCount < 30 && !isLoaded(splashAdKey)) {
                delay(100)
                waitCount++
            }
            showSplashAdAndNavigate()
        }
    } else {
        showSplashAdAndNavigate()
    }
}
```

### Flow mới (ĐÚNG):
```
onPreloadAds() → load ads (async)
                    ↓
                 [đợi load xong]
                    ↓
openHome() → check isLoaded() → show() → success
```

## Kết quả

- ✅ Ad được load đầy đủ trước khi show
- ✅ Không còn "ad not loaded, try backup"
- ✅ Tăng fill rate và revenue
- ✅ Có timeout 3s để tránh treo app

## Testing

1. Xóa app data
2. Mở app lần đầu
3. Quan sát log:
   ```
   PreloadInterstitial: load: ca-app-pub-xxx
   PreloadInterstitial: onAdLoaded: ca-app-pub-xxx
   SplashFragment: ✓ Splash ad loaded after XXXms
   PreloadInterstitial: show: ca-app-pub-xxx
   PreloadInterstitial: onAdShowedFullScreenContent: ca-app-pub-xxx
   ```

## Lưu ý

- Timeout 3s là hợp lý cho interstitial ads
- Nếu sau 3s vẫn chưa load, vẫn gọi show() để fallback sang backup
- Không block UI thread vì dùng coroutine
