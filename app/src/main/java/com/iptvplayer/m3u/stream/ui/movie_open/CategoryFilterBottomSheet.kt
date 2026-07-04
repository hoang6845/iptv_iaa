package com.iptvplayer.m3u.stream.ui.movie_open

import android.app.Dialog
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.iptvplayer.m3u.stream.R
import com.iptvplayer.m3u.stream.databinding.BottomSheetCategoryFilterBinding
import com.iptvplayer.m3u.stream.model.entity.RecommendCategory

class CategoryFilterBottomSheet(
    private val categories: List<RecommendCategory>,
    private val selectedCategoryId: Int?,
    private val totalCount: Int,
    private val onSelected: (Int?) -> Unit
) : DialogFragment() {

    private var _binding: BottomSheetCategoryFilterBinding? = null
    private val binding get() = _binding!!

    private val adapter = CategoryFilterAdapter()
    private var searchQuery = ""

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return Dialog(requireContext(), R.style.RightPanelDialog)
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            val screenWidth = resources.displayMetrics.widthPixels
            setLayout((screenWidth * 0.8).toInt(), ViewGroup.LayoutParams.MATCH_PARENT)
            val attrs = attributes
            attrs.gravity = Gravity.END
            attributes = attrs
            setDimAmount(0.4f)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = BottomSheetCategoryFilterBinding.inflate(inflater, container, false)
        .also { _binding = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        adjustInsetsForBottomNavigation(binding.root)
        binding.tvTitle.text    = getString(R.string.text_movies)
        binding.tvSubtitle.text = getString(R.string.text_categories, categories.size)

        binding.rvCategories.layoutManager = LinearLayoutManager(requireContext())
        binding.rvCategories.adapter = adapter

        adapter.setOnItemClick { categoryId ->
            onSelected(categoryId)
            dismiss()
        }

        renderList(searchQuery)

        binding.edtSearch.doAfterTextChanged {
            searchQuery = it.toString()
            renderList(searchQuery)
        }
    }

    private fun renderList(query: String) {
        val filtered = if (query.isBlank()) categories
        else categories.filter { it.categoryName.contains(query, ignoreCase = true) }

        val allItem = CategoryFilterAdapter.Item(
            categoryId = null,
            name       = "All",
            count      = totalCount,
            isSelected = selectedCategoryId == null
        )

        val items = listOf(allItem) + filtered.map {
            CategoryFilterAdapter.Item(
                categoryId = it.categoryId,
                name       = it.categoryName,
                count      = it.count,
                isSelected = it.categoryId == selectedCategoryId
            )
        }

        adapter.submitList(items)
    }

    fun adjustInsetsForBottomNavigation(viewBottom: View) {
        ViewCompat.setOnApplyWindowInsetsListener(viewBottom) { view, insets ->
            try {
                val params = view.layoutParams as ViewGroup.MarginLayoutParams
                val displayCutout =
                    insets.getInsetsIgnoringVisibility(WindowInsetsCompat.Type.systemBars())
                params.topMargin = (displayCutout.top + viewBottom.top / 5f).toInt()
                view.layoutParams = params
            } catch (e: Exception) {
                e.printStackTrace()
            }
            insets
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}