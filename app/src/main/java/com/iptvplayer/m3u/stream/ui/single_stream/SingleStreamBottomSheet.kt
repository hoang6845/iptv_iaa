package com.iptvplayer.m3u.stream.ui.single_stream

import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.graphics.toColorInt
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.iptvplayer.m3u.stream.R
import com.iptvplayer.m3u.stream.databinding.FragmentSingleStreamSheetBinding
import hoang.dqm.codebase.base.activity.BaseBottomSheetFragment
import hoang.dqm.codebase.base.activity.navigate
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

class SingleStreamBottomSheet : BaseBottomSheetFragment<FragmentSingleStreamSheetBinding>() {

    override fun getTheme(): Int = R.style.FullScreenBottomSheet

    override fun getVB(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentSingleStreamSheetBinding {
        _binding = FragmentSingleStreamSheetBinding.inflate(inflater, container, false)
        return binding
    }

    override fun initView() {
        binding.text.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if ((s?.length ?: 0) > 0) {
                    binding.btnAdd.setBackgroundResource(R.drawable.bg_btn)
                    binding.btnAdd.setTextColor(Color.WHITE)
                } else {
                    binding.btnAdd.setBackgroundResource(R.drawable.bg_btn_disable)
                    binding.btnAdd.setTextColor("#99A1AF".toColorInt())
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        binding.btnAdd.setOnClickListener {
            val url = binding.text.text.toString().trim()

            if (!isSupportedStream(url)) {
                binding.text.error = "Url không hợp lệ"
                return@setOnClickListener
            }

            // ✅ Show loading khi đang check
            binding.btnAdd.isEnabled = false
            binding.btnAdd.text = getString(R.string.text_checking_url)

            lifecycleScope.launch {
                val ok = checkUrlAliveWithExoPlayer(url)

                // ✅ Reset button
                binding.btnAdd.isEnabled = true
                binding.btnAdd.text = getString(R.string.text_play)

                if (ok) {
                    val bundle = Bundle().apply {
                        putString("url", url)
                    }
                    showInterstitialAd {
                        navigate(R.id.watchChannelFragment, bundle)
                        dismiss()
                    }
                } else {
                    binding.text.error = getString(R.string.text_url_error)
                }
            }
        }

        binding.btnClose.setOnClickListener {
            dismiss()
        }
    }

    private suspend fun checkUrlAliveWithExoPlayer(url: String): Boolean {
        return withTimeoutOrNull(10_000L) {
            suspendCancellableCoroutine { cont ->
                val player = ExoPlayer.Builder(requireContext()).build()

                val listener = object : Player.Listener {
                    override fun onPlaybackStateChanged(state: Int) {
                        when (state) {
                            Player.STATE_READY -> {
                                Log.d("ExoCheck", "✅ READY")
                                player.release()
                                if (cont.isActive) cont.resume(true)
                            }
                            Player.STATE_ENDED -> {
                                Log.d("ExoCheck", "⚠️ ENDED")
                                player.release()
                                if (cont.isActive) cont.resume(false)
                            }
                            else -> {}
                        }
                    }

                    override fun onPlayerError(error: PlaybackException) {
                        Log.d("ExoCheck", "❌ Error: ${error.message} | Cause: ${error.cause?.message}")
                        player.release()
                        if (cont.isActive) cont.resume(false)
                    }
                }

                // ✅ Dùng đúng buildMediaItem như WatchChannelFragment
                player.addListener(listener)
                player.setMediaItem(buildMediaItem(url))
                player.playWhenReady = false
                player.prepare()

                cont.invokeOnCancellation {
                    player.release()
                }
            }
        } ?: false
    }

    // ✅ Copy y chang từ WatchChannelFragment
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
                MediaItem.fromUri(Uri.parse(url))

            else ->
                MediaItem.fromUri(Uri.parse(url))
        }
    }

    private fun isSupportedStream(url: String): Boolean {
        if (url.isBlank()) return false
        val lower = url.lowercase()
        return lower.endsWith(".m3u8") ||
                lower.contains(".m3u8?") ||
                lower.endsWith(".mp4") ||
                lower.endsWith(".mkv") ||
                lower.endsWith(".avi") ||
                lower.endsWith(".mov") ||
                lower.endsWith(".flv") ||
                lower.endsWith(".wmv") ||
                lower.startsWith("rtmp://") ||
                lower.startsWith("rtsp://")
    }

    override fun onStart() {
        super.onStart()
        val dialog = dialog as? BottomSheetDialog ?: return
        val bottomSheet = dialog.findViewById<View>(
            com.google.android.material.R.id.design_bottom_sheet
        ) ?: return

        val layoutParams = bottomSheet.layoutParams
        layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT
        layoutParams.height = (resources.displayMetrics.heightPixels * 0.75).toInt()
        bottomSheet.layoutParams = layoutParams

        val behavior = BottomSheetBehavior.from(bottomSheet)
        behavior.state = BottomSheetBehavior.STATE_COLLAPSED
        behavior.isDraggable = true
        behavior.isHideable = true
        behavior.skipCollapsed = true
        behavior.peekHeight = layoutParams.height
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.FullScreenBottomSheet)
    }
}