# Hướng dẫn điều chỉnh Ads qua Remote Config

## Tổng quan
Bạn có thể điều chỉnh hành vi của ads trong app thông qua Firebase Remote Config mà không cần phải release version mới.

## Các biến Remote Config

### 1. Timing Configs (Đơn vị: milliseconds)

#### `time_delay_inter_splash_vs_open`
- **Mặc định:** 20000 (20 giây)
- **Mô tả:** Khoảng thời gian tối thiểu giữa lần show Interstitial Ad và App Open Ad
- **Ví dụ:** 
  - Nếu set = 30000 → Sau khi show Inter Ad, phải đợi 30 giây mới show Open Ad
  - Nếu set = 10000 → Chỉ cần đợi 10 giây

#### `time_delay_show_inter`
- **Mặc định:** 20000 (20 giây)
- **Mô tả:** Khoảng thời gian tối thiểu giữa các lần show Interstitial Ad
- **Ví dụ:**
  - Nếu set = 60000 → Chỉ show Inter Ad tối đa 1 lần/phút
  - Nếu set = 15000 → Có thể show Inter Ad mỗi 15 giây

### 2. Enable/Disable Flags (Boolean)

#### `is_show_ads_app`
- **Mặc định:** true
- **Mô tả:** Bật/tắt TẤT CẢ ads trong app
- **Tác dụng:**
  - `true` → Tất cả ads hoạt động bình thường
  - `false` → TẮT TOÀN BỘ ads (App Open, Interstitial, Native, Banner)
- **Use case:** Tắt ads khi có sự cố, hoặc trong các sự kiện đặc biệt

#### `is_show_ad_open`
- **Mặc định:** true
- **Mô tả:** Bật/tắt App Open Ad (ad hiện khi quay lại app từ background)
- **Tác dụng:**
  - `true` → Show App Open Ad khi user comeback
  - `false` → Không show App Open Ad
- **Use case:** Tắt App Open Ad nếu user phàn nàn quá nhiều

#### `is_show_inter_splash`
- **Mặc định:** true
- **Mô tả:** Bật/tắt Interstitial Ad sau splash screen
- **Tác dụng:**
  - `true` → Show Inter Ad sau splash (trước khi vào home/intro)
  - `false` → Bỏ qua Inter Ad, vào thẳng home/intro
- **Use case:** Tắt splash ad để cải thiện trải nghiệm lần đầu

## Cách hoạt động

### Flow logic:
```
1. App khởi động
2. Splash screen load Remote Config
3. loadRemoteConfigVariables() được gọi
4. Áp dụng các config vào AdsManager:
   - setGlobalAdsEnabled(is_show_ads_app)
   - setAppOpenAdEnabled(is_show_ad_open)
   - setInterSplashEnabled(is_show_inter_splash)
   - updateTimeIntervalShowInterVsOpen(time_delay_inter_splash_vs_open)
   - updateTimeIntervalShowInterstitialAd(time_delay_show_inter)
```

### Kiểm tra trước khi show ads:

**App Open Ad (onStart lifecycle):**
```kotlin
if (!isGlobalAdsEnabled) return  // Kiểm tra global flag
if (!isAppOpenAdEnabled) return  // Kiểm tra App Open flag
// ... show App Open Ad
```

**Interstitial Splash:**
```kotlin
if (!isGlobalAdsEnabled) {
    navigateToNextScreen()  // Skip ad
    return
}
if (!isInterSplashEnabled) {
    navigateToNextScreen()  // Skip ad
    return
}
// ... show Inter Ad
```

## Ví dụ sử dụng

### Scenario 1: Tắt tất cả ads trong 1 ngày
```json
{
  "is_show_ads_app": false
}
```

### Scenario 2: Chỉ tắt App Open Ad
```json
{
  "is_show_ads_app": true,
  "is_show_ad_open": false,
  "is_show_inter_splash": true
}
```

### Scenario 3: Giảm tần suất show ads
```json
{
  "time_delay_inter_splash_vs_open": 60000,
  "time_delay_show_inter": 45000
}
```
→ Inter Ad chỉ show tối đa mỗi 45 giây, và phải cách Open Ad ít nhất 60 giây

### Scenario 4: Tối ưu trải nghiệm lần đầu
```json
{
  "is_show_inter_splash": false
}
```
→ User lần đầu vào app sẽ không thấy Inter Ad sau splash

## Debug & Monitoring

### Log để theo dõi:
```
Tag: RemoteConfig
===== ADS CONFIG =====
Time Inter ↔ Open: 20000ms
Time Inter Interval: 20000ms
Show Open Ad: true
Show All Ads: true
Show Inter Splash: true
======================
```

### Log khi skip ads:
```
Tag: AppLifecycle
- "Skip: Global ads disabled"
- "Skip: App Open Ad disabled"

Tag: SplashFragment
- "Global ads disabled, skip splash ad"
- "Inter splash disabled, skip splash ad"
```

## Best Practices

1. **Test trước khi apply production:**
   - Dùng Firebase Remote Config conditions để test với một nhóm user nhỏ
   - Monitor crash rate và user feedback

2. **Timing hợp lý:**
   - Không set quá ngắn (< 10 giây) → User sẽ bị spam ads
   - Không set quá dài (> 2 phút) → Mất revenue

3. **Kết hợp flags:**
   - Có thể tắt từng loại ad riêng lẻ
   - Hoặc tắt toàn bộ bằng `is_show_ads_app`

4. **Emergency shutdown:**
   - Nếu có sự cố nghiêm trọng với ads, set `is_show_ads_app = false` ngay lập tức
   - Config sẽ được apply trong vòng vài phút

## Code Reference

- **Remote Config keys:** `codeBase/src/main/java/hoang/dqm/codebase/firebase/AppRemoteConfig.kt`
- **Load & apply config:** `app/src/main/java/com/silverlabtech/iptv/smartplayer/ui/splash/SplashFragment.kt` → `loadRemoteConfigVariables()`
- **Ads control logic:** `monetization/src/main/java/tpt/dev/monetization/ads/AdsManager.kt`
