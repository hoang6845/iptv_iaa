# Fix: "Có animation back nhưng vẫn ở lại màn"

## 🐛 Vấn đề

Khi user nhấn back từ màn xem phim (WatchChannelFragment), đôi khi có animation back nhưng fragment vẫn ở lại màn hình thay vì quay về màn trước.

## 🔍 Nguyên nhân

### Flow lỗi:
1. User nhấn Back → `handleBackPress()` được gọi
2. `requestedOrientation = UNSPECIFIED` → **Activity bắt đầu recreate** (đặc biệt trên Samsung, Xiaomi)
3. `popBackStack()` được gọi → Fragment bị remove khỏi back stack ✅
4. **Activity recreation hoàn tất** → Android restore back stack từ saved state
5. **Fragment xuất hiện lại** 😵 vì back stack được restore về trạng thái trước khi pop

### Root cause:
**Timing issue**: Reset orientation **TRƯỚC** khi fragment được remove khỏi back stack → Activity recreation trigger restore back stack → Fragment bị restore lại.

## ✅ Giải pháp

### Chuyển orientation reset từ `handleBackPress()` sang `onDestroyView()`

#### Tại sao `onDestroyView()` an toàn hơn?

| Vị trí | Thời điểm | Nguy cơ Recreation | An toàn? |
|--------|-----------|-------------------|----------|
| **handleBackPress()** | Trước khi pop | ⚠️ CAO - Fragment vừa pop xong bị restore | ❌ KHÔNG |
| **onDestroy()** | Sau khi remove | ⚠️ TRUNG BÌNH - Có thể trigger recreation thêm | ⚠️ RỦI RO |
| **onDestroyView()** | Sau khi view destroyed | ✅ THẤP - Fragment đã gone, view đã null | ✅ AN TOÀN |

### Lý do `onDestroyView()` là timing hoàn hảo:

1. **Fragment lifecycle**: Được gọi SAU khi fragment đã bị remove khỏi back stack
2. **View đã null**: View của fragment đã bị destroy, không còn attach vào Activity
3. **Không ảnh hưởng back stack**: Ngay cả khi Activity recreate, fragment này không còn trong stack để restore
4. **Timing**: Đủ muộn để tránh race condition, đủ sớm để cleanup trước khi Activity destroy

## 📝 Code Changes

### 1. handleBackPress() - BỎ orientation reset

```kotlin
private fun handleBackPress() {
    if (isBackInProgress) return
    isBackInProgress = true
    
    try {
        // ✅ KHÔNG reset orientation ở đây - để onDestroyView() xử lý
        // Tránh Activity recreation trigger restore back stack
        showSystemBars()
        isFullscreen = false

        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    playlistId?.let { id ->
                        channelId?.let { uniqueId ->
                            viewModel.saveFavourite(id, uniqueId)
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("WatchChannel", "Error saving favourite: ${e.message}")
            } finally {
                withContext(Dispatchers.Main) {
                    if (isAdded && !isDetached) {
                        popBackStack()
                    }
                    isBackInProgress = false
                }
            }
        }
    } catch (e: Exception) {
        // Error handling...
    }
}
```

### 2. onDestroyView() - THÊM orientation reset

```kotlin
override fun onDestroyView() {
    super.onDestroyView()
    try {
        // ✅ AN TOÀN: Reset orientation ở đây thay vì handleBackPress()
        // Fragment đã bị remove khỏi back stack, ngay cả khi Activity recreate
        // thì fragment này không còn trong stack để restore nữa
        if (activity != null) {
            requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
        uiHandler.removeCallbacks(hideControlsRunnable)
        sleepTimerUpdateHandler.removeCallbacksAndMessages(null)
    } catch (e: Exception) {
        android.util.Log.e("WatchChannel", "Error in onDestroyView: ${e.message}")
    }
}
```

### 3. onDestroy() - BỎ orientation reset

```kotlin
override fun onDestroy() {
    super.onDestroy()
    try {
        // ✅ BỎ requestedOrientation ra khỏi đây
        // onDestroyView() đã xử lý orientation rồi
        // Chỉ cleanup resources ở đây thôi
        sleepTimer?.cancel()
        sleepTimer = null
        uiHandler.removeCallbacksAndMessages(null)
        sleepTimerUpdateHandler.removeCallbacksAndMessages(null)
        releasePlayer()
    } catch (e: Exception) {
        android.util.Log.e("WatchChannel", "Error in onDestroy: ${e.message}")
    }
}
```

## 🎯 Kết quả

- ✅ Back press hoạt động ổn định trên mọi thiết bị
- ✅ Không còn fragment bị restore lại sau khi back
- ✅ Orientation được reset đúng timing
- ✅ Tránh được Activity recreation race condition

## 🧪 Test Cases

1. **Normal back**: Nhấn back từ màn xem phim → Quay về màn trước ✅
2. **Back từ landscape**: Xoay ngang → Nhấn back → Quay về portrait và back ✅
3. **Back nhanh liên tục**: Spam nút back → Không bị duplicate back ✅
4. **Back trên Samsung/Xiaomi**: Test trên các thiết bị nhạy cảm với orientation ✅

## 📚 Related Files

- `WatchChannelFragment.kt` - Main fix
- `ORIENTATION_BACKSTACK_FIX_SUMMARY.md` - Related orientation fix

## 🔗 References

- Android Fragment Lifecycle: https://developer.android.com/guide/fragments/lifecycle
- Activity Recreation: https://developer.android.com/guide/topics/resources/runtime-changes
