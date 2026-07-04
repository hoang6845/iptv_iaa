# Fix: Ads không load khi vào app lần 2

## Vấn đề

### Hiện tượng
- **Lần 1 vào app:** Ads được preload và hiển thị bình thường
- **Lần 2 vào app (sau khi kill app):** Không có ads hiển thị

### Nguyên nhân

#### 1. Logic sai trong BaseSplashFragment
```kotlin
// CODE CŨ - SAI
override fun initView() {
    if (isFirstSplash()) {  // ← Chỉ check lần đầu
        setFirstSplash(false)
        gatherConsentAndFetch()  // ← Chỉ chạy lần đầu
    } else {
        openHome()  // ← Lần 2 bỏ qua preload, vào home luôn
        return
    }
}
```

**Vấn đề:**
- `isFirstSplash()` chỉ `true` ở lần đầu tiên
- Lần 2 trở đi → bỏ qua `gatherConsentAndFetch()` → bỏ qua `preloadAds()`
- Không preload ads → không có ads để show

#### 2. Ads Pool là in-memory
```kotlin
// Trong PreloadInterstitialManagement
private val adsPool: MutableMap<String, InterstitialAd> = HashMap()
```

**Vấn đề:**
- `adsPool` lưu trong RAM, không persist
- Khi app bị kill → pool bị clear
- Mở lại app → pool rỗng → không có ads

#### 3. Flow hiện tại

**Lần 1 (First Launch):**
```
App Start
  ↓
isFirstSplash() = true
  ↓
gatherConsentAndFetch()
  ↓
fetch() → Remote Config
  ↓
preloadAds() → Load ads vào pool
  ↓
openHome() → Show ads từ pool ✓
  ↓
Navigate to Home
```

**Lần 2 (App killed & reopened):**
```
App Start
  ↓
isFirstSplash() = false
  ↓
openHome() TRỰC TIẾP (bỏ qua preload)
  ↓
adsPool rỗng (vì app bị kill)
  ↓
Không có ads để show ✗
  ↓
Navigate to Home (không ads)
```

## Giải pháp

### 1. Sửa BaseSplashFragment
**LUÔN** gather consent và preload ads, không phân biệt lần đầu hay không:

```kotlin
// CODE MỚI - ĐÚNG
override fun initView() {
    // LUÔN gather consent và fetch config
    gatherConsentAndFetch()
    
    // Chỉ set first splash flag để tracking
    if (isFirstSplash()) {
        setFirstSplash(false)
    }
}
```

### 2. Sửa SplashFragment
Luôn setup loading với progress bar:

```kotlin
// CODE MỚI
override fun initView() {
    super.initView()
    // Luôn setup loading vì luôn cần preload ads
    if (isFirst()) {
        binding.tvLoading.text =
            resources.getStringArray(R.array.text_first_time).random()
    } else {
        binding.tvLoading.text = getString(R.string.loading_text)
    }
    setupLoading()  // ← Luôn setup loading
    checkConsentShow()
}
```

### 3. Flow mới (Đúng)

**Mọi lần vào app:**
```
App Start
  ↓
gatherConsentAndFetch()
  ↓
fetch() → Remote Config
  ↓
preloadAds() → Load ads vào pool
  ↓
openHome() → Show ads từ pool ✓
  ↓
Navigate to Home
```

## Lợi ích

### ✅ Ads luôn được preload
- Mỗi lần vào app đều preload ads mới
- Không phụ thuộc vào in-memory pool
- Đảm bảo luôn có ads để show

### ✅ User experience tốt hơn
- Lần 1: Có ads ✓
- Lần 2: Có ads ✓
- Lần N: Có ads ✓

### ✅ Revenue ổn định
- Không bỏ lỡ impression
- Mỗi lần mở app đều có cơ hội show ads

## Lưu ý

### 1. Loading time
- Lần 2 vào app sẽ có loading (như lần 1)
- Nhưng đảm bảo có ads → tăng revenue
- Trade-off hợp lý: UX vs Revenue

### 2. Consent
- `gatherConsent()` chỉ show dialog nếu chưa có consent
- Nếu đã có consent → skip dialog, chỉ init MobileAds
- Không ảnh hưởng UX

### 3. Remote Config
- Fetch config mỗi lần vào app
- Đảm bảo luôn có config mới nhất
- Cache trong Firebase SDK → nhanh

### 4. Ads Pool lifecycle
```
App Start → Pool rỗng
  ↓
Preload → Pool có ads
  ↓
Show → Remove khỏi pool
  ↓
Reload → Add lại vào pool
  ↓
App Background → Pool vẫn còn (nếu không kill)
  ↓
App Killed → Pool bị clear
  ↓
App Restart → Preload lại từ đầu
```

## Testing

### Test case 1: First launch
1. Cài app mới
2. Mở app
3. **Expected:** Loading → Show ads → Navigate

### Test case 2: Second launch (app in background)
1. Mở app lần 1
2. Press Home (app vào background)
3. Mở app lại
4. **Expected:** Loading → Show ads → Navigate

### Test case 3: Second launch (app killed)
1. Mở app lần 1
2. Kill app (swipe away)
3. Mở app lại
4. **Expected:** Loading → Show ads → Navigate ✓ (FIX)

### Test case 4: No internet
1. Tắt internet
2. Mở app
3. **Expected:** Loading → Skip ads → Navigate

## Kết luận

Fix này đảm bảo:
- ✅ Ads được preload **MỌI LẦN** vào app
- ✅ Không bỏ lỡ impression
- ✅ Revenue ổn định
- ✅ User experience nhất quán

**Trade-off:** Loading time tăng nhẹ ở lần 2+, nhưng đảm bảo có ads.
