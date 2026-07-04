# Rewarded Ads Architecture

## 📐 System Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                         Application Layer                        │
├─────────────────────────────────────────────────────────────────┤
│  BaseActivity / BaseFragment                                     │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │ showRewardedAd()                                         │   │
│  │  - onAdShowed                                            │   │
│  │  - onAdDismissed                                         │   │
│  │  - onLoadFailed                                          │   │
│  │  - onRewarded ⭐                                         │   │
│  └─────────────────────────────────────────────────────────┘   │
└────────────────────────────┬────────────────────────────────────┘
                             │ calls
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│                         AdsManager                               │
├─────────────────────────────────────────────────────────────────┤
│  Singleton Manager                                               │
│  ┌──────────────────────┐    ┌──────────────────────────────┐  │
│  │ rewardedAdUtils      │    │ preloadRewardedManagement    │  │
│  │ (Direct Load)        │    │ (Pool Management)            │  │
│  └──────────────────────┘    └──────────────────────────────┘  │
└────────────┬────────────────────────────┬───────────────────────┘
             │                            │
             ▼                            ▼
┌────────────────────────┐    ┌──────────────────────────────────┐
│   RewardedAdUtils      │    │ PreloadRewardedManagement        │
│   (On-demand Loading)  │    │ (Preload & Pool)                 │
├────────────────────────┤    ├──────────────────────────────────┤
│ • showRewardedAd()     │    │ • load(adKey)                    │
│ • loadRewardedAd()     │    │ • loadWithWaterfall(adKeys)      │
│ • Loading dialog       │    │ • show(adKey)                    │
│ • Single ad            │    │ • isLoaded(adKey)                │
│                        │    │ • isLoading(adKey)               │
│                        │    │ • enableBackup(backupKey)        │
│                        │    │ • Pool: Map<String, RewardedAd>  │
│                        │    │ • Waterfall loading              │
│                        │    │ • Auto reload                    │
│                        │    │ • Backup fallback                │
└────────────────────────┘    └──────────────────────────────────┘
             │                            │
             └──────────┬─────────────────┘
                        ▼
        ┌───────────────────────────────┐
        │   Google Mobile Ads SDK       │
        │   (RewardedAd API)            │
        └───────────────────────────────┘
```

---

## 🔄 Flow Diagrams

### Flow 1: Direct Load & Show (RewardedAdUtils)

```
User Action
    │
    ├─► showRewardedAd()
    │       │
    │       ├─► Check premium / consent
    │       │
    │       ├─► Show loading dialog
    │       │
    │       ├─► Load ad from Google
    │       │       │
    │       │       ├─► onAdLoaded
    │       │       │       │
    │       │       │       ├─► Dismiss loading
    │       │       │       │
    │       │       │       └─► Show ad
    │       │       │               │
    │       │       │               ├─► onAdShowed ✅
    │       │       │               │
    │       │       │               ├─► User watches
    │       │       │               │
    │       │       │               ├─► onRewarded ⭐ (Give reward!)
    │       │       │               │
    │       │       │               └─► onAdDismissed ✅
    │       │       │
    │       │       └─► onAdFailedToLoad
    │       │               │
    │       │               └─► onLoadFailed ❌
    │       │
    │       └─► Return to app
    │
    └─► Done
```

### Flow 2: Preload & Show (PreloadRewardedManagement)

```
App Start
    │
    ├─► MainActivity.onCreate()
    │       │
    │       └─► preloadRewardedAds()
    │               │
    │               ├─► loadWithWaterfall([key1, key2, key3])
    │               │       │
    │               │       ├─► load(key1) ──► Pool[key1] = RewardedAd
    │               │       │   (delay 300ms)
    │               │       │
    │               │       ├─► load(key2) ──► Pool[key2] = RewardedAd
    │               │       │   (delay 600ms)
    │               │       │
    │               │       └─► load(key3) ──► Pool[key3] = RewardedAd
    │               │           (delay 900ms)
    │               │
    │               └─► enableBackup(backupKey)
    │                       │
    │                       └─► Pool[backup] = RewardedAd
    │
    ├─► User navigates to Shop
    │       │
    │       └─► isLoaded(key) ? enable button : disable button
    │
    ├─► User clicks "Watch Ad"
    │       │
    │       ├─► Check: isLoaded(key)?
    │       │       │
    │       │       ├─► Yes ✅
    │       │       │       │
    │       │       │       ├─► Get ad from Pool[key]
    │       │       │       │
    │       │       │       ├─► Show loading dialog
    │       │       │       │
    │       │       │       ├─► Show ad immediately (already loaded!)
    │       │       │       │       │
    │       │       │       │       ├─► onAdShowed ✅
    │       │       │       │       │
    │       │       │       │       ├─► onRewarded ⭐
    │       │       │       │       │
    │       │       │       │       └─► onAdDismissed ✅
    │       │       │       │
    │       │       │       ├─► Remove from Pool[key]
    │       │       │       │
    │       │       │       └─► Auto reload if needed
    │       │       │
    │       │       └─► No ❌
    │       │               │
    │       │               └─► Try backup
    │       │                       │
    │       │                       ├─► Pool[backup] exists?
    │       │                       │       │
    │       │                       │       ├─► Yes: Show backup
    │       │                       │       │
    │       │                       │       └─► No: onLoadFailed ❌
    │       │
    │       └─► Done
    │
    └─► App continues
```

---

## 🧩 Component Relationships

```
┌──────────────────────────────────────────────────────────────┐
│                     AdsManager                               │
│                     (Singleton)                              │
│                                                              │
│  ┌────────────────────────────────────────────────────┐    │
│  │              Lazy Initialization                   │    │
│  ├────────────────────────────────────────────────────┤    │
│  │                                                     │    │
│  │  rewardedAdUtils                                   │    │
│  │    │                                               │    │
│  │    ├─► RewardedAdUtils(                           │    │
│  │    │     rewardedAdId,                            │    │
│  │    │     adsManager,                              │    │
│  │    │     premiumManager,                          │    │
│  │    │     consentManager                           │    │
│  │    │   )                                           │    │
│  │    │                                               │    │
│  │  preloadRewardedManagement                        │    │
│  │    │                                               │    │
│  │    └─► PreloadRewardedManagement(                 │    │
│  │          application,                             │    │
│  │          adsManager,                              │    │
│  │          premiumManager,                          │    │
│  │          consentManager                           │    │
│  │        )                                           │    │
│  │                                                     │    │
│  └────────────────────────────────────────────────────┘    │
│                                                              │
│  Other managers:                                            │
│  • interstitialAdUtils                                      │
│  • singleNativeAdUtils                                      │
│  • bannerAdUtils                                            │
│  • appOpenAdUtils                                           │
│  • preloadInterstitialManagement                            │
│  • preloadNativeManagement                                  │
│  • preloadBannerManagement                                  │
│                                                              │
└──────────────────────────────────────────────────────────────┘
```

---

## 📊 Data Flow: Pool Management

```
┌─────────────────────────────────────────────────────────┐
│         PreloadRewardedManagement                       │
├─────────────────────────────────────────────────────────┤
│                                                         │
│  Pool: MutableMap<String, RewardedAd>                  │
│  ┌───────────────────────────────────────────────┐    │
│  │                                                │    │
│  │  "coins_ad"    ──► RewardedAd instance 1      │    │
│  │  "premium_ad"  ──► RewardedAd instance 2      │    │
│  │  "unlock_ad"   ──► RewardedAd instance 3      │    │
│  │  "backup_ad"   ──► RewardedAd instance 4      │    │
│  │                                                │    │
│  └───────────────────────────────────────────────┘    │
│                                                         │
│  Loading Pool: MutableSet<String>                      │
│  ┌───────────────────────────────────────────────┐    │
│  │                                                │    │
│  │  "feature_ad"  (currently loading...)         │    │
│  │  "shop_ad"     (currently loading...)         │    │
│  │                                                │    │
│  └───────────────────────────────────────────────┘    │
│                                                         │
│  Operations:                                            │
│  • load(key)       ──► Add to Loading → Load → Add to Pool
│  • show(key)       ──► Get from Pool → Show → Remove   │
│  • isLoaded(key)   ──► Check if exists in Pool        │
│  • isLoading(key)  ──► Check if exists in Loading     │
│  • remove(key)     ──► Remove from Pool               │
│  • clearAll()      ──► Clear both Pool & Loading      │
│                                                         │
└─────────────────────────────────────────────────────────┘
```

---

## 🎯 Usage Comparison

### Scenario 1: Infrequent Ad Usage

```
┌──────────────────────────┐
│   Settings Screen        │
│                          │
│  [Unlock Premium]        │
│       │                  │
│       │ Click            │
│       ▼                  │
│  showRewardedAd()        │ ◄─── USE: RewardedAdUtils
│  (Direct Load)           │      (Simple, on-demand)
│                          │
└──────────────────────────┘
```

### Scenario 2: Frequent Ad Usage

```
┌──────────────────────────────────────────────────────┐
│   Shop Screen (Frequent Usage)                       │
│                                                       │
│   [Watch Ad for 10 Coins] ◄─┐                       │
│   [Watch Ad for 50 Coins] ◄─┼─ USE: PreloadRewardedManagement
│   [Watch Ad for Premium]  ◄─┘     (Pool, instant show)
│                                                       │
│   Preloaded on app start:                           │
│   • Pool has ads ready                              │
│   • Show immediately when clicked                    │
│   • Auto reload after show                          │
│                                                       │
└──────────────────────────────────────────────────────┘
```

---

## 🔁 Lifecycle Integration

```
Application Lifecycle:
    │
    ├─► App Start
    │     │
    │     └─► AdsManager.initialize()
    │           │
    │           └─► rewardedAdUtils initialized (lazy)
    │           └─► preloadRewardedManagement initialized (lazy)
    │
    ├─► MainActivity.onCreate()
    │     │
    │     └─► preloadRewardedAds() (optional)
    │           │
    │           └─► preloadRewardedManagement.loadWithWaterfall()
    │
    ├─► User navigates
    │     │
    │     └─► Fragment.onResume()
    │           │
    │           └─► Check ad availability
    │               └─► Update UI (enable/disable buttons)
    │
    ├─► User clicks "Watch Ad"
    │     │
    │     └─► showRewardedAd() or preloadRewardedManagement.show()
    │           │
    │           └─► Ad lifecycle
    │                 │
    │                 ├─► onAdShowed
    │                 ├─► onRewarded ⭐
    │                 └─► onAdDismissed
    │
    └─► App continues
```

---

## 🎨 Visual Comparison: Direct vs Preload

### Direct Load (RewardedAdUtils)
```
Timeline:
User clicks button
    │
    ├─► [Show loading dialog]
    │   ▼
    │   [Load from Google] ⏱️ 2-5 seconds wait
    │   ▼
    │   [Ad ready]
    │   ▼
    │   [Show ad]
    │   ▼
    │   User watches & earns reward
    └─► Done

Pros: Simple, less memory
Cons: User waits for loading
```

### Preload (PreloadRewardedManagement)
```
Timeline:
App starts
    │
    ├─► [Preload ads in background]
    │   ▼
    │   [Pool ready with ads]
    │
Later...
    │
User clicks button
    │
    ├─► [Show ad IMMEDIATELY] ⚡ No wait!
    │   ▼
    │   User watches & earns reward
    │   ▼
    │   [Auto reload in background]
    └─► Done

Pros: Instant show, better UX
Cons: Uses more memory, preload time
```

---

## 📐 Class Diagram

```
┌──────────────────────────────┐
│      BaseAdUtils             │
│      (Abstract)              │
├──────────────────────────────┤
│ + premiumManager             │
│ + appAllowShowAd             │
│ + defaultAdRequest           │
└──────────┬───────────────────┘
           │ extends
           │
    ┌──────┴──────┐
    │             │
    ▼             ▼
┌────────────────────────┐  ┌──────────────────────────────┐
│  RewardedAdUtils       │  │ PreloadRewardedManagement    │
├────────────────────────┤  ├──────────────────────────────┤
│ - rewardedAdId         │  │ - adsPool: Map<String, Ad>   │
│ - adsManager           │  │ - loadingPools: Set<String>  │
│ - dialog               │  │ - backupAdKey: String        │
├────────────────────────┤  │ - isShowingAds: Boolean      │
│ + showRewardedAd()     │  │ - dialog                     │
│ - loadRewardedAd()     │  ├──────────────────────────────┤
└────────────────────────┘  │ + load(adKey)                │
                            │ + loadWithWaterfall(keys)    │
                            │ + show(adKey)                │
                            │ + isLoaded(adKey)            │
                            │ + isLoading(adKey)           │
                            │ + enableBackup(backupKey)    │
                            │ + clearAll()                 │
                            │ + remove(adKey)              │
                            └──────────────────────────────┘
```

---

## 🎯 Summary

### Architecture Principles:
1. **Separation of Concerns**: RewardedAdUtils vs PreloadRewardedManagement
2. **Lazy Initialization**: Components init only when needed
3. **Pool Management**: Efficient ad caching and reuse
4. **Callback Pattern**: Clean async handling
5. **Backup Fallback**: Reliability through backup ads

### Key Benefits:
- ✅ Clean architecture
- ✅ Easy to use API
- ✅ Performance optimized
- ✅ Memory efficient (when needed)
- ✅ Flexible (2 usage modes)
- ✅ Battle-tested pattern (same as interstitial)

**Ready to build amazing rewarded ad experiences!** 🚀
