package com.iptvplayer.m3u.stream.ui.sort_bottom_sheet

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.graphics.toColorInt
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.shape.CornerFamily
import com.google.android.material.shape.MaterialShapeDrawable
import com.google.android.material.shape.ShapeAppearanceModel
import com.iptvplayer.m3u.stream.R
import com.iptvplayer.m3u.stream.databinding.FragmentSortBottomSheetBinding
import com.iptvplayer.m3u.stream.utils.visible
import hoang.dqm.codebase.base.activity.BaseBottomSheetFragment

class SortBottomSheet(val isSelected: String): BaseBottomSheetFragment<FragmentSortBottomSheetBinding>() {
    override fun getTheme(): Int = R.style.FullScreenBottomSheet

    override fun getVB(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentSortBottomSheetBinding {
        _binding = FragmentSortBottomSheetBinding.inflate(inflater, container, false)
        return binding
    }

    override fun initView() {
        binding.aZ.setOnClickListener {
            val bundle = Bundle().apply {
                putString("action", "az")

            }
            parentFragmentManager.setFragmentResult("sort_bottom_sheet", bundle)
            dismiss()
        }

        binding.zA.setOnClickListener {
            val bundle = Bundle().apply {
                putString("action", "za")

            }
            parentFragmentManager.setFragmentResult("sort_bottom_sheet", bundle)
            dismiss()
        }

        binding.none.setOnClickListener {
            val bundle = Bundle().apply {
                putString("action", "none")

            }
            parentFragmentManager.setFragmentResult("sort_bottom_sheet", bundle)
            dismiss()
        }

        binding.recentlyAdd.setOnClickListener {
            val bundle = Bundle().apply {
                putString("action", "recentlyAdd")

            }
            parentFragmentManager.setFragmentResult("sort_bottom_sheet", bundle)
            dismiss()
        }

        binding.btnClose.setOnClickListener {
            dismiss()
        }
        updateUI()

    }

    fun updateUI(){
        when (isSelected){
            "az" -> {
                binding.aZ.setBackgroundColor(context?.getAttrColor(R.attr.colorBgBox)?:"#037EB9".toColorInt())
                binding.icAZ.visible()
                binding.aZ.setOnClickListener {  }
            }
            "za" -> {
                binding.zA.setBackgroundColor(context?.getAttrColor(R.attr.colorBgBox)?:"#037EB9".toColorInt())
                binding.icZA.visible()
                binding.zA.setOnClickListener {  }

            }
            "none"-> {
                binding.none.setBackgroundColor(context?.getAttrColor(R.attr.colorBgBox)?:"#037EB9".toColorInt())
                binding.icNone.visible()
                binding.none.setOnClickListener {  }

            }
            "recentlyAdd" -> {
                binding.recentlyAdd.setBackgroundColor(context?.getAttrColor(R.attr.colorBgBox)?:"#037EB9".toColorInt())
                binding.icRecentlyAdd.visible()
                binding.recentlyAdd.setOnClickListener {  }

            }
        }
    }
    fun Context.getAttrColor(attr: Int): Int {
        val typedValue = TypedValue()
        theme.resolveAttribute(attr, typedValue, true)
        return typedValue.data
    }
    override fun onStart() {
        super.onStart()
        val dialog = dialog as? BottomSheetDialog

        dialog?.setCanceledOnTouchOutside(true)
        dialog?.setCancelable(true)

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
                layoutParams.height = (resources.displayMetrics.heightPixels * 0.3).toInt()
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