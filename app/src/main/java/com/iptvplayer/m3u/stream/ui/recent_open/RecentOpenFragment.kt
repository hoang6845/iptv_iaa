package com.iptvplayer.m3u.stream.ui.recent_open

import android.app.AlertDialog
import android.app.PictureInPictureParams
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.util.Rational
import android.view.View
import android.view.WindowInsetsController
import android.view.inputmethod.InputMethodManager
import android.widget.ImageButton
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
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
import com.iptvplayer.m3u.stream.R
import com.iptvplayer.m3u.stream.databinding.FragmentRecentOpenBinding
import com.iptvplayer.m3u.stream.main.MainActivity
import com.iptvplayer.m3u.stream.model.dao.RecentChannelDao
import com.iptvplayer.m3u.stream.model.entity.FavouriteChannel
import com.iptvplayer.m3u.stream.model.entity.RecentChannel
import com.iptvplayer.m3u.stream.ui.channels.CategoryTabAdapter
import com.iptvplayer.m3u.stream.ui.update_bottom_sheet.DeleteBottomSheet
import com.iptvplayer.m3u.stream.utils.visible
import dagger.hilt.android.AndroidEntryPoint
import hoang.dqm.codebase.base.activity.BaseFragment
import hoang.dqm.codebase.base.activity.onBackPressed
import hoang.dqm.codebase.base.activity.popBackStack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@AndroidEntryPoint
class RecentOpenFragment : BaseFragment<FragmentRecentOpenBinding, RecentOpenViewModel>() {
    @Inject
    lateinit var recentChannelDao: RecentChannelDao
    private val adapter: RecentChannelAdapter by lazy { RecentChannelAdapter() }
    private val categoryTabAdapter: CategoryTabAdapter by lazy { CategoryTabAdapter() }

    private val nowPlayingAdapter: RecentNowPlayingAdapter by lazy { RecentNowPlayingAdapter() }

    private var player: ExoPlayer? = null
    private var currentChannel: RecentChannel? = null
    private var isSeeking = false
    private val uiHandler = Handler(Looper.getMainLooper())
    private val PROGRESS_UPDATE_MS = 500L
    private lateinit var seekBar: SeekBar
    private var isFullscreen = false
    private var castContext: CastContext? = null
    private var castSession: CastSession? = null
    private var sessionManagerListener: SessionManagerListener<CastSession>? = null
    private var favouriteList: List<FavouriteChannel> = emptyList()

    override fun initView() {
        adjustInsetsForBottomNavigation(binding.root)
        try {
            castContext = CastContext.getSharedInstance(requireContext())
            setupSessionManagerListener()
        } catch (e: Exception) {
            Log.e("CAST", "Init failed: ${e.message}", e)
        }
        setUpAdapter()
        seekBar = binding.playerView.findViewById(R.id.seekBar)
    }

    override fun initListener() {
        onBackPressed {
            if (isFullscreen) toggleFullscreen()
            else {
                popBackStack()
            }
        }

        binding.playerView.findViewById<ImageButton>(R.id.btnBack)?.visible()

        binding.playerView.findViewById<ImageButton>(R.id.btnBack)?.setOnClickListener {
            popBackStack()
        }

        binding.playerView.findViewById<ImageButton>(R.id.btnFullscreen)?.setOnClickListener {
            toggleFullscreen()
        }

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar, progress: Int, fromUser: Boolean) {}
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

        binding.btnSearch.setOnClickListener { openSearch() }

        binding.edtSearch.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) closeSearch()
        }

        binding.root.setOnClickListener {
            binding.edtSearch.clearFocus()
        }

        binding.edtSearch.doOnTextChanged { text, _, _, _ ->
            viewModel.search(text?.toString() ?: "")
        }

        binding.btnFavourite1.setOnClickListener {
            currentChannel?.let { viewModel.toggleFavourite(it) }
        }
    }

    override fun initData() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.favouriteChannels.collect { favList->
                        favouriteList = favList
                        adapter.listFavouriteChannel = favList
                        adapter.notifyDataSetChanged()
                        currentChannel?.let { nowPlayingAdapter.setChannel(it, favList) }
                        val isFav = favList.any {
                            it.id == currentChannel?.channelId && it.playlistId == currentChannel?.playlistId
                        }
                        binding.btnFavourite1.setImageResource(
                            if (isFav) R.drawable.favourite_open
                            else R.drawable.favourite_open_default
                        )
                    }
                }

                launch {
                    viewModel.recentChannels.collect {list ->
                        adapter.setList(list)
                        nowPlayingAdapter.setChannel(currentChannel, favouriteList)

                        if (list.isNotEmpty() && currentChannel == null) {
                            playChannel(list.first())
                        }
                    }
                }

                launch {
                    viewModel.listCategoryChannel.collect { categories ->
                        if (categories.isEmpty()) return@collect
                        CoroutineScope(Dispatchers.IO).launch{
                            val listCateCurrent = recentChannelDao.getAllGroupRaw()
                                .flatMap { it.split(";") }
                                .map { it.trim().lowercase() }
                                .filter { it.isNotEmpty() }
                                .distinct()
                            withContext(Dispatchers.Main){
                                val filtered = categories.filter { (type, value) ->
                                    type.lowercase() in listCateCurrent || type.lowercase() == "all"
                                }
                                if (filtered.isEmpty()) return@withContext
                                categoryTabAdapter.setList(filtered)
                                Log.d("check all cate", "initData: $filtered")

                            }
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

        adapter.setOnClickListener(
            { item, _ -> playChannel(item) },
            { item, _ ->
                viewModel.toggleFavourite(item)
            },
            { item, _ ->
                val bottomSheet = DeleteBottomSheet()
                bottomSheet.show(childFragmentManager, "delete_Bottom_Sheet")

                childFragmentManager.setFragmentResultListener(
                    "delete_bottom_sheet",
                    viewLifecycleOwner
                ) { _, bundle ->
                    val action = bundle.getString("action")
                    if (action == "delete") {
                        viewModel.removeRecent(item)
                    }
                }
            }
        )

        nowPlayingAdapter.setOnFavouriteClick { item ->
            viewModel.toggleFavourite(item)
        }

        binding.channelRecyclerView.adapter = ConcatAdapter(nowPlayingAdapter, adapter)
        binding.channelRecyclerView.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
    }

    private fun playChannel(channel: RecentChannel) {
        if (channel.url.isBlank()) return

        currentChannel = channel
        viewModel.setCurrentChannel(channel.channelId)

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
        binding.playerView.findViewById<TextView>(R.id.tvTitle)?.text = channel.name

        nowPlayingAdapter.setChannel(channel, favouriteList)

        binding.channelRecyclerView.post {
            (binding.channelRecyclerView.layoutManager as? LinearLayoutManager)
                ?.scrollToPositionWithOffset(0, 0)

            binding.channelRecyclerView.post {
                highlightNowPlaying()
            }
        }

        viewModel.addRecent(channel.copy(lastWatchTime = System.currentTimeMillis()))
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
            exo.setMediaItem(buildMediaItem(url))
            exo.prepare()
            exo.playWhenReady = true

            exo.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(state: Int) {
                    val lottie = binding.playerView
                        .findViewById<LottieAnimationView>(R.id.progress)
                    when (state) {
                        Player.STATE_BUFFERING -> lottie?.visibility = View.VISIBLE
                        Player.STATE_READY -> {
                            lottie?.visibility = View.GONE
                            startProgressUpdater()
                        }

                        else -> lottie?.visibility = View.GONE
                    }
                }
            })
        }

        binding.playerView.findViewById<ImageButton>(R.id.btnPicture)?.setOnClickListener {
            enterPictureInPicture()
        }
        binding.playerView.findViewById<ImageButton>(R.id.btn_cast)?.setOnClickListener {
            showCastDialog()
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

            else -> MediaItem.fromUri(url)
        }
    }

    private fun startProgressUpdater() {
        lifecycleScope.launch {
            while (true) {
                delay(PROGRESS_UPDATE_MS)
                val exo = player ?: break
                if (!isSeeking && exo.duration > 0) {
                    val pos = exo.currentPosition
                    val prog = ((pos * 1000L) / exo.duration).toInt()
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
            requireActivity().enterPictureInPictureMode(
                PictureInPictureParams.Builder().setAspectRatio(Rational(16, 9)).build()
            )
        } else {
            @Suppress("DEPRECATION")
            requireActivity().enterPictureInPictureMode()
        }
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean) {
        val mainActivity = (activity as? MainActivity)
            ?.takeIf { !it.isDestroyed && !it.isFinishing }
        if (isInPictureInPictureMode) {
            binding.bottomChannel.visibility = View.GONE
            hideSystemBars()
            mainActivity?.binding?.bar1?.visibility = View.INVISIBLE
        } else {
            binding.bottomChannel.visibility = View.VISIBLE
            if (!isFullscreen) {
                showSystemBars()
                mainActivity?.binding?.bar1?.visibility = View.VISIBLE
            }
        }
    }

    override fun onResume() {
        super.onResume(); player?.play()
    }

    override fun onPause() {
        super.onPause()
        if (!requireActivity().isInPictureInPictureMode) player?.pause()
    }

    override fun onStop() {
        super.onStop()
        if (!requireActivity().isInPictureInPictureMode) player?.pause()
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
        requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        showSystemBars()
    }

    private fun releasePlayer() {
        player?.release(); player = null
    }

    // ── Search ──────────────────────────────────────────────────────────────

    private fun openSearch() {
        binding.searchContainer.apply {
            visibility = View.VISIBLE; alpha = 0f; translationX = 200f
            animate().alpha(1f).translationX(0f).setDuration(250).start()
        }
        binding.edtSearch.requestFocus()
        showKeyboard(binding.edtSearch)
    }

    private fun closeSearch() {
        binding.searchContainer.animate()
            .alpha(0f).translationX(200f).setDuration(200)
            .withEndAction {
                if (!isAdded || isDetached || view == null) return@withEndAction
                binding.searchContainer.visibility = View.GONE
                hideKeyboard()
            }.start()
    }

    private fun showKeyboard(view: View) {
        (requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
            .showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun hideKeyboard() {
        (requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
            .hideSoftInputFromWindow(requireActivity().window.decorView.windowToken, 0)
    }

    // ── Fullscreen ──────────────────────────────────────────────────────────

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
            (binding.videoContainer.layoutParams as ConstraintLayout.LayoutParams).apply {
                dimensionRatio = null
                bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
                binding.videoContainer.layoutParams = this
            }
            (controlsOverlay?.layoutParams as? ConstraintLayout.LayoutParams)?.apply {
                dimensionRatio = null; controlsOverlay.layoutParams = this
            }
        } else {
            requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            btn?.setImageResource(R.drawable.ic_fullscreen)
            showSystemBars()
            mainActivity?.binding?.bar1?.visibility = View.VISIBLE
            binding.bottomChannel.visibility = View.VISIBLE
            (binding.videoContainer.layoutParams as ConstraintLayout.LayoutParams).apply {
                dimensionRatio = "16:9"
                bottomToBottom = ConstraintLayout.LayoutParams.UNSET
                binding.videoContainer.layoutParams = this
            }
            (controlsOverlay?.layoutParams as? ConstraintLayout.LayoutParams)?.apply {
                dimensionRatio = "16:9"; controlsOverlay.layoutParams = this
            }
        }
    }

    private fun hideSystemBars() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            requireActivity().window.apply {
                setDecorFitsSystemWindows(false)
                insetsController?.let {
                    it.hide(android.view.WindowInsets.Type.systemBars())
                    it.systemBarsBehavior =
                        WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                }
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

    private fun setupSessionManagerListener() {
        sessionManagerListener = object : SessionManagerListener<CastSession> {
            override fun onSessionStarted(session: CastSession, sessionId: String) {
                castSession = session
                loadMediaOnCast()
                player?.pause()
                updateCastIcon(connected = true)
            }
            override fun onSessionResumed(session: CastSession, wasSuspended: Boolean) {
                castSession = session
                updateCastIcon(connected = true)
            }
            override fun onSessionEnded(session: CastSession, error: Int) {
                castSession = null
                player?.play()
                updateCastIcon(connected = false)
            }
            override fun onSessionSuspended(session: CastSession, reason: Int) {}
            override fun onSessionStarting(session: CastSession) {}
            override fun onSessionStartFailed(session: CastSession, error: Int) {
                android.widget.Toast.makeText(requireContext(), "Failed to connect cast", Toast.LENGTH_SHORT).show()
            }
            override fun onSessionEnding(session: CastSession) {}
            override fun onSessionResuming(session: CastSession, sessionId: String) {}
            override fun onSessionResumeFailed(session: CastSession, error: Int) {}
        }
    }

    private fun showCastDialog() {
        val ctx = requireContext()
        val castCtx = castContext ?: run {
            AlertDialog.Builder(ctx)
                .setTitle("Cast not available")
                .setMessage("Google Play Services not initialized")
                .setPositiveButton("OK", null).show()
            return
        }

        if (castSession?.isConnected == true) {
            AlertDialog.Builder(ctx)
                .setTitle("Casting to TV")
                .setMessage("What would you like to do?")
                .setPositiveButton("Disconnect") { _, _ -> castCtx.sessionManager.endCurrentSession(true) }
                .setNeutralButton("Restart") { _, _ -> loadMediaOnCast() }
                .setNegativeButton("Cancel", null).show()
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
        }.show(childFragmentManager, "MediaRouteChooser")
    }

    private fun loadMediaOnCast() {
        val remoteClient = castSession?.remoteMediaClient ?: return
        val channel = currentChannel ?: return
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
        binding.playerView.findViewById<ImageButton>(R.id.btn_cast)?.setColorFilter(
            if (connected) android.graphics.Color.parseColor("#045DCC")
            else android.graphics.Color.WHITE,
            android.graphics.PorterDuff.Mode.SRC_IN
        )
    }

    private fun showSystemBars() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            requireActivity().window.insetsController?.show(android.view.WindowInsets.Type.systemBars())
        } else {
            @Suppress("DEPRECATION")
            requireActivity().window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
        }
    }

    override fun onStart() {
        super.onStart()
        sessionManagerListener?.let {
            castContext?.sessionManager?.addSessionManagerListener(it, CastSession::class.java)
        }
    }
}