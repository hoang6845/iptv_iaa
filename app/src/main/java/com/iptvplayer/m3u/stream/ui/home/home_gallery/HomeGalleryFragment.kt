package com.iptvplayer.m3u.stream.ui.home.home_gallery

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.textfield.TextInputEditText
import com.iptvplayer.m3u.stream.R
import com.iptvplayer.m3u.stream.databinding.FragmentHomeGalleryBinding
import com.iptvplayer.m3u.stream.ui.home.HomeViewModel
import com.iptvplayer.m3u.stream.ui.home.PlaylistAdapter
import com.iptvplayer.m3u.stream.ui.home.edit_playlist.EditPlaylistBottomSheet
import com.iptvplayer.m3u.stream.utils.AppConstants
import com.iptvplayer.m3u.stream.utils.DeletePlaylistDialog
import com.iptvplayer.m3u.stream.utils.EditNameDialog
import com.iptvplayer.m3u.stream.utils.PasscodeManager
import com.iptvplayer.m3u.stream.utils.PasscodeVerifyDialog
import com.iptvplayer.m3u.stream.utils.gone
import com.iptvplayer.m3u.stream.utils.visible
import dagger.hilt.android.AndroidEntryPoint
import hoang.dqm.codebase.base.activity.BaseFragment
import hoang.dqm.codebase.base.activity.navigate
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.getValue

@AndroidEntryPoint
class HomeGalleryFragment : BaseFragment<FragmentHomeGalleryBinding, HomeGalleryViewModel>() {
    @Inject
    lateinit var passcodeManager: PasscodeManager
    private val homeViewModel: HomeViewModel by activityViewModels()
    private val adapter: PlaylistAdapter by lazy {
        PlaylistAdapter()
    }

    override fun initView() {
        setUpAdapter()
    }

    override fun initListener() {
    }

    override fun initData() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                homeViewModel.playlists.collect { list ->
                    val newList =
                        list.filter { it.playlist.typePlayList == AppConstants.TYPE_PLAYLIST_GALLERY }
                    if (newList.isEmpty()){
                        binding.noContent.visible()
                        binding.rvPlaylist.gone()
                    }else{
                        binding.noContent.gone()
                        binding.rvPlaylist.visible()
                        adapter.setList(newList)
                    }
                }
            }
        }
    }

    fun setUpAdapter() {
        adapter.setOnClickItemAdapter({ item, position ->
            if (item.playlist.typePlayList == AppConstants.TYPE_PLAYLIST_POPULAR_CHANNEL) {
                navigate(R.id.categoryChannelFragment)
            } else {
                if (item.playlist.isPasscodeEnabled && passcodeManager.hasPasscode()) {
                    PasscodeVerifyDialog(
                        activity = requireActivity(),
                        passcodeManager = passcodeManager,
                        onSuccess = {
                            homeViewModel.setPlaylistSelected(item)
                            navigate(R.id.categoryChannelFragment)
                        },
                        onCancel = { }
                    ).show()
                } else {
                    homeViewModel.setPlaylistSelected(item)
                    navigate(R.id.categoryChannelFragment)
                }
            }
        }) { item, position ->
            val bottomSheet = EditPlaylistBottomSheet()
            val bundle = Bundle().apply {
                putParcelable("playlist", item)
            }
            bottomSheet.arguments = bundle
            bottomSheet.show(childFragmentManager, "BOTTOM_SHEET_EDIT")
        }

        binding.rvPlaylist.adapter = adapter
        binding.rvPlaylist.layoutManager = LinearLayoutManager(
            requireContext(),
            LinearLayoutManager.VERTICAL, false
        )

        childFragmentManager.setFragmentResultListener(
            "edit_playlist_result",
            viewLifecycleOwner
        ) { requestKey, bundle ->

            val action = bundle.getString("action")
            val id = bundle.getLong("playlistId")
            val name = bundle.getString("currentName")
            if (action == "edit") {
//                showDialogRename(id)
                EditNameDialog(
                    activity = requireActivity(),
                    currentName = name?:"",
                    onConfirm = { newName ->
                        viewModel.updateName(id, newName)
                    }
                ).show()
            }else if (action == "delete") {
                DeletePlaylistDialog(
                    activity = requireActivity(),
                    onConfirm = {
                        PasscodeVerifyDialog(
                            activity = requireActivity(),
                            passcodeManager = passcodeManager,
                            onSuccess = {
                                viewModel.deletePlaylist(id)
                            },
                            onCancel = { }
                        ).show()
                    }
                ).show()
            }
        }

    }

    fun showDialogRename(id: Long) {
        if (!isAdded || isDetached || view == null) return
        context?.let { ctx ->
            val dialogView = layoutInflater.inflate(R.layout.dialog_rename, null)
            val dialog = AlertDialog.Builder(ctx).setView(dialogView).setCancelable(true).create()

            val btnCancel = dialogView.findViewById<TextView>(R.id.btn_cancel)
            val btnConfirm = dialogView.findViewById<TextView>(R.id.btn_confirm)
            val newName = dialogView.findViewById<TextInputEditText>(R.id.edt_playlist_name)

            btnCancel.setOnClickListener {
                dialog.dismiss()
            }
            btnConfirm.setOnClickListener {
                viewModel.updateName(id, newName.text.toString())
                dialog.dismiss()
            }
            dialog.show()
        }

    }
}