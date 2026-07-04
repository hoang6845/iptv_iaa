package com.iptvplayer.m3u.stream.ui.home.home_all

import android.content.Context
import android.os.Bundle
import android.util.TypedValue
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.graphics.toColorInt
import androidx.core.text.buildSpannedString
import androidx.core.text.color
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.textfield.TextInputEditText
import com.iptvplayer.m3u.stream.R
import com.iptvplayer.m3u.stream.databinding.FragmentHomeAllBinding
import com.iptvplayer.m3u.stream.main.AddOptionSheet
import com.iptvplayer.m3u.stream.model.entity.PlaylistFavourite
import com.iptvplayer.m3u.stream.model.entity.PlaylistWithChannels
import com.iptvplayer.m3u.stream.ui.home.HomeViewModel
import com.iptvplayer.m3u.stream.ui.home.PlaylistAdapter
import com.iptvplayer.m3u.stream.ui.home.edit_playlist.EditPlaylistBottomSheet
import com.iptvplayer.m3u.stream.ui.single_stream.SingleStreamBottomSheet
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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class HomeAllFragment : BaseFragment<FragmentHomeAllBinding, HomeAllViewModel>() {
    @Inject
    lateinit var passcodeManager: PasscodeManager
    private val homeViewModel: HomeViewModel by activityViewModels()
    private val adapter: PlaylistAdapter by lazy {
        PlaylistAdapter()
    }

    override fun initView() {
        setUpAdapter()
        binding.tvHowToAdd.setOnClickListener {
            val bundle = Bundle().apply {
                putInt("position", 0)
            }
            navigate(R.id.howToYouFragment, bundle)
        }

        binding.btnImportPl.setOnClickListener {
            showInterstitialAd {
                showInterstitialAd {
                    navigate(
                        R.id.addPlaylistFragment
                    )
                }
            }
        }
    }

    override fun initListener() {
        val titles = listOf(
            getString(R.string.text_find_an_m3u_playlist_),
            getString(R.string.text_copy_the_link_or_download_),
            getString(R.string.text_import_the_playlist_into_the_app),
            getString(R.string.text_start_watching)
        )

        val listGuide = listOf(
            listOf(
                getString(R.string.text_use_any_search_engine_and_type_keywords_like_iptv_m3u_playlist_or_follow_instructions_from_your_tv_service_provider),
                getString(R.string.text_a_valid_m3u_link_usually_starts_with_http_or_https_ends_with_m3u_or_m3u8),
                getString(R.string.text_whenever_possible_use_reputable_and_legal_sources_some_playlists_may_be_temporary_password_protected_or_geo_restricted)
            ),
            listOf(
                getString(R.string.text_to_copy_the_m3u_link_mobile_long_press_the_link_copy_link_desktop_right_click_copy_link_address),
                getString(R.string.text_to_download_the_file_save_the_m3u_or_m3u8_file_somewhere_easy_to_find),
                getString(R.string.text_you_can_add_the_file_directly_from_your_device_storage_later_see_step_3)
            ),
            listOf(
                getString(R.string.text_open_iptv_smart_player),
                getString(R.string.text_tap_add_playlist),
                getString(R.string.text_choose_one_of_the_following_options_import_url_paste_the_copied_m3u_link_import_m3u_file_select_the_saved_m3u_file_from_your_device_storage),
                getString(R.string.text_optional_give_your_playlist_a_name_to_recognize_it_later),
                getString(R.string.text_tap_save)
            ),
            listOf(
                getString(R.string.text_wait_a_few_seconds_for_the_app_to_load_channels_and_categories),
                getString(R.string.text_browse_available_channels_or_use_search_to_find_specific_ones),
                getString(R.string.text_if_nothing_appears_check_your_internet_connection_make_sure_the_m3u_link_or_file_is_still_valid_and_active)
            )
        )
        binding.btnGuide2.setOnClickListener {
            val bottomSheet = GuideHomeFragment(listGuide[1], titles[1], 2)
            bottomSheet.show(childFragmentManager, "BOTTOM_SHEET_GUIDE")
        }

        binding.btnGuide3.setOnClickListener {
            val bottomSheet = GuideHomeFragment(listGuide[2], titles[2], 3)
            bottomSheet.show(childFragmentManager, "BOTTOM_SHEET_GUIDE")
        }

        binding.btnGuide4.setOnClickListener {
            val bottomSheet = GuideHomeFragment(listGuide[3], titles[3], 4)
            bottomSheet.show(childFragmentManager, "BOTTOM_SHEET_GUIDE")
        }

        binding.btnGuide1.setOnClickListener {
            val bottomSheet = GuideHomeFragment(listGuide[0], titles[0], 1)
            bottomSheet.show(childFragmentManager, "BOTTOM_SHEET_GUIDE")
        }

        binding.btnGoFavourite.setOnClickListener {
            showInterstitialAd {
                val bundle = Bundle().apply { putBoolean("isFavourite", true) }
                navigate(R.id.favouriteOpenFragment, bundle)
            }
        }

        binding.bgImportPl.setOnClickListener {
            val bottomSheet = AddOptionSheet()
            bottomSheet.show(childFragmentManager, "BOTTOM_SHEET_ADD_OPTION")
        }

        childFragmentManager.setFragmentResultListener(
            "add_option_result",
            this
        ) { _, bundle ->

            val action = bundle.getString("action")

            if (action == "open_add_xtream") {
                showInterstitialAd {
                    navigate(R.id.xtreamServerFragment)
                }

            } else if (action == "import_playlist") {
                showInterstitialAd {
                    navigate(R.id.addPlaylistFragment)
                }

            } else if (action == "upload_m3u") {
                showInterstitialAd {
                    val bundle = Bundle().apply {
                        putBoolean("isUpload", true)
                    }
                    navigate(R.id.addPlaylistFragment, bundle)
                }

            } else if (action == "add_single_stream") {
                val bottomSheet = SingleStreamBottomSheet()
                bottomSheet.show(parentFragmentManager, "SINGLE_STREAM")
            } else if (action == "gallery") {
                showInterstitialAd {
                    navigate(R.id.addGalleryFragment)
                }

            }
        }
    }

    override fun initData() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                combine(
                    homeViewModel.playlists,
                    viewModel.favouritePlaylists
                ) { playlists, favourites ->
                    Pair(playlists, favourites)
                }.collect { (playlists, favourites) ->

                    val favouriteNum = favourites.size

                    binding.tvNumChannel.text = buildSpannedString {
                        color("#037EB9".toColorInt()) {
                            append("$favouriteNum")
                        }
                        append(" ")
                        color(requireContext().getAttrColor(R.attr.textHint)) {
                            append("channels")
                        }
                    }

                    val favouritePlaylist = PlaylistFavourite(
                        name = "Favourite",
                        channels = favourites,
                        typePlayList = AppConstants.TYPE_PLAYLIST_FAVOURITE
                    )

                    homeViewModel.setFavouriteList(favouritePlaylist)

                    if (playlists.isNotEmpty()) {
                        binding.contentData.visible()
                        binding.contentNoData.gone()
                    } else {
                        binding.contentData.gone()
                        binding.contentNoData.visible()
                    }

                    adapter.setList(playlists)
                }
            }
        }
    }

    fun setUpAdapter() {
        adapter.setOnClickItemAdapter({ item, position ->
            if (item.playlist.typePlayList == AppConstants.TYPE_PLAYLIST_POPULAR_CHANNEL) {
                showInterstitialAd {
                    navigate(R.id.categoryChannelFragment)
                }
            } else {
                if (item.playlist.isPasscodeEnabled && passcodeManager.hasPasscode()) {
                    // Có passcode → verify trước
                    PasscodeVerifyDialog(
                        activity = requireActivity(),
                        passcodeManager = passcodeManager,
                        onSuccess = {
                            homeViewModel.setPlaylistSelected(item)
                            showInterstitialAd {
                                navigate(R.id.categoryChannelFragment)
                            }
                        },
                        onCancel = { }
                    ).show()
                } else {
                    if (item.playlist.typePlayList == AppConstants.TYPE_PLAYLIST_FILE || item.playlist.typePlayList == AppConstants.TYPE_PLAYLIST_GALLERY) {
                        homeViewModel.setPlaylistSelected(item)
                        showInterstitialAd {
                            navigate(R.id.listChannelFragment)
                        }
                        return@setOnClickItemAdapter
                    }
                    homeViewModel.setPlaylistSelected(item)
                    showInterstitialAd {
                        navigate(R.id.categoryChannelFragment)
                    }
                }
            }
        }) { item, position ->
            val bottomSheet = EditPlaylistBottomSheet()
            val bundle = Bundle().apply {
                putParcelable("playlist", item.playlist)
            }
            bottomSheet.arguments = bundle
            bottomSheet.show(childFragmentManager, "BOTTOM_SHEET_EDIT")
        }

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
                    currentName = name ?: "",
                    onConfirm = { newName ->
                        viewModel.updateName(id, newName)
                    }
                ).show()
            } else if (action == "delete") {

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

        binding.rvPlaylist.adapter = adapter
        binding.rvPlaylist.layoutManager = LinearLayoutManager(
            requireContext(),
            LinearLayoutManager.VERTICAL, false
        )

    }

    fun showDialogRename(id: Long) {
        if (!isAdded || isDetached || view == null) return
        context?.let { ctx ->
            val dialogView = layoutInflater.inflate(R.layout.dialog_rename, null)
            val dialog =
                AlertDialog.Builder(ctx).setView(dialogView).setCancelable(true).create()

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
            dialog.window?.setBackgroundDrawableResource(R.color.black_88000000)
            dialog.show()
        }
    }

    fun updateAdapter(playlist1: PlaylistWithChannels, playlist2: List<PlaylistWithChannels>) {
        val playlists = listOf(playlist1) + playlist2
        adapter.setList(playlists)
    }

    fun Context.getAttrColor(attr: Int): Int {
        val typedValue = TypedValue()
        theme.resolveAttribute(attr, typedValue, true)
        return typedValue.data
    }
}