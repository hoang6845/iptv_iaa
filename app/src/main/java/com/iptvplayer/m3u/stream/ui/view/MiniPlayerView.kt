package com.iptvplayer.m3u.stream.ui.view

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.view.GestureDetectorCompat
import androidx.media3.ui.PlayerView
import com.iptvplayer.m3u.stream.R
import com.iptvplayer.m3u.stream.main.PlayerManager
import com.iptvplayer.m3u.stream.model.entity.Channel
import kotlin.math.abs

class MiniPlayerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val playerView: PlayerView
    private val tvChannelName: TextView
    private val btnClose: ImageButton
    private val gestureDetector: GestureDetectorCompat
    
    private var initialX = 0f
    private var initialY = 0f
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    
    var onCloseListener: (() -> Unit)? = null
    var onClickListener: (() -> Unit)? = null
    
    private val DISMISS_THRESHOLD = 200f // pixels to dismiss
    
    init {
        LayoutInflater.from(context).inflate(R.layout.view_mini_player, this, true)
        
        playerView = findViewById(R.id.miniPlayerView)
        tvChannelName = findViewById(R.id.tvMiniChannelName)
        btnClose = findViewById(R.id.btnCloseMini)
        
        btnClose.setOnClickListener {
            onCloseListener?.invoke()
        }
        
        gestureDetector = GestureDetectorCompat(context, object : GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                onClickListener?.invoke()
                return true
            }
            
            override fun onDown(e: MotionEvent): Boolean {
                return true
            }
        })
        
        setupDragBehavior()
    }
    
    @SuppressLint("ClickableViewAccessibility")
    private fun setupDragBehavior() {
        setOnTouchListener { view, event ->
            gestureDetector.onTouchEvent(event)
            
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = view.x
                    initialY = view.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                
                MotionEvent.ACTION_MOVE -> {
                    val deltaX = event.rawX - initialTouchX
                    val deltaY = event.rawY - initialTouchY
                    
                    view.x = initialX + deltaX
                    view.y = initialY + deltaY
                    true
                }
                
                MotionEvent.ACTION_UP -> {
                    val deltaX = event.rawX - initialTouchX
                    val deltaY = event.rawY - initialTouchY
                    
                    // Check if should dismiss
                    if (abs(deltaY) > DISMISS_THRESHOLD) {
                        animateDismiss(deltaY > 0)
                    } else {
                        // Snap to edge
                        snapToEdge()
                    }
                    true
                }
                
                else -> false
            }
        }
    }
    
    private fun snapToEdge() {
        val displayMetrics = resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val viewCenterX = x + width / 2
        
        val targetX = if (viewCenterX < screenWidth / 2) {
            16f // Left edge with margin
        } else {
            screenWidth - width - 16f // Right edge with margin
        }
        
        ValueAnimator.ofFloat(x, targetX).apply {
            duration = 200
            addUpdateListener { animator ->
                x = animator.animatedValue as Float
            }
            start()
        }
    }
    
    private fun animateDismiss(downward: Boolean) {
        val displayMetrics = resources.displayMetrics
        val targetY = if (downward) {
            displayMetrics.heightPixels.toFloat()
        } else {
            -height.toFloat()
        }
        
        ValueAnimator.ofFloat(y, targetY).apply {
            duration = 250
            addUpdateListener { animator ->
                y = animator.animatedValue as Float
                alpha = 1f - (animator.animatedFraction * 0.5f)
            }
            start()
        }
        
        postDelayed({
            onCloseListener?.invoke()
        }, 250)
    }
    
    fun setChannel(channel: Channel) {
        tvChannelName.text = channel.name
        playerView.player = PlayerManager.getPlayer(context)
    }
    
    fun getPlayerView(): PlayerView {
        return playerView
    }
    
    fun setInitialPosition() {
        val displayMetrics = resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels
        
        // Position ở góc dưới phải
        x = (screenWidth - width - 16).toFloat()
        y = (screenHeight - height - 16 - 200).toFloat() // 200 = navigation bar height
    }
}