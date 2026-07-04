# Rewarded Ads Implementation Checklist ✅

## ✅ Đã hoàn thành

### 1. Core Implementation
- [x] **PreloadRewardedManagement.kt** - Preload management class
  - Pool management
  - Waterfall loading
  - Backup ad support
  - Loading dialog
  - Auto reload
  
- [x] **AdsManager.kt** - Integration
  - Added `preloadRewardedManagement` lazy property
  - Added import statement
  - RewardedAdUtils already exists

### 2. Helper Methods
- [x] **BaseActivity.kt**
  - Added `showRewardedAd()` method
  - Callbacks: onAdShowed, onAdDismissed, onLoadFailed, onRewarded

- [x] **BaseFragment.kt**  
  - Added `showRewardedAd()` method
  - Same callbacks as BaseActivity

### 3. Documentation
- [x] **REWARDED_ADS_GUIDE.md** - Comprehensive guide
  - Direct load & show
  - Preload management
  - Use cases & best practices
  - Advanced strategies
  - Tips & pitfalls

- [x] **REWARDED_ADS_EXAMPLE.kt** - 7 practical examples
  - Simple usage
  - Preloading
  - Game continue
  - Premium unlock
  - Multiple rewards
  - Ad availability check

- [x] **REWARDED_ADS_INTEGRATION_SUMMARY.md** - Overview
- [x] **REWARDED_ADS_CHECKLIST.md** - This file

### 4. Build & Verification
- [x] Code compiles successfully
- [x] No syntax errors
- [x] Proper imports added

---

## 📋 Những gì BẠN cần làm tiếp

### Step 1: Config Ad IDs ⚙️

Thêm vào `app/src/main/res/values/strings.xml`:

```xml
<!-- Rewarded Ad IDs -->
<string name="admob_rewarded_coins">YOUR_AD_UNIT_ID_HERE</string>
<string name="admob_rewarded_premium">YOUR_AD_UNIT_ID_HERE</string>
<string name="admob_rewarded_unlock">YOUR_AD_UNIT_ID_HERE</string>
<string name="admob_rewarded_backup">YOUR_AD_UNIT_ID_HERE</string>

<!-- For testing -->
<string name="admob_rewarded_test">ca-app-pub-3940256099942544/5224354917</string>
```

### Step 2: Update Constants (nếu cần) 🔧

Nếu bạn có file constants cho ads, thêm:

```kotlin
// AbstractAdsConstants.kt hoặc file tương tự
abstract class AdsConstants {
    abstract val REWARD_ID: String
    // ... các ad IDs khác
}
```

### Step 3: Preload trong MainActivity (Optional nhưng recommended) 🚀

```kotlin
class MainActivity : BaseMainActivity<ActivityMainBinding, MainViewModel>() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        preloadRewardedAds()
    }
    
    private fun preloadRewardedAds() {
        val adKeys = listOf(
            getString(R.string.admob_rewarded_coins),
            getString(R.string.admob_rewarded_premium)
        )
        
        AppMonetization.ads.preloadRewardedManagement.loadWithWaterfall(
            activity = this,
            adKeys = adKeys,
            delayMs = 300L
        )
        
        // Optional: backup ad
        AppMonetization.ads.preloadRewardedManagement.enableBackup(
            activity = this,
            backupKey = getString(R.string.admob_rewarded_backup)
        )
    }
}
```

### Step 4: Sử dụng trong Screen/Fragment 🎯

**Option A: Direct Load (Simple)**
```kotlin
showRewardedAd(
    onRewarded = { rewardItem ->
        // Trao thưởng cho user
        viewModel.addCoins(rewardItem.amount)
    },
    onLoadFailed = {
        Toast.makeText(context, "Ad not available", Toast.LENGTH_SHORT).show()
    }
)
```

**Option B: Preload (Better Performance)**
```kotlin
// 1. Check if loaded
if (AppMonetization.ads.preloadRewardedManagement.isLoaded(adKey)) {
    // 2. Show
    AppMonetization.ads.preloadRewardedManagement.show(
        activity = requireActivity(),
        adKey = adKey,
        reload = true,
        onRewarded = { rewardItem ->
            viewModel.addCoins(rewardItem.amount)
        }
    )
} else {
    Toast.makeText(context, "Ad is loading...", Toast.LENGTH_SHORT).show()
}
```

### Step 5: Testing 🧪

```kotlin
// Sử dụng test ad ID
val testAdId = "ca-app-pub-3940256099942544/5224354917"

// Test trong dev
showRewardedAd(...)

// Check logs
adb logcat | grep "PreloadRewarded"
adb logcat | grep "RewardedAdUtils"
```

---

## 🎯 Quick Start Example

Cách nhanh nhất để test:

```kotlin
// Trong bất kỳ Fragment nào
binding.btnTestReward.setOnClickListener {
    showRewardedAd(
        onRewarded = { rewardItem ->
            Toast.makeText(
                requireContext(),
                "You earned ${rewardItem.amount} ${rewardItem.type}",
                Toast.LENGTH_LONG
            ).show()
        },
        onLoadFailed = {
            Toast.makeText(
                requireContext(),
                "Ad failed to load",
                Toast.LENGTH_SHORT
            ).show()
        }
    )
}
```

---

## 📚 Documentation Reference

| File | Purpose |
|------|---------|
| `REWARDED_ADS_GUIDE.md` | Hướng dẫn chi tiết, best practices |
| `REWARDED_ADS_EXAMPLE.kt` | 7 ví dụ thực tế copy-paste |
| `REWARDED_ADS_INTEGRATION_SUMMARY.md` | Tổng quan implementation |
| `REWARDED_ADS_CHECKLIST.md` | Checklist này |

---

## ⚠️ Important Notes

### Callback Order:
```
1. onAdShowed() - Ad đang hiển thị
2. onRewarded() - User xem xong, nhận reward (QUAN TRỌNG!)
3. onAdDismissed() - Ad đóng
```

### Common Mistakes:
```kotlin
// ❌ WRONG: Trao reward sớm
onAdShowed = {
    giveReward() // User có thể skip!
}

// ✅ CORRECT: Trao reward đúng chỗ
onRewarded = { rewardItem ->
    giveReward(rewardItem.amount)
}
```

### Best Practices:
1. ✅ Always handle `onLoadFailed`
2. ✅ Validate reward in `onRewarded`
3. ✅ Preload ads for better UX
4. ✅ Use backup ads
5. ✅ Check `isLoaded()` before showing

---

## 🎉 Ready to Use!

Mọi thứ đã sẵn sàng:
- ✅ Code implemented & compiled
- ✅ Helper methods added
- ✅ Documentation complete
- ✅ Examples provided

Bạn chỉ cần:
1. Config ad IDs
2. Gọi `showRewardedAd()` ở bất kỳ đâu
3. Test và enjoy! 🚀

**Have fun coding!** 💪
