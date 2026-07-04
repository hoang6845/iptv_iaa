package com.iptvplayer.m3u.stream.ui.iap_bottom_sheet

import android.content.DialogInterface
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.shape.MaterialShapeDrawable
import com.google.android.material.shape.ShapeAppearanceModel
import com.iptvplayer.m3u.stream.R
import com.iptvplayer.m3u.stream.databinding.FragmentIapBottomSheetBinding
import hoang.dqm.codebase.base.activity.BaseBottomSheetFragment
import hoang.dqm.codebase.base.activity.navigate

class IAPBottomSheetFragment : BaseBottomSheetFragment<FragmentIapBottomSheetBinding>() {
    override fun getTheme(): Int = R.style.FullScreenBottomSheet

    override fun getVB(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentIapBottomSheetBinding {
        _binding = FragmentIapBottomSheetBinding.inflate(inflater, container, false)
        return binding
    }

    var itemSelected = 0

    override fun initView() {
        binding.btnIap1.setOnClickListener {
            itemSelected = 0
            updateUI()
        }

        binding.btnIap2.setOnClickListener {
            itemSelected = 1
            updateUI()
        }

        binding.btnIap3.setOnClickListener {
            itemSelected = 2
            updateUI()
        }
        updateUI()
        binding.btnSave.setOnClickListener {
            navigate(R.id.homeFragment, isPop = true)
        }
    }

    fun updateUI() {
        when (itemSelected) {
            0 -> {
                binding.btnIap1.setBackgroundResource(R.drawable.box_bg_iap)
                binding.btnIap2.setBackgroundResource(R.drawable.box_bg_iap_unselected)
                binding.btnIap3.setBackgroundResource(R.drawable.box_bg_iap_unselected)
                binding.btnSave.text = getString(R.string.text_start_free_now)
            }

            1 -> {
                binding.btnIap1.setBackgroundResource(R.drawable.box_bg_iap_unselected)
                binding.btnIap2.setBackgroundResource(R.drawable.box_bg_iap)
                binding.btnSave.text = getString(R.string.text_get_now)
                binding.btnIap3.setBackgroundResource(R.drawable.box_bg_iap_unselected)
            }

            2 -> {
                binding.btnIap1.setBackgroundResource(R.drawable.box_bg_iap_unselected)
                binding.btnIap2.setBackgroundResource(R.drawable.box_bg_iap_unselected)
                binding.btnSave.text = getString(R.string.text_get_now)
                binding.btnIap3.setBackgroundResource(R.drawable.box_bg_iap)
            }
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
                layoutParams.height = (resources.displayMetrics.heightPixels * 0.65).toInt()
                sheet.layoutParams = layoutParams

                val radius = resources.getDimension(hoang.dqm.codebase.R.dimen._16sdp)

                val shapeAppearanceModel = ShapeAppearanceModel.builder()
                    .setTopLeftCorner(
                        com.google.android.material.shape.CornerFamily.ROUNDED,
                        radius
                    )
                    .setTopRightCorner(
                        com.google.android.material.shape.CornerFamily.ROUNDED,
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
        dialog?.let {
            val bottomSheet =
                it.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.let { sheet ->
                val parent = sheet.parent as? ViewGroup
                parent?.let { p ->
                    if (p.findViewById<View>(R.id.btn_close_external) == null) {
                        val closeBtn = ImageView(requireContext()).apply {
                            id = R.id.btn_close_external
                            setImageResource(R.drawable.ic_close_dialog)
                            setPadding(16, 16, 16, 16)
                            visibility = View.INVISIBLE
                            setOnClickListener { dismiss() }
                        }

                        val sizePx = (36 * resources.displayMetrics.density).toInt()

                        val params =
                            androidx.coordinatorlayout.widget.CoordinatorLayout.LayoutParams(
                                sizePx,
                                sizePx
                            ).apply {
                                gravity = android.view.Gravity.TOP or android.view.Gravity.END
                                topMargin = 12
                                rightMargin = 12
                            }
                        p.addView(closeBtn, params)

                        closeBtn.postDelayed({
                            if (isAdded && !isDetached && view != null) {
                                closeBtn.visibility = View.VISIBLE
                            }
                        }, 3000)
                    }
                }
            }
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        navigate(R.id.homeFragment, isPop = true)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.FullScreenBottomSheet)
    }
}