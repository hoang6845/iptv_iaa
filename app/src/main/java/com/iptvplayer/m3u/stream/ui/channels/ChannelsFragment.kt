package com.iptvplayer.m3u.stream.ui.channels

import android.app.AlertDialog
import android.app.PictureInPictureParams
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Rational
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsetsController
import android.view.inputmethod.InputMethodManager
import android.widget.ImageButton
import android.widget.SeekBar
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.graphics.toColorInt
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.widget.doOnTextChanged
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import com.airbnb.lottie.LottieAnimationView
import com.google.android.gms.cast.MediaInfo
import com.google.android.gms.cast.MediaLoadRequestData
import com.google.android.gms.cast.MediaMetadata
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.cast.framework.CastSession
import com.google.android.gms.cast.framework.SessionManagerListener
import com.google.android.gms.cast.framework.media.RemoteMediaClient
import com.iptvplayer.m3u.stream.R
import com.iptvplayer.m3u.stream.databinding.FragmentChannelsBinding
import com.iptvplayer.m3u.stream.main.MainActivity
import com.iptvplayer.m3u.stream.model.dao.ChannelPopularDao
import com.iptvplayer.m3u.stream.model.entity.Channel
import com.iptvplayer.m3u.stream.utils.invisible
import com.iptvplayer.m3u.stream.utils.visible
import dagger.hilt.android.AndroidEntryPoint
import hoang.dqm.codebase.base.activity.BaseFragment
import hoang.dqm.codebase.base.activity.onBackPressed
import hoang.dqm.codebase.utils.collectLatestFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@AndroidEntryPoint
class ChannelsFragment : BaseFragment<FragmentChannelsBinding, ChannelsViewModel>() {
    @Inject
    lateinit var channelPopularDao: ChannelPopularDao
    private var restoreNormalUiRunnable: Runnable? = null
    private val categoryTabAdapter: CategoryTabAdapter by lazy { CategoryTabAdapter() }
    private val adapter: ChannelsAdapter by lazy { ChannelsAdapter() }
    private val nowPlayingAdapter: NowPlayingAdapter by lazy { NowPlayingAdapter() }
    private var player: ExoPlayer? = null
    private var currentChannel: Channel? = null
    private var isSeeking = false
    private val uiHandler = Handler(Looper.getMainLooper())
    private val PROGRESS_UPDATE_MS = 500L
    private lateinit var seekBar: SeekBar

    private var castContext: CastContext? = null
    private var castSession: CastSession? = null
    private var sessionManagerListener: SessionManagerListener<CastSession>? = null
    override fun initView() {
        adjustInsetsForBottomNavigation(binding.root)
        try {
            castContext = CastContext.getSharedInstance(requireContext())
            setupSessionManagerListener()
        } catch (e: Exception) {
            android.util.Log.e("CAST", "Init failed: ${e.message}", e)
        }
        setUpAdapter()
        seekBar = binding.playerView.findViewById<SeekBar>(R.id.seekBar)
        adapter.addLoadStateListener { loadState ->
            val isLoading = loadState.refresh is androidx.paging.LoadState.Loading
            val isEmpty = adapter.itemCount == 0

            binding.noContent.visibility = when {
                isLoading -> {
                    View.GONE
                }

                isEmpty -> {
                    View.VISIBLE
                }

                else -> {
                    View.GONE
                }
            }
        }

        // Sử dụng preloaded native ad để hiển thị nhanh hơn
        showPreloadedNativeOrLoad(binding.viewNativeAd, R.string.ads_collapse_channel, updateTimeout = false)
    }

    override fun initListener() {
        onBackPressed {
            if (isFullscreen) {
                toggleFullscreen()
            }
        }
        binding.playerView.findViewById<ImageButton>(R.id.btnFullscreen)?.setOnClickListener {
            toggleFullscreen()
        }

        binding.playerView.findViewById<ImageButton>(R.id.btn_favourite)?.setOnClickListener {
            currentChannel?.let { channel ->
                currentChannel = currentChannel?.copy(isFavourite = !channel.isFavourite)
                viewModel.toggleFavourite(channel)
                updateFavouriteIcon()

            }
        }
        binding.icCurrentFavourite.setOnClickListener {
            currentChannel?.let { channel ->
                currentChannel = currentChannel?.copy(isFavourite = !channel.isFavourite)
                nowPlayingAdapter.setChannel(currentChannel)
                viewModel.toggleFavourite(channel)
                updateFavouriteIcon()
            }
        }

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar, progress: Int, fromUser: Boolean) {
            }

            override fun onStartTrackingTouch(sb: SeekBar) {
                isSeeking = true
            }

            override fun onStopTrackingTouch(sb: SeekBar) {
                isSeeking = false
                val duration = player?.duration ?: 0L
                if (duration > 0) {
                    val pos = (duration * sb.progress) / 1000L
                    player?.seekTo(pos)
                }
            }
        })

        binding.btnSearch.setOnClickListener {
            openSearch()
        }

        binding.edtSearch.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                closeSearch()
            }
        }

        binding.root.setOnClickListener {
            binding.edtSearch.clearFocus()
        }

        binding.edtSearch.doOnTextChanged { text, _, _, _ ->
            viewModel.search(text?.toString() ?: "")
        }
        binding.playerView.findViewById<ImageButton>(R.id.btn_cast)?.setOnClickListener {
            showCastDialog()
        }
    }

    override fun initData() {
        collectLatestFlow(viewModel.listCategoryChannel) { categories ->
            if (categories.isEmpty()) return@collectLatestFlow
            CoroutineScope(Dispatchers.IO).launch {
                val listCateCurrent = channelPopularDao.getAllGroupRaw()
                    .flatMap { it.split(";") }
                    .map { it.trim().lowercase() }
                    .filter { it.isNotEmpty() }
                    .distinct()
                withContext(Dispatchers.Main) {
                    val filtered = categories.filter { (type, value) ->
                        type.lowercase() in listCateCurrent || type.lowercase() == "all"
                    }
                    if (filtered.isEmpty()) return@withContext
                    categoryTabAdapter.setList(filtered)
                    if (filtered.size == 1) {
                        binding.rvCategory.invisible()
                    } else {
                        binding.rvCategory.visible()
                    }
                }
            }


        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {

                launch {
                    viewModel.channels.collect { pagingData ->
                        adapter.submitData(
                            pagingData
                        )
                    }
                }
                launch {
                    adapter.loadStateFlow.collect { loadState ->
                        val isNotLoading = loadState.refresh is androidx.paging.LoadState.NotLoading
                        if (isNotLoading && adapter.itemCount > 0 && currentChannel == null) {
                            val firstChannel = adapter.snapshot().items.firstOrNull()
                            firstChannel?.let { playChannel(it) }
                        }
                    }
                }
            }
        }
    }

    private fun setUpAdapter() {
        categoryTabAdapter.setOnClickItemAdapter { item, position ->
            val isAlreadySelected = categoryTabAdapter.getCategorySelectedPosition() == position
            if (isAlreadySelected) {
            } else {
                categoryTabAdapter.setCategoriesSelected(position)
                viewModel.selectCategory(item)
            }
        }
        binding.rvCategory.adapter = categoryTabAdapter
        binding.rvCategory.layoutManager = LinearLayoutManager(
            requireContext(), LinearLayoutManager.HORIZONTAL, false
        )

        nowPlayingAdapter.setOnClickItemAdapter { item ->
            viewModel.toggleFavourite(item)

            if (item.id == currentChannel?.id) {
                currentChannel = currentChannel?.copy(isFavourite = !item.isFavourite)
                nowPlayingAdapter.setChannel(currentChannel)
                updateFavouriteIcon()
            }
        }

        adapter.setOnClickItemAdapter({ item, position ->
            playChannel(item)
        }) { item ->
            viewModel.toggleFavourite(item)

            if (item.id == currentChannel?.id) {
                currentChannel = currentChannel?.copy(isFavourite = !item.isFavourite)
                nowPlayingAdapter.setChannel(currentChannel) // ← sync icon favourite
                updateFavouriteIcon()
            }
        }
//        binding.channelRecyclerView.adapter = adapter
        binding.channelRecyclerView.adapter = ConcatAdapter(nowPlayingAdapter, adapter)
        binding.channelRecyclerView.layoutManager = LinearLayoutManager(
            requireContext(), LinearLayoutManager.VERTICAL, false
        )
    }


    private fun playChannel(channel: Channel) {
        if (channel.url.isBlank()) return
        viewModel.setCurrentChannel(channel.id)
        currentChannel = channel
        binding.playerView.findViewById<TextView>(R.id.tvTitle).text = channel.name

        if (binding.videoContainer.visibility != View.VISIBLE) {
            binding.videoContainer.apply {
                visibility = View.VISIBLE
                alpha = 0f
                scaleY = 0f
                pivotY = 0f  // scale từ trên xuống
                animate()
                    .alpha(1f)
                    .scaleY(1f)
                    .setDuration(350)
                    .setInterpolator(android.view.animation.DecelerateInterpolator())
                    .start()
            }
        }

        updateFavouriteIcon()
        nowPlayingAdapter.setChannel(channel)
        (binding.channelRecyclerView.layoutManager as? LinearLayoutManager)
            ?.smoothScrollToPosition(binding.channelRecyclerView, null, 0)
        highlightNowPlaying()

        releasePlayer()
        initPlayer(channel.url)

        if (castSession?.isConnected == true) {
            loadMediaOnCast()
            return
        }
    }

    private fun highlightNowPlaying() {
        binding.channelRecyclerView.post {
            val vh = binding.channelRecyclerView.findViewHolderForAdapterPosition(0)
                ?.itemView ?: return@post

            vh.animate().cancel()
            vh.alpha = 0f
            vh.translationY = -30f
            vh.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(350)
                .setInterpolator(android.view.animation.DecelerateInterpolator())
                .start()
        }
    }

    private fun initPlayer(url: String) {
        player = ExoPlayer.Builder(requireContext()).build().also { exo ->
            binding.playerView.player = exo

            val mediaItem = buildMediaItem(url)
            exo.setMediaItem(mediaItem)
            exo.prepare()
            exo.playWhenReady = true

            exo.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(state: Int) {
                    when (state) {
                        Player.STATE_BUFFERING ->
                            binding.playerView.findViewById<LottieAnimationView>(R.id.progress).visibility =
                                View.VISIBLE

                        Player.STATE_READY -> {
                            binding.playerView.findViewById<LottieAnimationView>(R.id.progress).visibility =
                                View.GONE

                            startProgressUpdater()
                        }

                        else ->
                            binding.playerView.findViewById<LottieAnimationView>(R.id.progress).visibility =
                                View.GONE

                    }
                }

                override fun onIsPlayingChanged(playing: Boolean) {
                    // Cập nhật icon play/pause trong custom control nếu cần
                }
            })
        }

        binding.playerView.findViewById<ImageButton>(R.id.btnPicture)?.setOnClickListener {
            enterPictureInPicture()
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
                    .setMimeType(MimeTypes.VIDEO_MP4)
                    .build()

            lower.startsWith("rtmp://") || lower.startsWith("rtsp://") ->
                MediaItem.fromUri(url.toUri())

            else ->
                MediaItem.fromUri(url.toUri())
        }
    }

    private fun startProgressUpdater() {
        lifecycleScope.launch {
            while (true) {
                delay(PROGRESS_UPDATE_MS)
                val exo = player ?: break
                if (!isSeeking && exo.duration > 0) {
                    val pos = exo.currentPosition
                    val dur = exo.duration
                    val prog = ((pos * 1000L) / dur).toInt()
                    seekBar.progress = prog
                }
            }
        }
    }

    private fun enterPictureInPicture() {
        if (!requireActivity().packageManager
                .hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)
        ) return

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

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean) {
        val mainActivity =
            (activity as? MainActivity)?.takeIf { !it.isDestroyed && !it.isFinishing }

        if (isInPictureInPictureMode) {
            binding.bottomChannel.visibility = View.GONE
            hideSystemBars()
            mainActivity?.binding?.bar1?.visibility = View.INVISIBLE
            binding.bottomChannel.visibility = View.GONE
        } else {
            binding.bottomChannel.visibility = View.VISIBLE
            if (!isFullscreen) {
                showSystemBars()
                mainActivity?.binding?.bar1?.visibility = View.INVISIBLE
                binding.bottomChannel.visibility = View.GONE
            }

        }
    }


    override fun onResume() {
        super.onResume()
        player?.play()
    }

    override fun onPause() {
        super.onPause()
        if (!requireActivity().isInPictureInPictureMode) {
            player?.pause()
        }
    }

    override fun onStop() {
        super.onStop()
        if (!requireActivity().isInPictureInPictureMode) {
            player?.pause()
        }
        sessionManagerListener?.let {
            castContext?.sessionManager?.removeSessionManagerListener(it, CastSession::class.java)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        restoreNormalUiRunnable?.let { uiHandler.removeCallbacks(it) }
        uiHandler.removeCallbacksAndMessages(null)
        releasePlayer()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        showSystemBars()
//        val mainActivity = (activity as? MainActivity)?.takeIf { !it.isDestroyed && !it.isFinishing }
//        mainActivity?.binding?.bar1?.visibility = View.VISIBLE
    }

    private fun releasePlayer() {
        player?.release()
        player = null
    }

    private fun openSearch() {
        binding.searchContainer.apply {
            visibility = View.VISIBLE
            alpha = 0f
            translationX = 200f

            animate()
                .alpha(1f)
                .translationX(0f)
                .setDuration(250)
                .start()
        }

        binding.edtSearch.requestFocus()
        showKeyboard(binding.edtSearch)
    }

    private fun closeSearch() {
        binding.searchContainer.animate()
            .alpha(0f)
            .translationX(200f)
            .setDuration(200)
            .withEndAction {
                if (!isAdded || isDetached || view == null) return@withEndAction
                binding.searchContainer.visibility = View.GONE
                hideKeyboard()
            }
            .start()
    }

    private fun showKeyboard(view: View) {
        val imm =
            requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun hideKeyboard() {
        val imm =
            requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(requireActivity().window.decorView.windowToken, 0)
    }

    private var isFullscreen = false

    private fun toggleFullscreen() {
        isFullscreen = !isFullscreen
        val btn = binding.playerView.findViewById<ImageButton>(R.id.btnFullscreen)
        val controlsOverlay =
            binding.playerView.findViewById<ConstraintLayout>(R.id.controlsOverlay)
        val mainActivity =
            (activity as? MainActivity)?.takeIf { !it.isDestroyed && !it.isFinishing }

        if (isFullscreen) {
            requireActivity().requestedOrientation =
                ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            btn?.setImageResource(R.drawable.ic_fullscren_exit)
            hideSystemBars()
            mainActivity?.binding?.bar1?.visibility = View.INVISIBLE
            mainActivity?.binding?.fabAdd1?.visibility = View.INVISIBLE
            binding.bottomChannel.visibility = View.GONE

            ViewCompat.setOnApplyWindowInsetsListener(binding.root, null)
            val params = binding.root.layoutParams as? ViewGroup.MarginLayoutParams
            params?.topMargin = 0
            binding.root.layoutParams = params

            (binding.videoContainer.layoutParams as ConstraintLayout.LayoutParams).apply {
                dimensionRatio = null
                bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
                binding.videoContainer.layoutParams = this
            }
            (controlsOverlay?.layoutParams as? ConstraintLayout.LayoutParams)?.apply {
                dimensionRatio = null
                controlsOverlay.layoutParams = this
            }

        } else {
            // Không dùng UNSPECIFIED ngay, vì máy vẫn có thể đang landscape
            requireActivity().requestedOrientation =
                ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

            btn?.setImageResource(R.drawable.ic_fullscreen)

            // Vẫn ẩn system bar + bottom bar trong lúc xoay lại
            hideSystemBars()
            mainActivity?.binding?.bar1?.visibility = View.INVISIBLE
            mainActivity?.binding?.fabAdd1?.visibility = View.INVISIBLE
            binding.bottomChannel.visibility = View.GONE

            restoreNormalPlayerLayout(controlsOverlay)

            // Chỉ show bottom bar sau khi màn đã portrait thật sự
            waitPortraitThenShowBottomUi()
        }
    }


    private fun hideSystemBars() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val window = requireActivity().window
            window.setDecorFitsSystemWindows(false) // ← quan trọng
            window.insetsController?.let {
                it.hide(android.view.WindowInsets.Type.systemBars())
                it.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            requireActivity().window.decorView.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_FULLSCREEN or
                            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    )
        }
    }

    private fun showSystemBars() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val window = requireActivity().window
            window.setDecorFitsSystemWindows(true)
            window.insetsController?.show(android.view.WindowInsets.Type.systemBars())
        } else {
            @Suppress("DEPRECATION")
            requireActivity().window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
        }
    }

    private fun restoreNormalPlayerLayout(controlsOverlay: ConstraintLayout?) {
        (binding.videoContainer.layoutParams as ConstraintLayout.LayoutParams).apply {
            dimensionRatio = "16:9"
            bottomToBottom = ConstraintLayout.LayoutParams.UNSET
            binding.videoContainer.layoutParams = this
        }

        (controlsOverlay?.layoutParams as? ConstraintLayout.LayoutParams)?.apply {
            dimensionRatio = "16:9"
            controlsOverlay.layoutParams = this
        }
    }

    private fun waitPortraitThenShowBottomUi() {
        restoreNormalUiRunnable?.let { uiHandler.removeCallbacks(it) }

        val mainActivity =
            (activity as? MainActivity)?.takeIf { !it.isDestroyed && !it.isFinishing }

        // Giữ ẩn trong lúc đang xoay từ landscape về portrait
        mainActivity?.binding?.bar1?.visibility = View.INVISIBLE
        mainActivity?.binding?.fabAdd1?.visibility = View.INVISIBLE
        binding.bottomChannel.visibility = View.GONE

        val runnable = object : Runnable {
            override fun run() {
                if (!isAdded || view == null || isFullscreen) return

                val isPortrait =
                    resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT

                if (isPortrait) {
                    showSystemBars()

                    mainActivity?.binding?.bar1?.visibility = View.VISIBLE
                    mainActivity?.binding?.fabAdd1?.visibility = View.VISIBLE
                    binding.bottomChannel.visibility = View.VISIBLE

                    adjustInsetsForBottomNavigation(binding.root)
                    ViewCompat.requestApplyInsets(binding.root)

                    // Nếu muốn sau khi về portrait app lại xoay theo sensor bình thường
                    requireActivity().requestedOrientation =
                        ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                } else {
                    uiHandler.postDelayed(this, 50)
                }
            }
        }

        restoreNormalUiRunnable = runnable
        uiHandler.post(runnable)
    }

    private fun updateFavouriteIcon(channel: Channel? = null) {
        val isFav = viewModel.isFavourite(channel ?: currentChannel)
        binding.playerView.findViewById<ImageButton>(R.id.btn_favourite)
            ?.setImageResource(if (isFav) R.drawable.favourite_open else R.drawable.favourite_open_default)
        binding.icCurrentFavourite.setImageResource(
            if (isFav) {
                R.drawable.favourite_open
            } else {

                R.drawable.favourite_open_default
            }
        )
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
                    requireContext(),
                    getString(R.string.text_failed_to_connect_to_cast_device),
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }

            override fun onSessionEnding(session: CastSession) {}
            override fun onSessionResuming(session: CastSession, sessionId: String) {}
            override fun onSessionResumeFailed(session: CastSession, error: Int) {}
        }
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

    private fun loadMediaOnCast() {
        val remoteClient: RemoteMediaClient = castSession?.remoteMediaClient ?: return
        val channel = currentChannel ?: return          // ← dùng currentChannel
        val mediaUrl = channel.url.takeIf { it.isNotBlank() } ?: return

        val metadata = MediaMetadata(MediaMetadata.MEDIA_TYPE_MOVIE).apply {
            putString(MediaMetadata.KEY_TITLE, channel.name)
        }

        val contentType = when {
            mediaUrl.lowercase().contains(".m3u8") -> "application/x-mpegurl"
            mediaUrl.lowercase().endsWith(".mp4") -> "video/mp4"
            else -> "video/mp4"
        }

        val mediaInfo = MediaInfo.Builder(mediaUrl)
            .setStreamType(MediaInfo.STREAM_TYPE_LIVE)
            .setContentType(contentType)
            .setMetadata(metadata)
            .build()

        val loadRequest = MediaLoadRequestData.Builder()
            .setMediaInfo(mediaInfo)
            .setAutoplay(true)
            .setCurrentTime(player?.currentPosition ?: 0L)
            .build()

        remoteClient.load(loadRequest)
    }

    private fun updateCastIcon(connected: Boolean) {
        binding.playerView.findViewById<ImageButton>(R.id.btn_cast).setColorFilter(
            if (connected) android.graphics.Color.parseColor("#045DCC")
            else android.graphics.Color.WHITE,
            android.graphics.PorterDuff.Mode.SRC_IN
        )
    }

    override fun onStart() {
        super.onStart()
        sessionManagerListener?.let {
            castContext?.sessionManager?.addSessionManagerListener(it, CastSession::class.java)
        }
    }
}