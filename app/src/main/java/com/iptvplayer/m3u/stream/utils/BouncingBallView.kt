package com.iptvplayer.m3u.stream.utils

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap

/**
 * Custom View hiển thị hiệu ứng quả banh nhảy lên xuống TẠI CHỖ, vừa xoay,
 * kèm vệt bóng (shadow) dưới chân co giãn to/nhỏ theo độ cao của banh:
 *  - Banh chạm đất  -> vệt bóng to, đậm.
 *  - Banh bay lên cao -> vệt bóng nhỏ, mờ hơn.
 *
 * Cách dùng:
 *   1. Thêm view vào layout XML (xem activity_main.xml mẫu).
 *   2. Trong code:
 *        ballView.setBallDrawable(R.drawable.ic_soccer_ball)
 *        ballView.startBounce()
 *      Khi không cần nữa: ballView.stopBounce()
 */
class BouncingBallView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // ----------------- Các tham số có thể tuỳ chỉnh -----------------

    /** Thời gian 1 lần nhảy (ms): từ lúc rời đất -> lên đỉnh -> chạm đất lại. */
    var bounceDuration: Long = 700L
        set(value) {
            field = value
            if (animator?.isRunning == true) startBounce()
        }

    /** Số vòng quay (360°) mà banh thực hiện trong MỖI lần nhảy. */
    var rotationsPerBounce: Float = 1f

    /** Độ cao nhảy tối đa, tính theo tỉ lệ % chiều cao của view (0..1). */
    var jumpHeightRatio: Float = 0.45f

    /** Kích thước quả banh, tính theo tỉ lệ % chiều rộng của view (0..1). */
    var ballSizeRatio: Float = 0.5f

    /** Độ rộng vệt bóng tối đa (lúc banh chạm đất), theo tỉ lệ % đường kính banh. */
    var shadowMaxWidthRatio: Float = 1.0f

    /** Tỉ lệ thu nhỏ tối thiểu của vệt bóng khi banh ở đỉnh (so với lúc to nhất). */
    var shadowMinScale: Float = 0.45f

    // ----------------- Nội bộ -----------------

    private var ballBitmap: Bitmap? = null
    private val ballPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }

    /** progress chạy từ 0 -> 1 trong MỖI lần nhảy (0 = vừa rời đất, 1 = vừa chạm đất lại). */
    private var progress = 0f
    private var animator: ValueAnimator? = null

    /** Gán ảnh quả banh từ resource (PNG hoặc vector drawable). */
    fun setBallDrawable(resId: Int) {
        val drawable = ContextCompat.getDrawable(context, resId) ?: return
        val w = drawable.intrinsicWidth.takeIf { it > 0 } ?: 256
        val h = drawable.intrinsicHeight.takeIf { it > 0 } ?: 256
        ballBitmap = drawable.toBitmap(width = w, height = h)
        invalidate()
    }

    /** Hoặc gán trực tiếp 1 Bitmap nếu bạn đã có sẵn. */
    fun setBallBitmap(bitmap: Bitmap) {
        ballBitmap = bitmap
        invalidate()
    }

    /** Bắt đầu animation: nhảy + xoay liên tục, lặp vô hạn. */
    fun startBounce() {
        animator?.cancel()
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = bounceDuration
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.RESTART
            interpolator = LinearInterpolator() // độ "rơi tự do" được tự tính bên dưới
            addUpdateListener {
                progress = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    /** Dừng animation, đưa banh về mặt đất. */
    fun stopBounce() {
        animator?.cancel()
        animator = null
        progress = 0f
        invalidate()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        animator?.cancel()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return

        val ballSize = w * ballSizeRatio
        val groundY = h * 0.92f // vị trí "mặt đất" - nơi banh chạm và vệt bóng nằm

        val t = progress

        // Quỹ đạo parabol mô phỏng trọng lực: 0 ở 2 đầu (mặt đất), max ở giữa (đỉnh).
        // => banh đi nhanh gần đất, chậm dần khi lên gần đỉnh (giống vật thể bị rơi tự do).
        val heightFactor = 4f * t * (1f - t) // 0..1
        val maxJump = h * jumpHeightRatio
        val ballOffsetY = maxJump * heightFactor

        val ballCenterX = w / 2f
        val ballCenterY = groundY - ballSize / 2f - ballOffsetY

        // ----- Vệt bóng dưới chân -----
        // heightFactor = 0 (đang ở đất)  -> scale = 1f   (to nhất)
        // heightFactor = 1 (đang ở đỉnh) -> scale = shadowMinScale (nhỏ nhất)
        val shadowScale = 1f - (1f - shadowMinScale) * heightFactor
        val shadowAlpha = (90 + (1f - heightFactor) * 90f).toInt().coerceIn(0, 180)

        val shadowWidth = ballSize * shadowMaxWidthRatio * shadowScale
        val shadowHeight = shadowWidth * 0.28f

        shadowPaint.alpha = shadowAlpha
        canvas.save()
        canvas.translate(ballCenterX, groundY)
        canvas.drawOval(
            -shadowWidth / 2f, -shadowHeight / 2f,
            shadowWidth / 2f, shadowHeight / 2f,
            shadowPaint
        )
        canvas.restore()

        // ----- Vẽ quả banh, xoay theo progress -----
        val bitmap = ballBitmap
        if (bitmap != null) {
            val rotation = t * 360f * rotationsPerBounce
            canvas.save()
            canvas.translate(ballCenterX, ballCenterY)
            canvas.rotate(rotation)
            val half = ballSize / 2f
            canvas.drawBitmap(bitmap, null, RectF(-half, -half, half, half), ballPaint)
            canvas.restore()
        }
    }
}