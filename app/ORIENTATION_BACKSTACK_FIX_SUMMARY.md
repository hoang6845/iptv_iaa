# 🔥 Fix: Backstack bị xóa khi xoay màn hình

## Vấn đề
Một số thiết bị không back được từ `WatchChannelFragment`, đặc biệt sau khi xoay màn hình.

## Nguyên nhân chính
**`SCREEN_ORIENTATION_SENSOR_LANDSCAPE` gây Activity recreation** → Backstack bị mất

## Giải pháp nhanh

### 1. Thay đổi orientation mode
```kotlin
// ❌ TRƯỚC (có vấn đề)
requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE

// ✅ SAU (ổn định)
requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
```

### 2. Xử lý orientation change
```kotlin
override fun onConfigurationChanged(newConfig: Configuration) {
    super.onConfigurationChanged(newConfig)
    // Sync UI state với orientation thực tế
    when (newConfig.orientation) {
        Configuration.ORIENTATION_LANDSCAPE -> {
            isFullscreen = true
            binding.btnFullscreen.setImageResource(R.drawable.ic_fullscren_exit)
        }
        Configuration.ORIENTATION_PORTRAIT -> {
            isFullscreen = false
            binding.btnFullscreen.setImageResource(R.drawable.ic_fullscreen)
        }
    }
}
```

### 3. Chống double back
```kotlin
private var isBackInProgress = false

private fun handleBackPress() {
    if (isBackInProgress) return  // Ngăn double back
    isBackInProgress = true
    
    // ... back logic ...
    
    // Reset flag trong finally block
    finally {
        isBackInProgress = false
    }
}
```

## Tại sao SENSOR_LANDSCAPE gây vấn đề?

| Mode | Behavior | Vấn đề |
|------|----------|--------|
| `SENSOR_LANDSCAPE` | Tự động xoay giữa landscape trái/phải theo sensor | Mỗi lần xoay = 1 config change → Có thể trigger recreation |
| `LANDSCAPE` | Cố định 1 hướng landscape | Ít config changes → Ổn định hơn |

## Test cases quan trọng
1. ✅ Fullscreen → Xoay màn hình → Back
2. ✅ Xoay nhiều lần → Back
3. ✅ Spam nút back nhanh

## Files thay đổi
- `app/src/main/java/com/silverlabtech/iptv/smartplayer/ui/watch_channel/WatchChannelFragment.kt`

## Chi tiết đầy đủ
Xem file `WATCH_CHANNEL_BACK_FIX.md` để biết thêm chi tiết.
