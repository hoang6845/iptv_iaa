package com.iptvplayer.m3u.stream.ui.add_playlist

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.iptvplayer.m3u.stream.ui.add_playlist.import_link.ImportLinkFragment
import com.iptvplayer.m3u.stream.ui.add_playlist.upload_file.UploadFileFragment

class AddPlaylistPagerAdapter(
    fragmentManager: FragmentManager,
    lifecycle: Lifecycle
) : FragmentStateAdapter(fragmentManager, lifecycle) {
    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> ImportLinkFragment()
            1-> UploadFileFragment()
            else -> ImportLinkFragment()

        }
    }

    override fun getItemCount(): Int = 2
}