package com.iptvplayer.m3u.stream.ui.home.edit_playlist

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.viewModels
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.iptvplayer.m3u.stream.R
import com.iptvplayer.m3u.stream.databinding.FragmentEditPlaylistBinding
import com.iptvplayer.m3u.stream.model.dao.PlaylistDao
import com.iptvplayer.m3u.stream.model.entity.PlaylistEntity
import com.iptvplayer.m3u.stream.ui.add_playlist.PasscodeDialog
import com.iptvplayer.m3u.stream.utils.AppConstants
import com.iptvplayer.m3u.stream.utils.PasscodeManager
import com.iptvplayer.m3u.stream.utils.PasscodeVerifyDialog
import com.iptvplayer.m3u.stream.utils.gone
import com.iptvplayer.m3u.stream.utils.visible
import dagger.hilt.android.AndroidEntryPoint
import hoang.dqm.codebase.base.activity.BaseBottomSheetFragment
import javax.inject.Inject

@AndroidEntryPoint
class EditPlaylistBottomSheet : BaseBottomSheetFragment<FragmentEditPlaylistBinding>() {
    override fun getTheme(): Int = R.style.FullScreenBottomSheet
    @Inject
    lateinit var passcodeManager: PasscodeManager
    private val viewModel: EditPlaylistViewModel by viewModels()
    @Inject
    lateinit var playlistDao: PlaylistDao

    override fun getVB(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentEditPlaylistBinding {
        _binding = FragmentEditPlaylistBinding.inflate(inflater, container, false)
        return binding
    }

    override fun initView() {
        val playlist = arguments?.getParcelable<PlaylistEntity>("playlist")
        if (playlist?.url == null) {
            binding.btnCopyUrl.gone()
        }
        binding.name.text = playlist?.name

        binding.btnClose.setOnClickListener {
            dismiss()
        }

        binding.btnEdit.setOnClickListener {
            val bundle = Bundle().apply {
                putString("action", "edit")
                putLong("playlistId", playlist?.id?:1)
                putString("currentName", playlist?.name?:"")

            }
            parentFragmentManager.setFragmentResult("edit_playlist_result", bundle)
            dismiss()
        }

        binding.btnCopyUrl.setOnClickListener {
            val clipboard =
                requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("copied_text", playlist?.url))
            Toast.makeText(requireContext(), "Copied", Toast.LENGTH_SHORT).show()

            dismiss()
        }

        binding.delete.setOnClickListener {
            val bundle = Bundle().apply {
                putString("action", "delete")
                putLong("playlistId", playlist?.id?:1)

            }
            parentFragmentManager.setFragmentResult("edit_playlist_result", bundle)
            dismiss()

//            CoroutineScope(Dispatchers.IO).launch {
//                playlist?.let {
//                    playlistDao.deletePlaylist(it)
//                }
//                withContext(Dispatchers.Main) {
//                    dismiss()
//
//                }
//            }
        }

        binding.btnToggleMusic.isChecked = playlist?.isPasscodeEnabled == true

        if (playlist?.typePlayList == AppConstants.TYPE_PLAYLIST_URL || playlist?.typePlayList == AppConstants.TYPE_PLAYLIST_FILE || playlist?.typePlayList == AppConstants.TYPE_PLAYLIST_GALLERY){
            binding.btnPasscode.visible()
            binding.delete.visible()
        } else {
            binding.delete.gone()
            binding.btnPasscode.gone()
        }

        var isUpdating = false

        binding.btnToggleMusic.setOnCheckedChangeListener { _, isChecked ->
            if (isUpdating) return@setOnCheckedChangeListener

            playlist?.let { playlist ->

                if (isChecked) {
                    if (!passcodeManager.hasPasscode()) {
                        showSetPasscodeDialog(
                            onSuccess = {
                                viewModel.togglePasscode(playlist.id, true)
                            },
                            onCancel = {
                                isUpdating = true
                                binding.btnToggleMusic.isChecked = false
                                isUpdating = false
                            }
                        )
                    } else {
                        viewModel.togglePasscode(playlist.id, true)
                    }

                } else {
                    PasscodeVerifyDialog(
                        activity = requireActivity(),
                        passcodeManager = passcodeManager,
                        onSuccess = {
                            viewModel.togglePasscode(playlist.id, false)
                        },
                        onCancel = {
                            isUpdating = true
                            binding.btnToggleMusic.isChecked = true
                            isUpdating = false
                        }
                    ).show()
                }
            }
        }

    }


    private fun setupFullWidth() {
        val dialog = dialog as? BottomSheetDialog ?: return

        val container = dialog.findViewById<ViewGroup>(android.R.id.content)
        container?.setPadding(0, 0, 0, 0)

        val bottomSheet =
            dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
        bottomSheet?.let { sheet ->
            // Xóa padding của tất cả parent views
            var parent = sheet.parent as? View
            while (parent != null) {
                parent.setPadding(0, 0, 0, 0)
                (parent.layoutParams as? ViewGroup.MarginLayoutParams)?.setMargins(0, 0, 0, 0)
                parent = parent.parent as? View
            }

            (sheet.parent as? View)?.setBackgroundColor(Color.TRANSPARENT)

            val layoutParams = sheet.layoutParams
            layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT
            layoutParams.height = (resources.displayMetrics.heightPixels * 0.6).toInt()
            sheet.layoutParams = layoutParams
            sheet.requestLayout()
        }
    }

    override fun onStart() {
        super.onStart()
        val dialog = dialog as? BottomSheetDialog ?: return

        val bottomSheet = dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet) ?: return

//        (bottomSheet.parent as? View)?.setBackgroundColor(Color.TRANSPARENT)

        val layoutParams = bottomSheet.layoutParams
        layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT
        layoutParams.height = (resources.displayMetrics.heightPixels * 0.4).toInt()
        bottomSheet.layoutParams = layoutParams


        val behavior = BottomSheetBehavior.from(bottomSheet)
        behavior.state = BottomSheetBehavior.STATE_COLLAPSED
        behavior.isDraggable = true
        behavior.isHideable = true
        behavior.skipCollapsed = true
        behavior.peekHeight = layoutParams.height
    }

    private fun showSetPasscodeDialog(onSuccess: () -> Unit, onCancel: () -> Unit) {
        PasscodeDialog(
            context = requireContext(),
            onConfirm = { passcode ->
                passcodeManager.savePasscode(passcode)
                onSuccess()
            },
            onCancel = {
                onCancel()
            }
        ).show()
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.FullScreenBottomSheet)
    }
}