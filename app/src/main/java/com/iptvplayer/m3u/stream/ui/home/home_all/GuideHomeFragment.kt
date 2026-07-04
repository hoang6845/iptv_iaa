package com.iptvplayer.m3u.stream.ui.home.home_all

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.iptvplayer.m3u.stream.R
import com.iptvplayer.m3u.stream.databinding.FragmentGuideHomeBinding
import hoang.dqm.codebase.base.activity.BaseBottomSheetFragment

class GuideHomeFragment(val listGuide: List<String>, val title: String, val num: Int) :
    BaseBottomSheetFragment<FragmentGuideHomeBinding>() {
    override fun getTheme(): Int = R.style.FullScreenBottomSheet

    private val adapter: TextAdapter by lazy {
        TextAdapter()
    }

    override fun getVB(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentGuideHomeBinding {
        _binding = FragmentGuideHomeBinding.inflate(inflater, container, false)
        return binding
    }

    override fun initView() {
        binding.num.text = num.toString()
        binding.title1.text = title
        adapter.setList(listGuide)

        binding.rvText.adapter = adapter
        binding.rvText.layoutManager = LinearLayoutManager(
            requireContext(),
            LinearLayoutManager.VERTICAL,
            false
        )

        binding.btnClose.setOnClickListener {
            dismiss()
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

//            (sheet.parent as? View)?.setBackgroundColor(Color.TRANSPARENT)

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
        layoutParams.height = (resources.displayMetrics.heightPixels * 0.6).toInt()
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