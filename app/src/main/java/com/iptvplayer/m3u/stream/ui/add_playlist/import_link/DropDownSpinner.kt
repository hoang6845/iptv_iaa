package com.iptvplayer.m3u.stream.ui.add_playlist.import_link

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatSpinner
import hoang.dqm.codebase.R

class DropDownSpinner @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : AppCompatSpinner(context, attrs, androidx.appcompat.R.attr.spinnerStyle, MODE_DROPDOWN) {

    override fun performClick(): Boolean {

        dropDownVerticalOffset = height + 6
        dropDownHorizontalOffset = 0

        setPopupMaxHeight()

        return super.performClick()
    }

    private fun setPopupMaxHeight() {
        try {
            val field = AppCompatSpinner::class.java.getDeclaredField("mPopup")
            field.isAccessible = true
            val popup = field.get(this)

            val method = popup.javaClass.getMethod("setHeight", Int::class.javaPrimitiveType)

            val maxHeight = resources.getDimensionPixelSize(R.dimen._200sdp)
            method.invoke(popup, maxHeight)

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}