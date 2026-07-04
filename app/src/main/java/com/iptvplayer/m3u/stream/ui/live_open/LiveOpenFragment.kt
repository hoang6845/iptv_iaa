package com.iptvplayer.m3u.stream.ui.live_open

import android.app.AlertDialog
import android.app.PictureInPictureParams
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Rational
import android.view.View
import android.view.WindowInsetsController
import android.view.inputmethod.InputMethodManager
import android.widget.ImageButton
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.net.toUri
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
import com.iptvplayer.m3u.stream.databinding.FragmentLiveOpenBinding
import com.iptvplayer.m3u.stream.main.MainActivity
import com.iptvplayer.m3u.stream.model.entity.LiveXtream
import com.iptvplayer.m3u.stream.ui.update_bottom_sheet.UpdateBottomSheet
import dagger.hilt.android.AndroidEntryPoint
import hoang.dqm.codebase.base.activity.BaseFragment
import hoang.dqm.codebase.base.activity.onBackPressed
import hoang.dqm.codebase.base.activity.popBackStack
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@AndroidEntryPoint
class LiveOpenFragment : BaseFragment<FragmentLiveOpenBinding, LiveOpenViewModel>() {
    private val serverId: Int? by lazy {
        arguments?.getInt("serverId")
    }
    private val categoryTabAdapter: CategoryTabAdapter by lazy { CategoryTabAdapter() }

    private val adapter: LiveOpenAdapter by lazy { LiveOpenAdapter() }
    private val nowPlayingAdapter: NowPlayingAdapter by lazy { NowPlayingAdapter() }
    private var player: ExoPlayer? = null
    private var currentLiveXtream: LiveXtream? = null
    private var isSeeking = false
    private val uiHandler = Handler(Looper.getMainLooper())
    private val PROGRESS_UPDATE_MS = 500L
    private lateinit var seekBar: SeekBar

    private var castContext: CastContext? = null
    private var castSession: CastSession? = null
    private val username: String? by lazy {
        arguments?.getString("username")
    }

    private val password: String? by lazy {
        arguments?.getString("password")
    }

    private val server: String? by lazy {
        arguments?.getString("server")
    }

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
        setupCategoryRecyclerView()
        seekBar = binding.playerView.findViewById<SeekBar>(R.id.seekBar)
        adapter.addLoadStateListener { loadState ->
            binding.progressLoading.visibility =
                if (adapter.itemCount == 0)
                    View.VISIBLE
                else {
                    View.GONE
                }
        }
    }

    override fun initListener() {
        onBackPressed {
            if (isFullscreen) {
                toggleFullscreen()
            } else {
                popBackStack()
            }
        }
        binding.playerView.findViewById<ImageButton>(R.id.btnFullscreen)?.setOnClickListener {
            toggleFullscreen()
        }

        binding.btnSetting.setOnClickListener {
            val bottomSheet = UpdateBottomSheet()
            bottomSheet.show(childFragmentManager, "BOTTOM_SHEET_UPDATE")
        }

        childFragmentManager.setFragmentResultListener(
            "update_bottom_sheet",
            viewLifecycleOwner
        ) { _, bundle ->
            val action = bundle.getString("action")
            if (action == "update") {
                serverId?.let { viewModel.updateLiveXtream(it) }
            }
        }

        binding.playerView.setOnClickListener {

        }

//        binding.btnSort.setOnClickListener {
//            SortBottomSheet(viewModel.getSort()).show(childFragmentManager, "sort")
//        }
//
//        childFragmentManager.setFragmentResultListener("sort_bottom_sheet", viewLifecycleOwner) { _, bundle ->
//            val action = bundle.getString("action") ?: "none"
//            viewModel.setSort(action)
//        }

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
        viewModel.setPrefs(username, password,server)
        serverId?.let {
            viewModel.loadCategories(serverId = it)
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
                    viewModel.categories.collect { categories ->
                        categoryTabAdapter.setList(categories)
                        if (categories.isNotEmpty()) {
                            categoryTabAdapter.setCategoriesSelected(0)
                        }
                    }
                }
                launch {
                    viewModel.updateState.collect { state ->
                        when (state) {
                            is LiveXtreamUpdateState.Idle -> {
                                binding.layoutLoading.visibility = View.GONE
                            }
                            is LiveXtreamUpdateState.Loading -> {
                                binding.layoutLoading.visibility = View.VISIBLE
                                binding.tvProgress.text = "${state.progress}%"
                                binding.tvLoadingLabel.text = when {
                                    state.progress <= 10 -> getString(R.string.text_connecting)
                                    state.progress <= 30 -> getString(R.string.text_loading_current_data)
                                    state.progress <= 60 -> getString(R.string.text_downloading)
                                    state.progress <= 80 -> getString(R.string.text_merging_channels)
                                    else -> getString(R.string.text_saving)
                                }
                            }
                            is LiveXtreamUpdateState.Success -> {
                                binding.layoutLoading.visibility = View.GONE
                                Toast.makeText(requireContext(), "Channels updated!", Toast.LENGTH_SHORT).show()
                                viewModel.resetUpdateState()
                            }
                            is LiveXtreamUpdateState.Error -> {
                                binding.layoutLoading.visibility = View.GONE
                                Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
                                viewModel.resetUpdateState()
                            }
                        }
                    }
                }


            }
        }
    }

    private fun setUpAdapter() {
        adapter.setOnClickItemAdapter({ item, position ->
            playLiveXtream(item)
        })
//        binding.channelRecyclerView.adapter = adapter
        binding.channelRecyclerView.adapter = ConcatAdapter(nowPlayingAdapter, adapter)
        binding.channelRecyclerView.layoutManager = LinearLayoutManager(
            requireContext(), LinearLayoutManager.VERTICAL, false
        )
    }

    private fun setupCategoryRecyclerView() {
        binding.rvCategory.apply {
            adapter = categoryTabAdapter
            layoutManager = LinearLayoutManager(
                requireContext(), LinearLayoutManager.HORIZONTAL, false
            )
        }

        categoryTabAdapter.setOnClickItemAdapter { item, position ->
            categoryTabAdapter.setCategoriesSelected(position)
            viewModel.selectCategory(position)
            // Scroll list kênh về đầu khi đổi category
            binding.channelRecyclerView.scrollToPosition(0)
        }
    }


    private fun playLiveXtream(liveXtream: LiveXtream) {
        viewModel.setLiveXtreamId(liveXtream.streamId)
        currentLiveXtream = liveXtream
        binding.playerView.findViewById<TextView>(R.id.tvTitle).text = liveXtream.name
        binding.videoContainer.visibility = View.VISIBLE
        nowPlayingAdapter.setLiveXtream(liveXtream)
        (binding.channelRecyclerView.layoutManager as? LinearLayoutManager)
            ?.smoothScrollToPosition(binding.channelRecyclerView, null, 0)
        highlightNowPlaying()

        releasePlayer()

        if (castSession?.isConnected == true) {
            loadMediaOnCast()
            return
        }

        initPlayer(liveXtream.streamId)
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

    private fun initPlayer(idLive: Int) {
        val url = viewModel.getFullUrl(idLive)
        if (url.isBlank()) return
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
            binding.bottomChannel.visibility = View.GONE

            // Video container fill toàn màn hình
            (binding.videoContainer.layoutParams as ConstraintLayout.LayoutParams).apply {
                dimensionRatio = null
                bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
                binding.videoContainer.layoutParams = this
            }

            // Controls overlay fill toàn bộ PlayerView
            (controlsOverlay?.layoutParams as? ConstraintLayout.LayoutParams)?.apply {
                dimensionRatio = null
                controlsOverlay.layoutParams = this
            }

        } else {
            requireActivity().requestedOrientation =
                ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            btn?.setImageResource(R.drawable.ic_fullscreen)
            showSystemBars()
            mainActivity?.binding?.bar1?.visibility = View.VISIBLE
            binding.bottomChannel.visibility = View.VISIBLE

            // Khôi phục video container
            (binding.videoContainer.layoutParams as ConstraintLayout.LayoutParams).apply {
                dimensionRatio = "16:9"
                bottomToBottom = ConstraintLayout.LayoutParams.UNSET
                binding.videoContainer.layoutParams = this
            }

            // Khôi phục controls overlay
            (controlsOverlay?.layoutParams as? ConstraintLayout.LayoutParams)?.apply {
                dimensionRatio = "16:9"
                controlsOverlay.layoutParams = this
            }
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
            requireActivity().window.insetsController?.show(android.view.WindowInsets.Type.systemBars())
        } else {
            @Suppress("DEPRECATION")
            requireActivity().window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
        }
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
        val liveXtream = currentLiveXtream ?: return
        val mediaUrl = viewModel.getFullUrl(liveXtream.streamId).takeIf { it.isNotBlank() } ?: return

        val metadata = MediaMetadata(MediaMetadata.MEDIA_TYPE_MOVIE).apply {
            putString(MediaMetadata.KEY_TITLE, liveXtream.name)
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