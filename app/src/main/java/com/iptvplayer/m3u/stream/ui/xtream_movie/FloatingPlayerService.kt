package com.iptvplayer.m3u.stream.ui.xtream_movie

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageButton
import androidx.annotation.OptIn
import androidx.core.app.NotificationCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.iptvplayer.m3u.stream.R

@OptIn(UnstableApi::class)
class FloatingPlayerService : Service() {

    companion object {
        const val CHANNEL_ID = "floating_player_channel"
        const val NOTIF_ID   = 1001
    }

    val EXTRA_URL = "extra_url"
    val EXTRA_TITLE = "extra_title"

    private var windowManager : WindowManager? = null
    private var floatingView  : View?           = null
    private var player        : ExoPlayer?      = null

    private var videoUrl      = ""
    private var videoTitle    = ""
    private var startPosition = 0L
    private var startPlaying  = true

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIF_ID, buildNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        videoUrl      = intent?.getStringExtra(EXTRA_URL)   ?: ""
        videoTitle    = intent?.getStringExtra(EXTRA_TITLE) ?: ""
        startPosition = intent?.getLongExtra("position", 0L)                ?: 0L
        startPlaying  = intent?.getBooleanExtra("playing", true)            ?: true

        showFloatingWindow()
        return START_NOT_STICKY
    }

    // ────────────────────────────────────────────────────────────────────────
    // FLOATING WINDOW
    // ────────────────────────────────────────────────────────────────────────
    private fun showFloatingWindow() {
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        val inflater = LayoutInflater.from(this)
        floatingView = inflater.inflate(R.layout.layout_floating_player, null)

        val params = WindowManager.LayoutParams(
            resources.getDimensionPixelSize(R.dimen.pip_width),
            resources.getDimensionPixelSize(R.dimen.pip_height),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.END
            x = 16
            y = 80
        }

        windowManager?.addView(floatingView, params)
        setupFloatingPlayer()
        setupFloatingDrag(params)
        setupFloatingButtons()
    }

    private fun setupFloatingPlayer() {
        val pipPlayerView = floatingView?.findViewById<PlayerView>(R.id.pipPlayerView)

        player = ExoPlayer.Builder(this).build().also { exo ->
            pipPlayerView?.player = exo
            val mediaItem = MediaItem.fromUri(Uri.parse(videoUrl))
            exo.setMediaItem(mediaItem)
            exo.prepare()
            exo.seekTo(startPosition)
            exo.playWhenReady = startPlaying
        }
    }

    private fun setupFloatingButtons() {
        val floatingView = this.floatingView ?: return

        // Tap body → reopen PlayerActivity
        floatingView.setOnClickListener {
            reopenPlayerActivity()
        }

        // Close button
        floatingView.findViewById<ImageButton>(R.id.btnPipClose)?.setOnClickListener {
            stopSelf()
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupFloatingDrag(params: WindowManager.LayoutParams) {
        val view = floatingView ?: return
        var initialX = 0; var initialY = 0
        var initialTouchX = 0f; var initialTouchY = 0f
        var moved = false

        view.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX      = params.x
                    initialY      = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    moved = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - initialTouchX).toInt()
                    val dy = (event.rawY - initialTouchY).toInt()
                    if (Math.abs(dx) > 5 || Math.abs(dy) > 5) moved = true
                    params.x = initialX - dx
                    params.y = initialY - dy
                    windowManager?.updateViewLayout(view, params)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!moved) reopenPlayerActivity()
                    true
                }
                else -> false
            }
        }
    }

    private fun reopenPlayerActivity() {
        val intent = Intent(this, XtreamMovieFragment::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_URL,   videoUrl)
            putExtra(EXTRA_TITLE, videoTitle)
            putExtra("resume_position", player?.currentPosition ?: 0L)
            putExtra("resume_playing",  player?.isPlaying ?: false)
        }
        startActivity(intent)
        stopSelf()
    }

    // ────────────────────────────────────────────────────────────────────────
    // NOTIFICATION
    // ────────────────────────────────────────────────────────────────────────
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Floating Player",
                NotificationManager.IMPORTANCE_LOW
            ).apply { description = "IPTV mini player" }
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val tapIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, XtreamMovieFragment::class.java).apply {
                putExtra(EXTRA_URL,   videoUrl)
                putExtra(EXTRA_TITLE, videoTitle)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Now Playing")
            .setContentText(videoTitle)
            .setSmallIcon(R.drawable.ic_play)
            .setContentIntent(tapIntent)
            .setOngoing(true)
            .build()
    }

    // ────────────────────────────────────────────────────────────────────────
    // CLEANUP
    // ────────────────────────────────────────────────────────────────────────
    override fun onDestroy() {
        super.onDestroy()
        floatingView?.let { windowManager?.removeView(it) }
        player?.release()
        player = null
    }
}