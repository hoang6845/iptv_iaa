package com.iptvplayer.m3u.stream.ui.watch_channel

import android.app.Activity
import android.app.AlertDialog
import android.app.PictureInPictureParams
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Rational
import android.view.View
import android.view.WindowInsetsController
import android.widget.LinearLayout
import android.widget.SeekBar
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.google.android.gms.cast.MediaInfo
import com.google.android.gms.cast.MediaLoadRequestData
import com.google.android.gms.cast.MediaMetadata
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.cast.framework.CastSession
import com.google.android.gms.cast.framework.SessionManagerListener
import com.google.android.gms.cast.framework.media.RemoteMediaClient
import com.iptvplayer.m3u.stream.R
import com.iptvplayer.m3u.stream.databinding.ActivityWatchChannelBinding
import com.iptvplayer.m3u.stream.ui.xtream_movie.FloatingPlayerService
import com.iptvplayer.m3u.stream.utils.ErrorPlayerDialog
import com.iptvplayer.m3u.stream.utils.gone
import com.iptvplayer.m3u.stream.utils.visible
import dagger.hilt.android.AndroidEntryPoint
import hoang.dqm.codebase.base.activity.BaseActivity
import hoang.dqm.codebase.utils.collectLatestFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@AndroidEntryPoint
class WatchChannelActivity : BaseActivity<ActivityWatchChannelBinding, WatchChannelViewModel>() {

    companion object {
        private const val EXTRA_URL         = "extra_url"
        private const val EXTRA_NAME        = "extra_name"
        private const val EXTRA_PLAYLIST_ID = "extra_playlist_id"
        private const val EXTRA_CHANNEL_ID  = "extra_channel_id"

        fun start(
            context: Context,
            url: String,
            name: String? = null,
            playlistId: Long? = null,
            channelId: String? = null,
        ) {
            val intent = Intent(context, WatchChannelActivity::class.java).apply {
                putExtra(EXTRA_URL, url)
                putExtra(EXTRA_NAME, name)
                playlistId?.let { putExtra(EXTRA_PLAYLIST_ID, it) }
                channelId?.let { putExtra(EXTRA_CHANNEL_ID, it) }

                if (context !is Activity) {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }
            context.startActivity(intent)
        }
    }

    // -------------------------------------------------------------------------
    // Constants
    // -------------------------------------------------------------------------
    private val CONTROLS_HIDE_DELAY_MS = 3_500L
    private val SEEK_STEP_MS           = 10_000L
    private val PROGRESS_UPDATE_MS     = 500L

    private val url: String? by lazy { intent.getStringExtra(EXTRA_URL) }
    private val name: String? by lazy { intent.getStringExtra(EXTRA_NAME) }
    private val playlistId: Long? by lazy {
        if (intent.hasExtra(EXTRA_PLAYLIST_ID)) intent.getLongExtra(EXTRA_PLAYLIST_ID, -1L).takeIf { it != -1L }
        else null
    }
    private val channelId: String? by lazy { intent.getStringExtra(EXTRA_CHANNEL_ID) }

    // -------------------------------------------------------------------------
    // State
    // -------------------------------------------------------------------------
    private var player: ExoPlayer? = null
    private var isPlaying    = true
    private var isFullscreen = false
    private var isLocked     = false
    private var isSeeking    = false
    private var isExiting    = false
    private var isBackInProgress = false
    private var progressJob: Job? = null

    private val uiHandler           = Handler(Looper.getMainLooper())
    private val hideControlsRunnable = Runnable { hideControls() }

    private var sleepTimer: CountDownTimer? = null
    private var sleepTimerEndTime = 0L
    private val sleepTimerUpdateHandler = Handler(Looper.getMainLooper())
    private val sleepTimerUpdateRunnable = object : Runnable {
        override fun run() {
            updateSleepTimerIcon()
            sleepTimerUpdateHandler.postDelayed(this, 1000L)
        }
    }

    private var audioManager: AudioManager? = null
    private var maxVolume = 0
    private var retryCount = 0

    // Cast
    private var castContext: CastContext? = null
    private var castSession: CastSession? = null
    private var sessionManagerListener: SessionManagerListener<CastSession>? = null

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------


    override fun initView() {
        adjustInsetsForBottomNavigation(binding.topBar)
        forcePortraitMode()

        try {
            castContext = CastContext.getSharedInstance(this)
            setupSessionManagerListener()
        } catch (e: Exception) {
            android.util.Log.e("CAST", "Init failed: ${e.message}", e)
        }

        binding.root.post { initVolumeAndBrightness() }

        playlistId?.let { id ->
            viewModel.getPlaylist(id)
            channelId?.let { uniqueId -> viewModel.loadFavourite(id, uniqueId) }
        }

        url?.let { u ->
            setupImmersive()
            binding.tvTitle.text = name ?: "Now Playing"
            initPlayer(u)
            scheduleHideControls()
        }
    }

    override fun initListener() {
        // Mute toggle
        binding.muteButton.setOnClickListener {
            val currentVol = audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 0
            if (currentVol > 0) {
                audioManager?.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0)
                binding.volumePanel.findSeekBar()?.progress = 0
                updateMuteIcon(0)
            } else {
                val halfVol = maxVolume / 2
                audioManager?.setStreamVolume(AudioManager.STREAM_MUSIC, halfVol, 0)
                binding.volumePanel.findSeekBar()?.progress = 50
                updateMuteIcon(50)
            }
        }

        binding.btnFavourite.setOnClickListener { viewModel.toggleFavourite() }

        binding.playerView.setOnClickListener  { toggleControls() }
        binding.root.setOnClickListener        { toggleControls() }
        binding.controlsOverlay.setOnClickListener { toggleControls() }

        binding.btnBack.setOnClickListener { handleBackPress() }

        binding.btnPlayPause.setOnClickListener {
            player?.let { exo -> if (exo.isPlaying) exo.pause() else exo.play() }
            rescheduleHideControls()
        }

        binding.btnRewind.setOnClickListener {
            player?.let { exo ->
                exo.seekTo(maxOf(0L, exo.currentPosition - SEEK_STEP_MS))
                showSeekFeedback("-10s")
            }
            rescheduleHideControls()
        }

        binding.btnForward.setOnClickListener {
            player?.let { exo ->
                exo.seekTo(minOf(exo.duration, exo.currentPosition + SEEK_STEP_MS))
                showSeekFeedback("+10s")
            }
            rescheduleHideControls()
        }

        binding.seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    val duration = player?.duration ?: 0L
                    if (duration > 0) binding.tvCurrentTime.text = formatTime((duration * progress) / 1000L)
                }
            }
            override fun onStartTrackingTouch(sb: SeekBar) {
                isSeeking = true
                uiHandler.removeCallbacks(hideControlsRunnable)
            }
            override fun onStopTrackingTouch(sb: SeekBar) {
                isSeeking = false
                val duration = player?.duration ?: 0L
                if (duration > 0) player?.seekTo((duration * sb.progress) / 1000L)
                rescheduleHideControls()
            }
        })

        binding.btnFullscreen.setOnClickListener {
            toggleFullscreen()
            rescheduleHideControls()
        }

        binding.btnSleepTimer.setOnClickListener {
            showSleepTimerDialog()
            rescheduleHideControls()
        }

        binding.btnPopupPlay.setOnClickListener { enterPictureInPicture() }

        binding.btnCast.setOnClickListener {
            showCastDialog()
            rescheduleHideControls()
        }

        binding.btnLock.setOnClickListener {
            isLocked = !isLocked
            updateLockState()
        }
        binding.btnUnlock.setOnClickListener {
            isLocked = !isLocked
            updateLockState()
        }
    }

    override fun initData() {
        viewModel.isPlaying.observe(this) { /* handled by ExoPlayer listener */ }
        viewModel.position.observe(this) { /* handled by ExoPlayer listener */ }

        collectLatestFlow(viewModel.isFavourite) { value ->
            binding.btnFavourite.setImageResource(
                if (value) R.drawable.favourited else R.drawable.favourite
            )
        }
    }

    // -------------------------------------------------------------------------
    // Back press  — finish() instead of popBackStack()
    // -------------------------------------------------------------------------
    override fun onBackPressedDispatcher() {
        handleBackPress()
    }

    private fun handleBackPress() {
        if (isFullscreen) {
            exitFullscreen()
            return
        }

        if (isBackInProgress) return

        isBackInProgress = true
        isExiting = true

        playlistId?.let { pid ->
            channelId?.let { cid ->
                lifecycleScope.launch(Dispatchers.IO) {
                    viewModel.saveFavourite(pid, cid)
                }
            }
        }

        uiHandler.removeCallbacksAndMessages(null)
        sleepTimerUpdateHandler.removeCallbacksAndMessages(null)
        sleepTimer?.cancel()
        sleepTimer = null

        releasePlayer()

        finish()
    }

    // -------------------------------------------------------------------------
    // Player
    // -------------------------------------------------------------------------

    private fun initPlayer(url: String) {
        releasePlayer()
        player = ExoPlayer.Builder(this).build().also { exo ->
            binding.playerView.player = exo
            exo.setMediaItem(buildMediaItem(url))
            exo.prepare()
            exo.seekTo(viewModel.position.value ?: 0L)
            exo.playWhenReady = viewModel.isPlaying.value ?: true

            exo.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(state: Int) {
                    when (state) {
                        Player.STATE_BUFFERING -> binding.progressLoading.visibility = View.VISIBLE
                        Player.STATE_READY -> {
                            binding.progressLoading.visibility = View.GONE
                            binding.tvTotalTime.text = formatTime(exo.duration)
                            startProgressUpdater()
                        }
                        Player.STATE_ENDED -> {
                            isPlaying = false
                            binding.btnPlayPause.setImageResource(R.drawable.ic_play)
                        }
                        else -> binding.progressLoading.visibility = View.GONE
                    }
                }

                override fun onIsPlayingChanged(playing: Boolean) {
                    isPlaying = playing
                    binding.btnPlayPause.setImageResource(
                        if (playing) R.drawable.ic_pause else R.drawable.ic_play
                    )
                }

                override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                    binding.progressLoading.visibility = View.GONE
                    showErrorDialog()
                }
            })
        }
    }

    private fun buildMediaItem(url: String): MediaItem {
        val lower = url.lowercase()
        return when {
            lower.endsWith(".m3u8") || lower.contains(".m3u8?") ->
                MediaItem.Builder().setUri(url).setMimeType(MimeTypes.APPLICATION_M3U8).build()
            lower.endsWith(".mpd") || lower.contains(".mpd?") ->
                MediaItem.Builder()
                    .setUri(url)
                    .setMimeType(MimeTypes.APPLICATION_MPD)
                    .build()
            lower.endsWith(".mp4") || lower.endsWith(".mkv") ||
                    lower.endsWith(".avi") || lower.endsWith(".mov") ||
                    lower.endsWith(".flv") || lower.endsWith(".wmv") ->
                MediaItem.Builder().setUri(url).setMimeType(MimeTypes.VIDEO_MP4).build()

            lower.startsWith("rtmp://") || lower.startsWith("rtsp://") ->
                MediaItem.fromUri(Uri.parse(url))

            else -> MediaItem.fromUri(Uri.parse(url))
        }
    }

    private fun releasePlayer() {
        progressJob?.cancel()
        progressJob = null

        binding.playerView.player = null

        player?.release()
        player = null
    }

    private fun startProgressUpdater() {
        progressJob?.cancel()

        progressJob = lifecycleScope.launch {
            while (true) {
                delay(PROGRESS_UPDATE_MS)

                player?.let { exo ->
                    if (!isSeeking && exo.duration > 0) {
                        val pos = exo.currentPosition
                        val dur = exo.duration
                        val prog = ((pos * 1000L) / dur).toInt()

                        binding.seekBar.progress = prog
                        binding.tvCurrentTime.text = formatTime(pos)
                        binding.tvTotalTime.text = formatTime(dur)
                    }
                }
            }
        }
    }

    // -------------------------------------------------------------------------
    // Controls overlay
    // -------------------------------------------------------------------------

    private fun toggleControls() {
        if (binding.controlsOverlay.isVisible) hideControls() else showControls()
    }

    private fun showControls() {
        binding.controlsOverlay.visibility = View.VISIBLE
        binding.controlsOverlay.animate().alpha(1f).setDuration(200).start()
        scheduleHideControls()
    }

    private fun hideControls() {
        if (isFinishing || isDestroyed) return

        binding.controlsOverlay.animate()
            .alpha(0f)
            .setDuration(300)
            .withEndAction {
                if (!isFinishing && !isDestroyed) {
                    binding.controlsOverlay.visibility = View.GONE
                }
            }
            .start()
    }

    private fun scheduleHideControls() {
        uiHandler.removeCallbacks(hideControlsRunnable)
        uiHandler.postDelayed(hideControlsRunnable, CONTROLS_HIDE_DELAY_MS)
    }

    private fun rescheduleHideControls() = scheduleHideControls()

    // -------------------------------------------------------------------------
    // Lock
    // -------------------------------------------------------------------------

    private fun updateLockState() {
        if (isLocked) {
            binding.topBar.visibility      = View.GONE
            binding.btnRewind.visibility   = View.GONE
            binding.btnPlayPause.visibility = View.GONE
            binding.btnForward.visibility  = View.GONE
            binding.bottomControl.gone()
            binding.btnUnlock.visible()
        } else {
            binding.topBar.visibility      = View.VISIBLE
            binding.btnRewind.visibility   = View.VISIBLE
            binding.btnPlayPause.visibility = View.VISIBLE
            binding.btnForward.visibility  = View.VISIBLE
            binding.bottomControl.visible()
            binding.btnUnlock.gone()
        }
    }

    // -------------------------------------------------------------------------
    // Fullscreen / orientation
    // -------------------------------------------------------------------------
    private fun forcePortraitMode() {
        try {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        } catch (e: Exception) {
            android.util.Log.e("WatchChannel", "Error force portrait: ${e.message}")
        }
    }

    private fun forceLandscapeMode() {
        try {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        } catch (e: Exception) {
            android.util.Log.e("WatchChannel", "Error force landscape: ${e.message}")
        }
    }

    private fun applyCurrentOrientationState() {
        if (isFullscreen) {
            forceLandscapeMode()
        } else {
            forcePortraitMode()
        }
    }

    private fun setupImmersive() {
        hideSystemBars()
    }
    private fun toggleFullscreen() {
        try {
            if (isFullscreen) {
                exitFullscreen()
            } else {
                enterFullscreen()
            }
        } catch (e: Exception) {
            android.util.Log.e("WatchChannel", "Error toggling fullscreen: ${e.message}")
        }
    }

    private fun enterFullscreen() {
        isFullscreen = true

        forceLandscapeMode()

        binding.btnFullscreen.setImageResource(R.drawable.ic_fullscren_exit)
        hideSystemBars()
    }

    private fun exitFullscreen() {
        isFullscreen = false

        forcePortraitMode()

        binding.btnFullscreen.setImageResource(R.drawable.ic_fullscreen)
        showSystemBars()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        try {
            applyCurrentOrientationState()

            binding.btnFullscreen.setImageResource(
                if (isFullscreen) R.drawable.ic_fullscren_exit
                else R.drawable.ic_fullscreen
            )
        } catch (e: Exception) {
            android.util.Log.e("WatchChannel", "Error in onConfigurationChanged: ${e.message}")
        }
    }

    // -------------------------------------------------------------------------
    // System bars
    // -------------------------------------------------------------------------

    private fun hideSystemBars() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.let {
                it.hide(android.view.WindowInsets.Type.systemBars())
                it.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_FULLSCREEN or
                            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    )
        }
    }

    private fun showSystemBars() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.show(android.view.WindowInsets.Type.systemBars())
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
        }
    }

    // -------------------------------------------------------------------------
    // Picture-in-Picture
    // -------------------------------------------------------------------------

    private fun enterPictureInPicture() {
        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)) {
            startOverlayService()
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            enterPictureInPictureMode(
                PictureInPictureParams.Builder().setAspectRatio(Rational(16, 9)).build()
            )
        } else {
            @Suppress("DEPRECATION")
            enterPictureInPictureMode()
        }
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        if (isInPictureInPictureMode) binding.controlsOverlay.visibility = View.GONE
        else showControls()
    }

    private fun startOverlayService() {
        val u = url ?: return
        val serviceIntent = Intent(this, FloatingPlayerService::class.java).apply {
            putExtra("extra_url",  u)
            putExtra("extra_title", name ?: "")
            putExtra("position",  player?.currentPosition ?: 0L)
            putExtra("playing",   player?.isPlaying ?: false)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(serviceIntent)
        else startService(serviceIntent)
        releasePlayer()
        moveTaskToBack(true)
    }

    // -------------------------------------------------------------------------
    // Sleep Timer
    // -------------------------------------------------------------------------

    private fun showSleepTimerDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_sleep_timer, null)
        val dialog = android.app.Dialog(this).apply {
            setContentView(dialogView)
            window?.setBackgroundDrawableResource(android.R.color.transparent)
            window?.setLayout(
                (resources.displayMetrics.widthPixels * 0.85).toInt(),
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        val remaining = getRemainingSeconds()
        if (remaining > 0) {
            dialogView.findViewById<android.widget.TextView>(R.id.tvDialogTitle).text =
                getString(R.string.text_sleep_timer_left, formatCountdown(remaining))
        }

        mapOf(R.id.optOff to 0, R.id.opt10 to 10, R.id.opt30 to 30, R.id.opt60 to 60)
            .forEach { (viewId, minutes) ->
                dialogView.findViewById<android.widget.TextView>(viewId).setOnClickListener {
                    cancelSleepTimer()
                    if (minutes > 0) startSleepTimer(minutes * 60L)
                    dialog.dismiss()
                }
            }

        dialogView.findViewById<android.widget.TextView>(R.id.optCustom).setOnClickListener {
            dialog.dismiss()
            showCustomSleepTimerDialog()
        }
        dialogView.findViewById<android.widget.TextView>(R.id.btnCancel).setOnClickListener {
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun showCustomSleepTimerDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_custom_timer, null)
        val dialog = android.app.Dialog(this).apply {
            setContentView(dialogView)
            window?.setBackgroundDrawableResource(android.R.color.transparent)
            window?.setLayout(
                (resources.displayMetrics.widthPixels * 0.85).toInt(),
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
        val etMinutes = dialogView.findViewById<android.widget.EditText>(R.id.etMinutes)
        dialogView.findViewById<android.widget.TextView>(R.id.btnCustomConfirm).setOnClickListener {
            val minutes = etMinutes.text.toString().toLongOrNull()
            if (minutes != null && minutes in 1..180) {
                cancelSleepTimer(); startSleepTimer(minutes * 60L); dialog.dismiss()
            } else {
                etMinutes.error = getString(R.string.text_enter_a_value_from_1_to_180_minutes)
            }
        }
        dialogView.findViewById<android.widget.TextView>(R.id.btnCustomCancel).setOnClickListener {
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun startSleepTimer(durationSeconds: Long) {
        sleepTimerEndTime = System.currentTimeMillis() + durationSeconds * 1000L
        sleepTimer = object : CountDownTimer(durationSeconds * 1000L, 1000L) {
            override fun onTick(ms: Long) {}
            override fun onFinish() {
                player?.pause()
                sleepTimerEndTime = 0L
                sleepTimerUpdateHandler.removeCallbacks(sleepTimerUpdateRunnable)
                binding.btnSleepTimer.setBackgroundResource(R.drawable.ic_bg_watch)
                android.widget.Toast.makeText(
                    this@WatchChannelActivity,
                    getString(R.string.text_end_timer_pause_playback),
                    android.widget.Toast.LENGTH_LONG
                ).show()
            }
        }.start()
        binding.btnSleepTimer.setColorFilter(
            android.graphics.Color.parseColor("#FFC107"), android.graphics.PorterDuff.Mode.SRC_IN
        )
        sleepTimerUpdateHandler.post(sleepTimerUpdateRunnable)
        android.widget.Toast.makeText(this, formatCountdown(durationSeconds), android.widget.Toast.LENGTH_SHORT).show()
    }

    private fun cancelSleepTimer() {
        sleepTimer?.cancel(); sleepTimer = null; sleepTimerEndTime = 0L
        sleepTimerUpdateHandler.removeCallbacks(sleepTimerUpdateRunnable)
        binding.btnSleepTimer.setColorFilter(android.graphics.Color.WHITE, android.graphics.PorterDuff.Mode.SRC_IN)
    }

    private fun updateSleepTimerIcon() {
        val remaining = getRemainingSeconds()
        if (remaining <= 0) { sleepTimerUpdateHandler.removeCallbacks(sleepTimerUpdateRunnable); return }
        binding.btnSleepTimer.imageAlpha = if (remaining in 1..60 && (remaining % 2L) == 0L) 160 else 255
    }

    private fun getRemainingSeconds(): Long {
        if (sleepTimerEndTime == 0L) return 0L
        return maxOf(0L, (sleepTimerEndTime - System.currentTimeMillis()) / 1000L)
    }

    private fun formatCountdown(totalSeconds: Long): String {
        val h = totalSeconds / 3600; val m = (totalSeconds % 3600) / 60; val s = totalSeconds % 60
        return if (h > 0) "%d hr %02d min".format(h, m)
        else if (m > 0) "%d min %02d sec".format(m, s)
        else "%d sec".format(s)
    }

    // -------------------------------------------------------------------------
    // Cast
    // -------------------------------------------------------------------------

    private fun showCastDialog() {
        val castCtx = castContext ?: run {
            AlertDialog.Builder(this, R.style.AlertDialogDark)
                .setTitle(getString(R.string.text_cast_not_available))
                .setMessage(getString(R.string.text_google_play_services_has_not_been_initialized))
                .setPositiveButton(getString(R.string.text_ok), null).show()
            return
        }
        if (castSession?.isConnected == true) {
            AlertDialog.Builder(this, R.style.AlertDialogDark)
                .setTitle(getString(R.string.text_casting_to_tv))
                .setMessage(getString(R.string.text_what_would_you_like_to_do))
                .setPositiveButton(getString(R.string.text_disconnect)) { _, _ -> castCtx.sessionManager.endCurrentSession(true) }
                .setNeutralButton(getString(R.string.text_restart_from_beginning)) { _, _ -> loadMediaOnCast() }
                .setNegativeButton(getString(R.string.text_cancel), null).show()
            return
        }
        val selector = androidx.mediarouter.media.MediaRouteSelector.Builder()
            .addControlCategory(
                com.google.android.gms.cast.CastMediaControlIntent.categoryForCast(
                    com.google.android.gms.cast.CastMediaControlIntent.DEFAULT_MEDIA_RECEIVER_APPLICATION_ID
                )
            ).build()
        androidx.mediarouter.app.MediaRouteChooserDialogFragment().apply {
            routeSelector = selector
        }.show(supportFragmentManager, "MediaRouteChooser")
    }

    private fun setupSessionManagerListener() {
        sessionManagerListener = object : SessionManagerListener<CastSession> {
            override fun onSessionStarted(session: CastSession, sessionId: String) {
                castSession = session; loadMediaOnCast(); player?.pause(); updateCastIcon(true)
            }
            override fun onSessionResumed(session: CastSession, wasSuspended: Boolean) {
                castSession = session; updateCastIcon(true)
            }
            override fun onSessionEnded(session: CastSession, error: Int) {
                castSession = null; player?.play(); updateCastIcon(false)
            }
            override fun onSessionSuspended(session: CastSession, reason: Int) {}
            override fun onSessionStarting(session: CastSession) {}
            override fun onSessionStartFailed(session: CastSession, error: Int) {
                android.widget.Toast.makeText(
                    this@WatchChannelActivity,
                    getString(R.string.text_failed_to_connect_to_cast_device),
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
            override fun onSessionEnding(session: CastSession) {}
            override fun onSessionResuming(session: CastSession, sessionId: String) {}
            override fun onSessionResumeFailed(session: CastSession, error: Int) {}
        }
    }

    private fun loadMediaOnCast() {
        val remoteClient = castSession?.remoteMediaClient ?: return
        val mediaUrl = url ?: return
        val metadata = MediaMetadata(MediaMetadata.MEDIA_TYPE_MOVIE).apply {
            putString(MediaMetadata.KEY_TITLE, name ?: "Now Playing")
        }
        val contentType = when {
            mediaUrl.lowercase().contains(".m3u8") -> "application/x-mpegurl"
            mediaUrl.lowercase().endsWith(".mp4")  -> "video/mp4"
            else -> "video/mp4"
        }
        val mediaInfo = MediaInfo.Builder(mediaUrl)
            .setStreamType(MediaInfo.STREAM_TYPE_LIVE)
            .setContentType(contentType)
            .setMetadata(metadata)
            .build()
        remoteClient.load(
            MediaLoadRequestData.Builder()
                .setMediaInfo(mediaInfo)
                .setAutoplay(true)
                .setCurrentTime(player?.currentPosition ?: 0L)
                .build()
        )
    }

    private fun updateCastIcon(connected: Boolean) {
        binding.btnCast.setColorFilter(
            if (connected) android.graphics.Color.parseColor("#045DCC") else android.graphics.Color.WHITE,
            android.graphics.PorterDuff.Mode.SRC_IN
        )
    }

    // -------------------------------------------------------------------------
    // Volume & Brightness
    // -------------------------------------------------------------------------

    private fun initVolumeAndBrightness() {
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        maxVolume    = audioManager!!.getStreamMaxVolume(AudioManager.STREAM_MUSIC)

        val volPercent = (audioManager!!.getStreamVolume(AudioManager.STREAM_MUSIC) * 100) / maxVolume
        binding.volumePanel.findSeekBar()?.progress = volPercent
        binding.lightPanel.findSeekBar()?.progress  = getCurrentBrightness()

        binding.volumePanel.findSeekBar()?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    audioManager?.setStreamVolume(AudioManager.STREAM_MUSIC, (progress * maxVolume) / 100, 0)
                    updateMuteIcon(progress)
                }
            }
            override fun onStartTrackingTouch(sb: SeekBar) {}
            override fun onStopTrackingTouch(sb: SeekBar) {}
        })

        binding.lightPanel.findSeekBar()?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser) setBrightness(progress)
            }
            override fun onStartTrackingTouch(sb: SeekBar) {}
            override fun onStopTrackingTouch(sb: SeekBar) {}
        })
    }

    private fun getCurrentBrightness(): Int {
        val lp = window.attributes
        if (lp.screenBrightness >= 0f) return (lp.screenBrightness * 100).toInt()
        return try {
            (Settings.System.getInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS) * 100) / 255
        } catch (e: Exception) { 50 }
    }

    private fun setBrightness(progress: Int) {
        val lp = window.attributes
        lp.screenBrightness = (progress.coerceAtLeast(5)) / 100f
        window.attributes = lp
    }

    private fun updateMuteIcon(volumePercent: Int) {
        binding.muteButton.setImageResource(
            if (volumePercent == 0) R.drawable.ic_volume_off else R.drawable.ic_volumn
        )
    }

    private fun LinearLayout.findSeekBar(): SeekBar? {
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            if (child is android.widget.FrameLayout) {
                for (j in 0 until child.childCount) {
                    if (child.getChildAt(j) is SeekBar) return child.getChildAt(j) as SeekBar
                }
            }
        }
        return null
    }

    // -------------------------------------------------------------------------
    // Error dialog
    // -------------------------------------------------------------------------

    private fun showErrorDialog() {
        try {
            ErrorPlayerDialog(
                activity = this,
                onReload = {
                    retryCount++
                    url?.let {
                        releasePlayer()
                        initPlayer(it)
                    }
                },
                onBack   = { handleBackPress() }
            ).show()
        } catch (e: Exception) {
            android.util.Log.e("WatchChannel", "Error showing dialog: ${e.message}")
            handleBackPress()
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private fun showSeekFeedback(text: String) {
        binding.tvSeekFeedback.text = text
        binding.tvSeekFeedback.visibility = View.VISIBLE
        binding.tvSeekFeedback.alpha = 1f
        uiHandler.postDelayed({
            binding.tvSeekFeedback.animate().alpha(0f).setDuration(500)
                .withEndAction { binding.tvSeekFeedback.visibility = View.GONE }.start()
        }, 700)
    }

    private fun formatTime(ms: Long): String {
        if (ms <= 0) return "00:00"
        val totalSeconds = ms / 1000
        val hours   = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return if (hours > 0) "%02d:%02d:%02d".format(hours, minutes, seconds)
        else "%02d:%02d".format(minutes, seconds)
    }


    override fun onResume() {
        super.onResume()
        if (isExiting) return

        applyCurrentOrientationState()
        hideSystemBars()

        sessionManagerListener?.let {
            castContext?.sessionManager?.addSessionManagerListener(it, CastSession::class.java)
        }
        castSession = castContext?.sessionManager?.currentCastSession

        if (player == null) {
            url?.let { initPlayer(it) }
            return
        }
        player?.let { exo ->
            val savedPlaying = viewModel.isPlaying.value ?: true
            when (exo.playbackState) {
                Player.STATE_IDLE, Player.STATE_ENDED -> {
                    exo.prepare()
                    val savedPos = viewModel.position.value ?: 0L
                    if (savedPos > 0) exo.seekTo(savedPos)
                    if (savedPlaying) exo.play()
                }
                Player.STATE_READY, Player.STATE_BUFFERING -> {
                    if (savedPlaying) exo.play()
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        sessionManagerListener?.let {
            castContext?.sessionManager?.removeSessionManagerListener(it, CastSession::class.java)
        }
        viewModel.setPosition(player?.currentPosition ?: 0L)
        viewModel.setPlay(player?.isPlaying ?: false)
        if (!isInPictureInPictureMode) player?.pause()
    }

    override fun onStop() {
        super.onStop()
        if (!isInPictureInPictureMode) player?.pause()
    }

    override fun onDestroy() {
        super.onDestroy()

        try {
            isExiting = true

            sleepTimer?.cancel()
            sleepTimer = null

            uiHandler.removeCallbacksAndMessages(null)
            sleepTimerUpdateHandler.removeCallbacksAndMessages(null)

            releasePlayer()
        } catch (e: Exception) {
            android.util.Log.e("WatchChannel", "Error in onDestroy: ${e.message}")
        }
    }
}