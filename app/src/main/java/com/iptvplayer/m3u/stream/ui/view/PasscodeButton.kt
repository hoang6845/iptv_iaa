package com.iptvplayer.m3u.stream.ui.view

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatButton
import androidx.core.content.ContextCompat
import androidx.core.content.withStyledAttributes
import com.iptvplayer.m3u.stream.R

class PasscodeButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatButton(context, attrs, defStyleAttr) {

    var isBackspace: Boolean = false
        private set

    init {
        context.withStyledAttributes(attrs, R.styleable.PasscodeButton) {
            isBackspace = getBoolean(R.styleable.PasscodeButton_isBackspace, false)
        }

        if (isBackspace) {
            text = null
            // Xóa compound drawable cũ, vẽ thủ công trong onDraw
            setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (!isBackspace) return

        val drawable = ContextCompat.getDrawable(context, R.drawable.ic_backspace) ?: return

        val dw = drawable.intrinsicWidth
        val dh = drawable.intrinsicHeight

        val left = (width - dw) / 2
        val top  = (height - dh) / 2

        drawable.setBounds(left, top, left + dw, top + dh)
        drawable.draw(canvas)
    }
}