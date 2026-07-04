# Preload Native Ads - Test Checklist

## Quick Test Guide

### ✅ Test 1: Normal Flow (Happy Path)
**Mục tiêu**: Verify preloaded ads hoạt động đúng

**Steps**:
1. Mở app từ đầu (fresh start)
2. Đợi ở splash screen ~2-3 giây
3. Navigate to Home screen
4. Navigate to Channels screen

**Expected Results**:
- ✅ Home: Native ad hiển thị **ngay lập tức** (không thấy shimmer)
- ✅ Channels: Native ad hiển thị **ngay lập tức** (không thấy shimmer)
- ✅ Không có loading spinner hoặc delay

**Logs to check**:
```
D/PreloadNativeManagement: Native ad loaded successfully: ads_native_home
D/BaseFragment: Showed preloaded native ad: ads_native_home
D/BaseFragment: Showed preloaded native ad: ads_collapse_channel
```

---

### ✅ Test 2: Fast Navigation
**Mục tiêu**: Test fallback khi user navigate quá nhanh

**Steps**:
1. Mở app
2. **Ngay lập tức** tap vào màn hình để skip splash (trong 0.5 giây)
3. Navigate to Home

**Expected Results**:
- ⚠️ Pool có thể chưa có ads
- ✅ Fallback to `loadSingleNative()` gracefully
- ✅ Thấy loading shimmer (bình thường)
- ✅ Ads load sau 2-3 giây

**Logs to check**:
```
D/BaseFragment: Preloaded ad not available, loading new ad: ads_native_home
```

---

### ✅ Test 3: Second Visit
**Mục tiêu**: Verify reload mechanism

**Steps**:
1. Navigate to Home (ads shown)
2. Navigate to Channels
3. Go back to Home again
4. Go back to Channels again

**Expected Results**:
- ✅ Lần 2 vào Home: Ads vẫn hiển thị ngay (đã reload ở lần 1)
- ✅ Lần 2 vào Channels: Ads vẫn hiển thị ngay (đã reload ở lần 1)
- ✅ Không thấy loading

**Logs to check**:
```
D/PreloadNativeManagement: Native ad loaded successfully: ads_native_home (reload)
```

---

### ✅ Test 4: Premium User
**Mục tiêu**: Verify premium không show ads

**Steps**:
1. Login với premium account
2. Navigate to Home
3. Navigate to Channels

**Expected Results**:
- ✅ Không có native ads hiển thị
- ✅ Không có loading
- ✅ ViewNativeAd bị ẩn hoàn toàn

---

### ✅ Test 5: Network Issues
**Mục tiêu**: Test behavior khi không có internet

**Steps**:
1. Tắt Wi-Fi và Mobile Data
2. Mở app
3. Navigate to Home
4. Navigate to Channels

**Expected Results**:
- ⚠️ Preload ở splash sẽ fail
- ✅ Fallback gracefully (không crash)
- ⚠️ Native ads không hiển thị (expected)
- ✅ App vẫn hoạt động bình thường

---

## Performance Comparison

### Metric to measure: Time to show ads

**Trước khi fix**:
```
Splash → Home: ~3-5 giây (loading shimmer visible)
Splash → Channels: ~3-5 giây (loading shimmer visible)
```

**Sau khi fix**:
```
Splash → Home: ~0.1 giây (instant)
Splash → Channels: ~0.1 giây (instant)
```

**Cách đo**:
1. Record video màn hình
2. Đếm frames từ khi vào screen đến khi ads hiển thị
3. So sánh trước/sau

---

## Logcat Commands

### Filter relevant logs:
```bash
# Filter preload ads logs
adb logcat | grep -E "PreloadNative|BaseFragment"

# Filter ads logs only
adb logcat | grep -E "ads_native_home|ads_collapse_channel"

# Filter all monetization logs
adb logcat | grep "tpt.dev.monetization"
```

### Expected successful flow:
```
D/SplashFragment: Starting preload native ads with waterfall
D/PreloadNativeManagement: Start loading: ads_native_home
D/PreloadNativeManagement: Native ad loaded successfully: ads_native_home
D/PreloadNativeManagement: Start loading: ads_collapse_channel
D/PreloadNativeManagement: Native ad loaded successfully: ads_collapse_channel
...
D/BaseFragment: Showed preloaded native ad: ads_native_home
D/PreloadNativeManagement: Start loading: ads_native_home (reload)
...
D/BaseFragment: Showed preloaded native ad: ads_collapse_channel
D/PreloadNativeManagement: Start loading: ads_collapse_channel (reload)
```

---

## Common Issues & Solutions

### Issue 1: "Preloaded ad not available"
**Symptom**: Log shows fallback to loadSingleNative

**Causes**:
- User navigate quá nhanh (before preload done)
- Network slow → preload chưa xong
- Premium user → ads không load

**Solution**: 
- Expected behavior, fallback is working
- Consider tăng thời gian splash nếu cần

---

### Issue 2: Ads không hiển thị
**Symptom**: Blank space, không có ads

**Check**:
1. Premium user? → Expected
2. Network connected? → Check internet
3. AdMob configured? → Check google-services.json
4. Consent given? → Check UMP consent

**Debug**:
```bash
adb logcat | grep -E "AdMob|Failed to load"
```

---

### Issue 3: Memory leak warning
**Symptom**: LeakCanary warning về NativeAd

**Check**:
- Old ads có được destroy không?
- Fragment lifecycle handled đúng không?

**Solution**:
```kotlin
nativeAd?.destroy()  // ← Đảm bảo có dòng này
nativeAd = preloadedAd
```

---

## Sign-off Checklist

Before merging to production:

- [ ] ✅ Test 1: Happy path passed
- [ ] ✅ Test 2: Fast navigation handled
- [ ] ✅ Test 3: Second visit works
- [ ] ✅ Test 4: Premium user no ads
- [ ] ✅ Test 5: Network issues handled
- [ ] ✅ No compile errors
- [ ] ✅ No runtime crashes
- [ ] ✅ Logs look clean
- [ ] ✅ Performance improved (subjective feel)
- [ ] ✅ Memory usage stable (no leaks)

---

**Tester**: _____________  
**Date**: _____________  
**Status**: ⬜ Pass / ⬜ Fail  
**Notes**: _____________
