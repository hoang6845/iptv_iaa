package com.iptvplayer.m3u.stream.main

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.shape.MaterialShapeDrawable
import com.google.android.material.shape.ShapeAppearanceModel
import com.iptvplayer.m3u.stream.R
import com.iptvplayer.m3u.stream.databinding.FragmentAddOptionSheetBinding
import hoang.dqm.codebase.base.activity.BaseBottomSheetFragment
import hoang.dqm.codebase.base.activity.navigate
import hoang.dqm.codebase.base.activity.navigateFade
import hoang.dqm.codebase.utils.AppMonetization
import hoang.dqm.codebase.utils.premium

class AddOptionSheet : BaseBottomSheetFragment<FragmentAddOptionSheetBinding>() {
    override fun getTheme(): Int = R.style.FullScreenBottomSheet

    override fun getVB(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentAddOptionSheetBinding {
        _binding = FragmentAddOptionSheetBinding.inflate(inflater, container, false)
        return binding
    }

    override fun initView() {
        binding.btnClose.setOnClickListener {
            dismiss()
        }

        binding.btnAddXtream.setOnClickListener {
            val bundle = Bundle().apply {
                putString("action", "open_add_xtream")
            }
            parentFragmentManager.setFragmentResult("add_option_result", bundle)
            dismiss()
        }

        binding.btnLoadVideo.setOnClickListener {
            val bundle = Bundle().apply {
                putString("action", "gallery")
            }
            parentFragmentManager.setFragmentResult("add_option_result", bundle)
            dismiss()
        }

        binding.btnImportPl.setOnClickListener {
            val bundle = Bundle().apply {
                putString("action", "import_playlist")
            }
            parentFragmentManager.setFragmentResult("add_option_result", bundle)
            dismiss()
        }

        binding.btnUploadM3u.setOnClickListener {
            val bundle = Bundle().apply {
                putString("action", "upload_m3u")
            }
            parentFragmentManager.setFragmentResult("add_option_result", bundle)
            dismiss()
        }

        binding.btnSingleStream.setOnClickListener {
//            if (!AppMonetization.premium.isSubscribed()){
//                navigateFade(R.id.IAPFragment)
//                dismiss()
//                return@setOnClickListener
//            }
            val bundle = Bundle().apply {
                putString("action", "add_single_stream")
            }
            parentFragmentManager.setFragmentResult("add_option_result", bundle)
            dismiss()
        }

        binding.howToUse.paint.isUnderlineText = true
        binding.howToUse.setOnClickListener {
            navigate(R.id.howToYouFragment)
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