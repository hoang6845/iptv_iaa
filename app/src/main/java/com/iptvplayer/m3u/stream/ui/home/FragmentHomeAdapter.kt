package com.iptvplayer.m3u.stream.ui.home

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.iptvplayer.m3u.stream.ui.home.home_all.HomeAllFragment
import com.iptvplayer.m3u.stream.ui.home.home_file.HomeFileFragment
import com.iptvplayer.m3u.stream.ui.home.home_gallery.HomeGalleryFragment
import com.iptvplayer.m3u.stream.ui.home.home_url.HomeUrlFragment

class FragmentHomeAdapter(
    fragmentManager: FragmentManager,
    lifecycle: Lifecycle
): FragmentStateAdapter(fragmentManager, lifecycle) {
    override fun createFragment(position: Int): Fragment {
        return when (position){
            0 -> HomeAllFragment()
            1 -> HomeUrlFragment()
            2 -> HomeFileFragment()
            3 -> HomeGalleryFragment()
            else -> HomeAllFragment()
        }
    }

    override fun getItemCount(): Int = 4
}