package com.iptvplayer.m3u.stream.ui.watch_channel

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
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.util.Rational
import android.view.View
import android.view.WindowInsetsController
import android.widget.LinearLayout
import android.widget.SeekBar
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
import com.iptvplayer.m3u.stream.databinding.FragmentWatchChannelBinding
import com.iptvplayer.m3u.stream.ui.xtream_movie.FloatingPlayerService
import com.iptvplayer.m3u.stream.utils.ErrorPlayerDialog
import com.iptvplayer.m3u.stream.utils.gone
import com.iptvplayer.m3u.stream.utils.visible
import dagger.hilt.android.AndroidEntryPoint
import hoang.dqm.codebase.base.activity.BaseFragment
import hoang.dqm.codebase.base.activity.onBackPressed
import hoang.dqm.codebase.base.activity.popBackStack
import hoang.dqm.codebase.utils.collectLatestFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import tpt.dev.monetization.ads.preload.showInterstitialAd

@AndroidEntryPoint
class WatchChannelFragment : BaseFragment<FragmentWatchChannelBinding, WatchChannelViewModel>() {
    val EXTRA_URL = "extra_url"
    val EXTRA_TITLE = "extra_title"
    private val CONTROLS_HIDE_DELAY_MS = 3_500L
    private val SEEK_STEP_MS = 10_000L
    private val PROGRESS_UPDATE_MS = 500L

    private var player: ExoPlayer? = null
    private var isPlaying: Boolean = true
    private var isFullscreen: Boolean = false
    private var isLocked: Boolean = false
    private var isBackInProgress = false
    private var isExiting = false

    private val uiHandler = Handler(Looper.getMainLooper())
    private val hideControlsRunnable = Runnable { hideControls() }
    private var sleepTimer: CountDownTimer? = null
    private var isSeeking: Boolean = false
    private var audioManager: AudioManager? = null
    private var maxVolume: Int = 0
    private val url: String? by lazy {
        arguments?.getString("url")
    }

    private val name: String? by lazy {
        arguments?.getString("name")
    }

    private val playlistId: Long? by lazy {
        arguments?.getLong("playlistId")
    }

    val channelId: String? by lazy {
        arguments?.getString("channelId")
    }
    private var retryCount = 0
    private val MAX_RETRY = 0
    private var sleepTimerEndTime: Long = 0L
    private val sleepTimerUpdateHandler = Handler(Looper.getMainLooper())
    private val sleepTimerUpdateRunnable = object : Runnable {
        override fun run() {
            updateSleepTimerIcon()
            sleepTimerUpdateHandler.postDelayed(this, 1000L)
        }
    }

    private var castContext: CastContext? = null
    private var castSession: CastSession? = null
    private var sessionManagerListener: SessionManagerListener<CastSession>? = null

    override fun initView() {
        forcePortraitMode()
        adjustInsetsForBottomNavigation(binding.topBar)
        try {
            castContext = CastContext.getSharedInstance(requireContext())
            setupSessionManagerListener()
        } catch (e: Exception) {
            android.util.Log.e("CAST", "Init failed: ${e.message}", e)
        }
        binding.root.post {
            initVolumeAndBrightness()
        }
        playlistId?.let { id ->
            viewModel.getPlaylist(id)
            channelId?.let { uniqueId ->
                viewModel.loadFavourite(id, uniqueId)
            }
        }
        url?.let { url ->
            setupImmersive()

            val title = name ?: "Now Playing"
            binding.tvTitle.text = title

            initPlayer(url)
            scheduleHideControls()
        }

    }

    override fun initListener() {
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
        binding.btnFavourite.setOnClickListener {
            viewModel.toggleFavourite()
        }
        onBackPressed {
            handleBackPress()
        }
        binding.playerView.setOnClickListener {
//            if (isLocked) return@setOnClickListener
            toggleControls()
        }
        binding.root.setOnClickListener {
            toggleControls()
        }
        binding.controlsOverlay.setOnClickListener {
//            if (isLocked) return@setOnClickListener
            toggleControls()
        }

        binding.btnBack.setOnClickListener {
            handleBackPress()
        }

        binding.btnPlayPause.setOnClickListener {
            player?.let { exo ->
                if (exo.isPlaying) exo.pause() else exo.play()
            }
            rescheduleHideControls()
        }

        // Rewind 10s
        binding.btnRewind.setOnClickListener {
            player?.let { exo ->
                val newPos = maxOf(0L, exo.currentPosition - SEEK_STEP_MS)
                exo.seekTo(newPos)
                showSeekFeedback("-10s")
            }
            rescheduleHideControls()
        }

        // Forward 10s
        binding.btnForward.setOnClickListener {
            player?.let { exo ->
                val newPos = minOf(exo.duration, exo.currentPosition + SEEK_STEP_MS)
                exo.seekTo(newPos)
                showSeekFeedback("+10s")
            }
            rescheduleHideControls()
        }

        // Seekbar
        binding.seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    val duration = player?.duration ?: 0L
                    if (duration > 0) {
                        val pos = (duration * progress) / 1000L
                        binding.tvCurrentTime.text = formatTime(pos)
                    }
                }
            }

            override fun onStartTrackingTouch(sb: SeekBar) {
                isSeeking = true
                uiHandler.removeCallbacks(hideControlsRunnable)
            }

            override fun onStopTrackingTouch(sb: SeekBar) {
                isSeeking = false
                val duration = player?.duration ?: 0L
                if (duration > 0) {
                    val pos = (duration * sb.progress) / 1000L
                    player?.seekTo(pos)
                }
                rescheduleHideControls()
            }
        })

        // Fullscreen toggle
        binding.btnFullscreen.setOnClickListener {
            toggleFullscreen()
            rescheduleHideControls()
        }

        // Sleep Timer
        binding.btnSleepTimer.setOnClickListener {
            showSleepTimerDialog()
            rescheduleHideControls()
        }

        // Popup Play (PiP)
        binding.btnPopupPlay.setOnClickListener {
            enterPictureInPicture()
        }

        // Cast stub
        binding.btnCast.setOnClickListener {
            showCastDialog()
            rescheduleHideControls()
        }

        // Lock
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
        viewModel.isPlaying.observe(viewLifecycleOwner) { isPlaying ->

        }

        viewModel.position.observe(viewLifecycleOwner) { position ->

        }
        collectLatestFlow(viewModel.isFavourite) { value ->
            binding.btnFavourite.setImageResource(if (value) R.drawable.favourited else R.drawable.favourite)
        }
    }

    private fun initPlayer(url: String) {
        player = ExoPlayer.Builder(requireContext()).build().also { exo ->

            binding.playerView.player = exo

            val mediaItem = buildMediaItem(url)
            exo.setMediaItem(mediaItem)
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
                MediaItem.Builder()
                    .setUri(url)
                    .setMimeType(MimeTypes.APPLICATION_M3U8)
                    .build()
            lower.endsWith(".mpd") || lower.contains(".mpd?") ->
                MediaItem.Builder()
                    .setUri(url)
                    .setMimeType(MimeTypes.APPLICATION_MPD)
                    .build()
            lower.endsWith(".mp4") || lower.endsWith(".mkv") ||
                    lower.endsWith(".avi") || lower.endsWith(".mov") ||
                    lower.endsWith(".flv") || lower.endsWith(".wmv") ->
                MediaItem.Builder()
                    .setUri(url)
                    .setMimeType(MimeTypes.VIDEO_MP4)     // ExoPlayer handles all progressive
                    .build()

            lower.startsWith("rtmp://") || lower.startsWith("rtsp://") ->
                MediaItem.fromUri(Uri.parse(url))         // ExoPlayer RTSP built-in

            else ->
                MediaItem.fromUri(Uri.parse(url))         // Auto-detect fallback
        }
    }

    private fun toggleControls() {
        if (binding.controlsOverlay.isVisible) hideControls()
        else showControls()
    }

    private fun showControls() {
        binding.controlsOverlay.visibility = View.VISIBLE
        binding.controlsOverlay.animate().alpha(1f).setDuration(200).start()
        scheduleHideControls()
    }

    private fun hideControls() {
        if (!isAdded || view == null || isDetached) return

        binding.controlsOverlay.animate()
            .alpha(0f)
            .setDuration(300)
            .withEndAction {
                if (isAdded && view != null && !isDetached) {
                    binding.controlsOverlay.visibility = View.GONE
                }
            }
            .start()
    }

    private fun scheduleHideControls() {
        uiHandler.removeCallbacks(hideControlsRunnable)
        uiHandler.postDelayed(hideControlsRunnable, CONTROLS_HIDE_DELAY_MS)
    }

    private fun rescheduleHideControls() {
        uiHandler.removeCallbacks(hideControlsRunnable)
        uiHandler.postDelayed(hideControlsRunnable, CONTROLS_HIDE_DELAY_MS)
    }

    private fun updateLockState() {
        if (isLocked) {
            // Hide all controls except the lock button itself
            val topBar = binding.controlsOverlay.findViewById<LinearLayout>(R.id.topBar)
            binding.btnRewind.visibility = View.GONE
            binding.btnPlayPause.visibility = View.GONE
            binding.btnForward.visibility = View.GONE
            topBar?.visibility = View.GONE
            binding.btnUnlock.visible()
            binding.bottomControl.gone()
        } else {
            val topBar = binding.controlsOverlay.findViewById<LinearLayout>(R.id.topBar)
            binding.btnRewind.visibility = View.VISIBLE
            binding.btnPlayPause.visibility = View.VISIBLE
            binding.btnForward.visibility = View.VISIBLE
            topBar?.visibility = View.VISIBLE
            binding.bottomControl.visible()
            binding.btnUnlock.gone()
        }
    }

    private fun toggleFullscreen() {
        try {
            if (!isAdded || activity == null) return

            if (isFullscreen) {
                exitFullscreen()
            } else {
                enterFullscreen()
            }
        } catch (e: Exception) {
            android.util.Log.e("WatchChannel", "Error toggling fullscreen: ${e.message}")
        }
    }

    private fun handleBackPress() {
        Log.d(TAG_BACK, "handleBackPress called")
        Log.d(TAG_BACK, "isFullscreen=$isFullscreen, isBackInProgress=$isBackInProgress, isExiting=$isExiting")
        Log.d(TAG_BACK, "playlistId=$playlistId, channelId=$channelId")

        if (isFullscreen) {
            Log.d(TAG_BACK, "Currently fullscreen -> exitFullscreen")
            exitFullscreen()
            return
        }

        if (isBackInProgress) {
            Log.d(TAG_BACK, "Back ignored because isBackInProgress=true")
            return
        }

        isBackInProgress = true
        isExiting = true

        Log.d(TAG_BACK, "Set isBackInProgress=true, isExiting=true")

        CoroutineScope(Dispatchers.IO).launch {
            val pid = playlistId
            val cid = channelId

            if (pid == null || cid == null) {
                Log.d(TAG_BACK, "saveFavourite skipped: pid=$pid, cid=$cid")
                return@launch
            }

            Log.d(TAG_BACK, "saveFavourite start: pid=$pid, cid=$cid")

            try {
                viewModel.saveFavourite(pid, cid)
                Log.d(TAG_BACK, "saveFavourite success")
            } catch (e: Exception) {
                Log.e(TAG_BACK, "saveFavourite error: ${e.message}", e)
            }
        }

        Log.d(TAG_BACK, "releasePlayer start")
        releasePlayer()
        Log.d(TAG_BACK, "releasePlayer done")

        Log.d(TAG_BACK, "remove ui/sleep handlers")
        uiHandler.removeCallbacksAndMessages(null)
        sleepTimerUpdateHandler.removeCallbacksAndMessages(null)

        Log.d(TAG_ADS, "showInterstitialAd start: key=${getString(R.string.full_back)}")

        showInterstitialAd(
            getString(R.string.full_back),
            onAdClosed = { isShowed ->
                    popBackStack()
            }
        )
    }

    private companion object {
        private const val TAG_BACK = "BACK_FLOW"
        private const val TAG_ADS = "ADS_CALLBACK"
    }

    private fun exitAndResetOrientation() {
        handleBackPress()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        try {
            if (!isAdded || view == null) return

            applyCurrentOrientationState()

            binding.btnFullscreen.setImageResource(
                if (isFullscreen) R.drawable.ic_fullscren_exit
                else R.drawable.ic_fullscreen
            )
        } catch (e: Exception) {
            android.util.Log.e("WatchChannel", "Error in onConfigurationChanged: ${e.message}")
        }
    }

    private fun hideSystemBars() {
        try {
            if (!isAdded || activity == null) return
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                requireActivity().window.insetsController?.let {
                    it.hide(android.view.WindowInsets.Type.systemBars())
                    it.systemBarsBehavior =
                        WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                }
            } else {
                @Suppress("DEPRECATION")
                requireActivity().window.decorView.systemUiVisibility = (
                        View.SYSTEM_UI_FLAG_FULLSCREEN or
                                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        )
            }
        } catch (e: Exception) {
            android.util.Log.e("WatchChannel", "Error hiding system bars: ${e.message}")
        }
    }

    private fun showSystemBars() {
        try {
            if (!isAdded || activity == null) return
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                requireActivity().window.insetsController?.show(android.view.WindowInsets.Type.systemBars())
            } else {
                @Suppress("DEPRECATION")
                requireActivity().window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
            }
        } catch (e: Exception) {
            android.util.Log.e("WatchChannel", "Error showing system bars: ${e.message}")
        }
    }

    private fun setupImmersive() {
        //test bug
        hideSystemBars()
    }

    private fun enterPictureInPicture() {
        if (!requireActivity().packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)) {
            startOverlayService()
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val params = PictureInPictureParams.Builder()
                .setAspectRatio(Rational(16, 9))
                .build()
            requireActivity().enterPictureInPictureMode(params)
        } else {
            @Suppress("DEPRECATION")
            requireActivity().enterPictureInPictureMode()
        }
    }

    private fun startOverlayService() {
        if (url == null) return
        else {
            val serviceIntent = Intent(requireContext(), FloatingPlayerService::class.java).apply {
                putExtra(EXTRA_URL, url!!)
                putExtra(EXTRA_TITLE, name ?: "")
                putExtra("position", player?.currentPosition ?: 0L)
                putExtra("playing", player?.isPlaying ?: false)
            }
            val ctx = requireContext()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ctx.startForegroundService(serviceIntent)
            } else {
                ctx.startService(serviceIntent)
            }
            releasePlayer()
            requireActivity().moveTaskToBack(true)
        }
    }

    private fun showSleepTimerDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_sleep_timer, null)
        val dialog = android.app.Dialog(requireContext()).apply {
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

        val minuteMap = mapOf(
            R.id.optOff to 0,
            R.id.opt10 to 10,
            R.id.opt30 to 30,
            R.id.opt60 to 60
        )

        minuteMap.forEach { (viewId, minutes) ->
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
        val dialog = android.app.Dialog(requireContext()).apply {
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
                cancelSleepTimer()
                startSleepTimer(minutes * 60L)
                dialog.dismiss()
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
                    requireContext(), getString(R.string.text_end_timer_pause_playback), android.widget.Toast.LENGTH_LONG
                ).show()
            }
        }.start()

        binding.btnSleepTimer.setColorFilter(
            android.graphics.Color.parseColor("#FFC107"), android.graphics.PorterDuff.Mode.SRC_IN
        )
        sleepTimerUpdateHandler.post(sleepTimerUpdateRunnable)

        android.widget.Toast.makeText(
            requireContext(), "${formatCountdown(durationSeconds)}", android.widget.Toast.LENGTH_SHORT
        ).show()
    }

    private fun cancelSleepTimer() {
        sleepTimer?.cancel()
        sleepTimer = null
        sleepTimerEndTime = 0L
        sleepTimerUpdateHandler.removeCallbacks(sleepTimerUpdateRunnable)
        binding.btnSleepTimer.setColorFilter(
            android.graphics.Color.WHITE, android.graphics.PorterDuff.Mode.SRC_IN
        )
    }

    private fun updateSleepTimerIcon() {
        val remaining = getRemainingSeconds()
        if (remaining <= 0) {
            sleepTimerUpdateHandler.removeCallbacks(sleepTimerUpdateRunnable)
            return
        }
        binding.btnSleepTimer.imageAlpha = if (remaining in 1..60 && (remaining % 2L) == 0L) 160 else 255
    }

    private fun getRemainingSeconds(): Long {
        if (sleepTimerEndTime == 0L) return 0L
        return maxOf(0L, (sleepTimerEndTime - System.currentTimeMillis()) / 1000L)
    }

    private fun formatCountdown(totalSeconds: Long): String {
        val h = totalSeconds / 3600
        val m = (totalSeconds % 3600) / 60
        val s = totalSeconds % 60
        return if (h > 0) "%d hr %02d min".format(h, m)
        else if (m > 0) "%d min %02d sec".format(m, s)
        else "%d sec".format(s)
    }

    private fun showCastDialog() {
        val ctx = requireContext()
        val castCtx = castContext ?: run {
            AlertDialog.Builder(ctx, R.style.AlertDialogDark)
                .setTitle(getString(R.string.text_cast_not_available))
                .setMessage(getString(R.string.text_google_play_services_has_not_been_initialized))
                .setPositiveButton(getString(R.string.text_ok), null)
                .show()
            return
        }

        if (castSession != null && castSession!!.isConnected) {
             AlertDialog.Builder(ctx, R.style.AlertDialogDark)
                .setTitle(getString(R.string.text_casting_to_tv))
                .setMessage(getString(R.string.text_what_would_you_like_to_do))
                .setPositiveButton(getString(R.string.text_disconnect)) { _, _ ->
                    castCtx.sessionManager.endCurrentSession(true)
                }
                .setNeutralButton(getString(R.string.text_restart_from_beginning)) { _, _ ->
                    loadMediaOnCast()
                }
                .setNegativeButton(getString(R.string.text_cancel), null)
                .show()
            return
        }

        val selector = androidx.mediarouter.media.MediaRouteSelector.Builder()
            .addControlCategory(
                com.google.android.gms.cast.CastMediaControlIntent.categoryForCast(
                    com.google.android.gms.cast.CastMediaControlIntent.DEFAULT_MEDIA_RECEIVER_APPLICATION_ID
                )
            )
            .build()

        androidx.mediarouter.app.MediaRouteChooserDialogFragment().apply {
            routeSelector = selector
        }.show(childFragmentManager, "MediaRouteChooser")
    }

    private fun forcePortraitMode() {
        try {
            if (!isAdded || isDetached || activity == null || view == null) return
            // Chỉ set nếu chưa ở portrait
            if (requireActivity().requestedOrientation != ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
                requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            }
        } catch (e: Exception) { }
    }

    private fun forceLandscapeMode() {
        try {
            if (!isAdded || isDetached || activity == null || view == null) return
            // Chỉ set nếu chưa ở landscape
            if (requireActivity().requestedOrientation != ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
                requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            }
        } catch (e: Exception) { }
    }

    private fun applyCurrentOrientationState() {
        if (isFullscreen) {
            forceLandscapeMode()
        } else {
            forcePortraitMode()
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


    private fun startProgressUpdater() {
        lifecycleScope.launch {
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

    private fun showSeekFeedback(text: String) {
        binding.tvSeekFeedback.text = text
        binding.tvSeekFeedback.visibility = View.VISIBLE
        binding.tvSeekFeedback.alpha = 1f
        uiHandler.postDelayed({
            binding.tvSeekFeedback.animate()
                .alpha(0f)
                .setDuration(500)
                .withEndAction { binding.tvSeekFeedback.visibility = View.GONE }
                .start()
        }, 700)
    }

    private fun formatTime(ms: Long): String {
        if (ms <= 0) return "00:00"
        val totalSeconds = ms / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return if (hours > 0)
            "%02d:%02d:%02d".format(hours, minutes, seconds)
        else
            "%02d:%02d".format(minutes, seconds)
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode)
        if (isInPictureInPictureMode) {
            binding.controlsOverlay.visibility = View.GONE
        } else {
            showControls()
        }
    }

    private fun initVolumeAndBrightness() {
        audioManager = requireContext().getSystemService(Context.AUDIO_SERVICE) as AudioManager
        maxVolume = audioManager!!.getStreamMaxVolume(AudioManager.STREAM_MUSIC)

        val currentVolume = audioManager!!.getStreamVolume(AudioManager.STREAM_MUSIC)
        val volumePercent = (currentVolume * 100) / maxVolume
        binding.volumePanel.findSeekBar()?.progress = volumePercent

        val currentBrightness = getCurrentBrightness()
        binding.lightPanel.findSeekBar()?.progress = currentBrightness

        binding.volumePanel.findSeekBar()?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    val vol = (progress * maxVolume) / 100
                    audioManager?.setStreamVolume(AudioManager.STREAM_MUSIC, vol, 0)
                    updateMuteIcon(progress)
                }
            }
            override fun onStartTrackingTouch(sb: SeekBar) {}
            override fun onStopTrackingTouch(sb: SeekBar) {}
        })

        // Brightness SeekBar listener
        binding.lightPanel.findSeekBar()?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser) setBrightness(progress)
            }
            override fun onStartTrackingTouch(sb: SeekBar) {}
            override fun onStopTrackingTouch(sb: SeekBar) {}
        })
    }

    private fun getCurrentBrightness(): Int {
        val lp = requireActivity().window.attributes

        if (lp.screenBrightness >= 0f) {
            return (lp.screenBrightness * 100).toInt()
        }

        return try {
            val brightness = Settings.System.getInt(
                requireContext().contentResolver,
                Settings.System.SCREEN_BRIGHTNESS
            )
            (brightness * 100) / 255
        } catch (e: Exception) {
            50
        }
    }

    private fun setBrightness(progress: Int) {
        val brightness = progress.coerceAtLeast(5) / 100f

        val lp = requireActivity().window.attributes
        lp.screenBrightness = brightness
        requireActivity().window.attributes = lp
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

    override fun onResume() {
        super.onResume()

        if (isExiting || isBackInProgress) return

        //test bug
        Log.d("exiting", "onResume: not out")
        applyCurrentOrientationState()

        hideSystemBars()

        sessionManagerListener?.let {
            castContext?.sessionManager?.addSessionManagerListener(it, CastSession::class.java)
        }

        castSession = castContext?.sessionManager?.currentCastSession

        player?.let { exo ->
            val savedPos = viewModel.position.value ?: 0L
            val savedPlaying = viewModel.isPlaying.value ?: true

            when (exo.playbackState) {
                Player.STATE_IDLE, Player.STATE_ENDED -> {
                    exo.prepare()
                    if (savedPos > 0) exo.seekTo(savedPos)
                    if (savedPlaying) exo.play()
                }
                Player.STATE_READY, Player.STATE_BUFFERING -> {
                    if (savedPlaying) exo.play()
                }
            }
        } ?: run {
            if (!isExiting && !isBackInProgress) {  // ← guard kép
                url?.let { initPlayer(it) }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        sessionManagerListener?.let {
            castContext?.sessionManager?.removeSessionManagerListener(it, CastSession::class.java)
        }
        val savedPosition = player?.currentPosition ?: 0L
        val savedIsPlaying = player?.isPlaying ?: false
        viewModel.setPosition(savedPosition)
        viewModel.setPlay(savedIsPlaying)
        if (!requireActivity().isInPictureInPictureMode) {
            player?.pause()
        }
    }

    override fun onStop() {
        super.onStop()
        if (!requireActivity().isInPictureInPictureMode) {
            player?.pause()
        }
    }

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

    override fun onDestroyView() {
        isExiting = true

        uiHandler.removeCallbacksAndMessages(null)
        sleepTimerUpdateHandler.removeCallbacksAndMessages(null)
        sleepTimer?.cancel()
        releasePlayer()

        //test bug
//        if (activity != null && isFullscreen) {
//            requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
//        }

        super.onDestroyView()
    }

    private fun releasePlayer() {
        player?.release()
        player = null
    }

    private fun setupSessionManagerListener() {
        sessionManagerListener = object : SessionManagerListener<CastSession> {
            override fun onSessionStarted(session: CastSession, sessionId: String) {
                castSession = session
                loadMediaOnCast()
                // Pause local player khi cast bắt đầu
                player?.pause()
                updateCastIcon(connected = true)
            }

            override fun onSessionResumed(session: CastSession, wasSuspended: Boolean) {
                castSession = session
                updateCastIcon(connected = true)
            }

            override fun onSessionEnded(session: CastSession, error: Int) {
                castSession = null
                // Resume local player khi cast kết thúc
                player?.play()
                updateCastIcon(connected = false)
            }

            override fun onSessionSuspended(session: CastSession, reason: Int) {}
            override fun onSessionStarting(session: CastSession) {}
            override fun onSessionStartFailed(session: CastSession, error: Int) {
                android.widget.Toast.makeText(
                    requireContext(), getString(R.string.text_failed_to_connect_to_cast_device), android.widget.Toast.LENGTH_SHORT
                ).show()
            }
            override fun onSessionEnding(session: CastSession) {}
            override fun onSessionResuming(session: CastSession, sessionId: String) {}
            override fun onSessionResumeFailed(session: CastSession, error: Int) {}
        }
    }

    private fun loadMediaOnCast() {
        val remoteClient: RemoteMediaClient = castSession?.remoteMediaClient ?: return
        val mediaUrl = url ?: return

        val metadata = MediaMetadata(MediaMetadata.MEDIA_TYPE_MOVIE).apply {
            putString(MediaMetadata.KEY_TITLE, name ?: "Now Playing")
            // Nếu có thumbnail: addImage(WebImage(Uri.parse(thumbnailUrl)))
        }

        val contentType = when {
            mediaUrl.lowercase().contains(".m3u8") -> "application/x-mpegurl"
            mediaUrl.lowercase().endsWith(".mp4")  -> "video/mp4"
            else -> "video/mp4"
        }

        val mediaInfo = MediaInfo.Builder(mediaUrl)
            .setStreamType(MediaInfo.STREAM_TYPE_LIVE)   // Dùng STREAM_TYPE_BUFFERED cho VOD
            .setContentType(contentType)
            .setMetadata(metadata)
            .build()

        val loadRequest = MediaLoadRequestData.Builder()
            .setMediaInfo(mediaInfo)
            .setAutoplay(true)
            .setCurrentTime((player?.currentPosition ?: 0L))
            .build()

        remoteClient.load(loadRequest)
    }

    private fun updateCastIcon(connected: Boolean) {
        binding.btnCast.setColorFilter(
            if (connected) android.graphics.Color.parseColor("#045DCC")
            else android.graphics.Color.WHITE,
            android.graphics.PorterDuff.Mode.SRC_IN
        )
    }

    private fun showErrorDialog() {
        if (!isAdded || activity == null) return
        try {
            ErrorPlayerDialog(
                activity = requireActivity(),
                onReload = {
                    retryCount++
                    url?.let { initPlayer(it) }
                },
                onBack = {
                    handleBackPress()
                }
            ).show()
        } catch (e: Exception) {
            android.util.Log.e("WatchChannel", "Error showing error dialog: ${e.message}")
            // Fallback: just go back
            handleBackPress()
        }
    }
}