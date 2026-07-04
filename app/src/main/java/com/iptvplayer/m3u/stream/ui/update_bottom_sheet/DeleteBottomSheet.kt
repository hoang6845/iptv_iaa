package com.iptvplayer.m3u.stream.ui.update_bottom_sheet

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.shape.CornerFamily
import com.google.android.material.shape.MaterialShapeDrawable
import com.google.android.material.shape.ShapeAppearanceModel
import com.iptvplayer.m3u.stream.R
import com.iptvplayer.m3u.stream.databinding.FragmentDeleteBottomSheetBinding
import hoang.dqm.codebase.base.activity.BaseBottomSheetFragment

class DeleteBottomSheet: BaseBottomSheetFragment<FragmentDeleteBottomSheetBinding>() {
    override fun getTheme(): Int = R.style.FullScreenBottomSheet

    override fun getVB(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentDeleteBottomSheetBinding {
        _binding = FragmentDeleteBottomSheetBinding.inflate(inflater, container, false)
        return binding
    }

    override fun initView() {
        binding.icUpdate.setOnClickListener {
            val bundle = Bundle().apply {
                putString("action", "delete")

            }
            parentFragmentManager.setFragmentResult("delete_bottom_sheet", bundle)
            dismiss()
        }

        binding.tvUpdate.setOnClickListener {
            val bundle = Bundle().apply {
                putString("action", "delete")

            }
            parentFragmentManager.setFragmentResult("delete_bottom_sheet", bundle)
            dismiss()
        }

        binding.btnClose.setOnClickListener {
            dismiss()
        }
    }

    override fun onStart() {
        super.onStart()
        val dialog = dialog as? BottomSheetDialog

        dialog?.setCanceledOnTouchOutside(false)
        dialog?.setCancelable(false)

        val window = dialog?.window
        window?.apply {
            setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setDimAmount(0.5f)
        }
        dialog?.let {
            val bottomSheet =
                it.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.let { sheet ->
                (sheet.parent as? View)?.background = null
                val container = it.findViewById<ViewGroup>(android.R.id.content)
                container?.setPadding(0, 0, 0, 0)

                (sheet.parent as? View)?.let { parent ->
                    parent.setBackgroundColor(Color.TRANSPARENT)
                    parent.setPadding(0, 0, 0, 0)

                    val parentParams = parent.layoutParams as? ViewGroup.MarginLayoutParams
                    parentParams?.setMargins(0, 0, 0, 0)
                    parent.layoutParams = parentParams
                }

                val layoutParams = sheet.layoutParams
                layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT
                layoutParams.height = (resources.displayMetrics.heightPixels * 0.15).toInt()
                sheet.layoutParams = layoutParams

                val radius = resources.getDimension(hoang.dqm.codebase.R.dimen._16sdp)

                val shapeAppearanceModel = ShapeAppearanceModel.builder()
                    .setTopLeftCorner(
                        CornerFamily.ROUNDED,
                        radius
                    )
                    .setTopRightCorner(
                        CornerFamily.ROUNDED,
                        radius
                    )
                    .build()

                val materialShapeDrawable = MaterialShapeDrawable(shapeAppearanceModel).apply {
                    fillColor = ColorStateList.valueOf(Color.WHITE)
                }

                sheet.background = materialShapeDrawable

                val behavior = BottomSheetBehavior.from(sheet)
                behavior.state = BottomSheetBehavior.STATE_COLLAPSED
                behavior.isDraggable = false
                behavior.isHideable = false
                behavior.skipCollapsed = true
                behavior.peekHeight = layoutParams.height

                sheet.requestLayout()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.FullScreenBottomSheet)
    }
}