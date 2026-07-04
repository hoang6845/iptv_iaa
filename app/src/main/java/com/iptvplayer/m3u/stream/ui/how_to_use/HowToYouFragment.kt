package com.iptvplayer.m3u.stream.ui.how_to_use

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.util.TypedValue
import androidx.core.graphics.toColorInt
import androidx.core.text.buildSpannedString
import androidx.core.text.color
import com.iptvplayer.m3u.stream.R
import com.iptvplayer.m3u.stream.databinding.FragmentHowToYouBinding
import com.iptvplayer.m3u.stream.utils.gone
import com.iptvplayer.m3u.stream.utils.visible
import hoang.dqm.codebase.base.activity.BaseFragment
import hoang.dqm.codebase.base.activity.onBackPressed
import hoang.dqm.codebase.base.activity.popBackStack
import hoang.dqm.codebase.utils.TabButton


class HowToYouFragment : BaseFragment<FragmentHowToYouBinding, HowToUseViewModel>() {
    override fun initView() {
        adjustInsetsForBottomNavigation(binding.toolBar)
        currentIndex = arguments?.getInt("position")?:0
        setUpTabBar()
        updateUiTextView()
    }

    private var currentIndex = 0

    override fun initListener() {
        binding.btnBack.setOnClickListener {
            popBackStack()
        }


        onBackPressed { popBackStack() }

        binding.btnSearch.setOnClickListener {
            val query = when (currentIndex) {
                0 -> "Free Popular IPTV Playlist"
                1 -> "Free Popular Xtream Account"
                else -> "Free m3u file download"
            }
            val uri = Uri.parse("https://www.google.com/search?q=${Uri.encode(query)}")
            startActivity(Intent(Intent.ACTION_VIEW, uri))
        }
    }

    override fun initData() {

    }

    fun setUpTabBar(){
        binding.tabBar.setup(
            items = listOf(
                TabButton.TabBarItem(
                    title = getString(R.string.text_playlist_cap),
                    unselectedTextColor = requireContext().getAttrColor(R.attr.textHint),
                    selectedTextColor = Color.WHITE
                ),
                TabButton.TabBarItem(
                    title = getString(R.string.text_xtream_cap),
                    unselectedTextColor =requireContext().getAttrColor(R.attr.textHint),
                    selectedTextColor = Color.WHITE
                ),
                TabButton.TabBarItem(
                    title = getString(R.string.text_file_cap),
                    unselectedTextColor =requireContext().getAttrColor(R.attr.textHint),
                    selectedTextColor = Color.WHITE
                )
            ),
            currentIndex,
            onTabSelected = { index ->
                when (index){
                    0 -> {
                        currentIndex = 0
                        updateUi()
                    }
                    1->{
                        currentIndex = 1
                        updateUi()
                    }
                    2-> {
                        currentIndex = 2
                        updateUi()
                    }
                }
            }
        )
        binding.tabBar.selectTab(currentIndex)
        updateUi()
    }

    fun Context.getAttrColor(attr: Int): Int {
        val typedValue = TypedValue()
        theme.resolveAttribute(attr, typedValue, true)
        return typedValue.data
    }

    fun updateUi(){
        when (currentIndex) {
            0 -> {
                binding.contentPlaylist.visible()
                binding.contentXtream.gone()
                binding.contentFile.gone()
            }
            1 -> {
                binding.contentPlaylist.gone()
                binding.contentXtream.visible()
                binding.contentFile.gone()
            }
            2 -> {
                binding.contentPlaylist.gone()
                binding.contentXtream.gone()
                binding.contentFile.visible()
            }
        }
    }

    fun updateUiTextView() {
        binding.text1File.text = buildSpannedString {
            color(requireContext().getAttrColor(R.attr.myColorOnSurface)) {
                append(getString(R.string.text_search_for))
            }
            append(" ")
            color("#0062FF".toColorInt()) {
                append("\"${getString(R.string.text_quot_m3u_file_quot)}\"")
            }
            append(" ")
            color(requireContext().getAttrColor(R.attr.myColorOnSurface)) {
                append(getString(R.string.text_in_any_search_engine))
            }
        }

        binding.text1Xtream.text = buildSpannedString {
            color(requireContext().getAttrColor(R.attr.myColorOnSurface)) {
                append(getString(R.string.text_search_for))
            }
            append(" ")
            color("#0062FF".toColorInt()) {
                append("\"${getString(R.string.text_free_popular_xtream)}\"")
            }
            append(" ")
            color(requireContext().getAttrColor(R.attr.myColorOnSurface)) {
                append(getString(R.string.text_in_any_search_engine))
            }
        }

        binding.text2Xtream.text = buildSpannedString {
            color(requireContext().getAttrColor(R.attr.myColorOnSurface)) {
                append(getString(R.string.text_2_find_and_copy_the))
            }
            append(" ")
            color("#0062FF".toColorInt()) {
                append("\"${getString(R.string.text_host_username_and_password)}\"")
            }
            append(" ")
            color(requireContext().getAttrColor(R.attr.myColorOnSurface)) {
                append(getString(R.string.text_of_the_xtream_account))
            }
        }

        binding.text3Xtream.text = buildSpannedString {
            color(requireContext().getAttrColor(R.attr.myColorOnSurface)) {
                append(getString(R.string.text_3_go_back_to_the_app_click))
            }
            append(" ")
            color("#0062FF".toColorInt()) {
                append("\"${getString(R.string.text_quot_add_quot)}\"")
            }
            append(" ")
            color(requireContext().getAttrColor(R.attr.myColorOnSurface)) {
                append(getString(R.string.text_button_then_select))
            }
            append(" ")
            color("#0062FF".toColorInt()) {
                append(getString(R.string.text_quot_add_your_xtream_account_quot))
            }
            append(" ")
            color(requireContext().getAttrColor(R.attr.myColorOnSurface)) {
                append(getString(R.string.text_and_paste_the_link))
            }
        }

        binding.text1.text = buildSpannedString {
            color(requireContext().getAttrColor(R.attr.myColorOnSurface)) {
                append(getString(R.string.text_search_for))
            }
            append(" ")
            color("#0062FF".toColorInt()) {
                append("\"${getString(R.string.text_free_popular_playlist)}\"")
            }
            append(" ")
            color(requireContext().getAttrColor(R.attr.myColorOnSurface)) {
                append(getString(R.string.text_in_any_search_engine))
            }
        }

        binding.text2.text = buildSpannedString {
            color(requireContext().getAttrColor(R.attr.myColorOnSurface)) {
                append(getString(R.string.text_copy_the))
            }
            append(" ")
            color("#0062FF".toColorInt()) {
                append("\"${getString(R.string.text_quot_m3u_quot)}\"")
            }
            append(" ")
            color(requireContext().getAttrColor(R.attr.myColorOnSurface)) {
                append(getString(R.string.text_playlist_link_or_download_the_file))
            }
        }
    }
}