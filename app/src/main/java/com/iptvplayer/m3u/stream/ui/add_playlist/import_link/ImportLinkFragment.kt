package com.iptvplayer.m3u.stream.ui.add_playlist.import_link

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.iptvplayer.m3u.stream.R
import com.iptvplayer.m3u.stream.databinding.FragmentImportLinkBinding
import com.iptvplayer.m3u.stream.model.entity.Country
import com.iptvplayer.m3u.stream.ui.add_playlist.PasscodeDialog
import com.iptvplayer.m3u.stream.ui.home.HomeFragment
import com.iptvplayer.m3u.stream.utils.PasscodeManager
import dagger.hilt.android.AndroidEntryPoint
import hoang.dqm.codebase.base.activity.BaseFragment
import hoang.dqm.codebase.base.activity.navigate
import hoang.dqm.codebase.base.activity.onBackPressed
import hoang.dqm.codebase.base.activity.popBackStack
import hoang.dqm.codebase.utils.collectLatestFlow
import javax.inject.Inject

@AndroidEntryPoint
class ImportLinkFragment : BaseFragment<FragmentImportLinkBinding, ImportLinkViewModel>() {
    @Inject
    lateinit var passcodeManager: PasscodeManager

    var isExpand = true
    var isExpandCategory = true
    private var currentPasscode: String? = null
    private val categoryAdapter: CategoryAdapter by lazy { CategoryAdapter() }
    private var activeSelection: SelectionMode = SelectionMode.NONE

    enum class SelectionMode { NONE, COUNTRY, CATEGORY }
    private var listCountryExplore = mutableListOf<Country>()

    private lateinit var adapter: CountrySpinnerAdapter

    override fun initView() {
        listCountryExplore = mutableListOf(Country(getString(R.string.text_choose_country), R.drawable.flag_demo, ""),
        Country("United States", R.drawable.flag_us, "US"),
        Country("United Kingdom", R.drawable.flag_uk, "UK"),
        Country("France", R.drawable.flag_franch, "FR"),
        Country("Germany", R.drawable.flag_germany, "DE"),
        Country("Spain", R.drawable.flag_spain, "ES"),
        Country("Italy", R.drawable.flag_italya, "IT"),
        Country("Netherlands", R.drawable.flag_netherlands, "NE"),
        Country("Belgium", R.drawable.flag_belgium, "BE"),
        Country("Indonesia", R.drawable.flag_indonesia, "IN"),
        Country("Vietnam", R.drawable.flag_vietnam, "VN"),
        Country("United Arab Emirates", R.drawable.flag_uae, "AE"),
        Country("Saudi Arabia", R.drawable.flag_saudi_arabia, "SA"))
        adapter = CountrySpinnerAdapter(
            requireContext(),
            listCountryExplore,
            selectedPosition = 0
        )

        adapter.listener = object : CountrySpinnerAdapter.OnCountrySelectedListener {
            override fun onCountrySelected(country: Country, position: Int) {
                binding.spinnerCountry.setSelection(position)
                adapter.setSelectedPosition(position)
                binding.edtUrl.setText(
                    getString(
                        R.string.text_https_iptv_org_github_io_iptv_countries_m3u,
                        listCountryExplore[position].id.lowercase()
                    )
                )
                binding.edtPlaylistName.setText(listCountryExplore[position].name)
                if (position == 0) {
                    activeSelection = SelectionMode.NONE
                    setCategoryEnabled(true)
                    return
                }
                activeSelection = SelectionMode.COUNTRY
                setCategoryEnabled(false)
            }
        }
        binding.spinnerCountry.adapter = adapter

        updateUI()
        setUpRVCategory()
    }

    override fun initListener() {
        binding.textHowtoUpload.paint.isUnderlineText = true
        binding.textHowtoUpload.setOnClickListener {
            val bundle = Bundle().apply {
                putInt("position", 0)
            }
            navigate(R.id.howToYouFragment, bundle)
        }
        binding.edtUrl.addTextChangedListener { text ->
            if (text.isNullOrEmpty()) {
                activeSelection = SelectionMode.NONE
                setCategoryEnabled(true)
                setCountryEnabled(true)
                categoryAdapter.clearSelection()
                binding.spinnerCountry.setSelection(0)
                adapter.setSelectedPosition(0)
            }
        }
        onBackPressed { popBackStack() }
        binding.textUrl.setEndIconOnClickListener {
            val text = binding.edtUrl.text.toString()
            if (text.isNotEmpty()) {
                val clipboard =
                    requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("url", text))
                Toast.makeText(requireContext(), "Copied", Toast.LENGTH_SHORT).show()
            }
        }

        binding.layoutExpandCountry.setOnClickListener {
            isExpand = !isExpand
            binding.spinnerCountry.visibility = if (isExpand) View.VISIBLE else View.GONE
            binding.imgArrow.animate().rotation(if (isExpand) 90f else 0f).duration = 200
        }

        binding.layoutExpandCategory.setOnClickListener {
            isExpandCategory = !isExpandCategory
            binding.rvCategory.visibility = if (isExpandCategory) View.VISIBLE else View.GONE
            binding.imgArrow1.animate().rotation(if (isExpandCategory) 90f else 0f).duration = 200
        }

        binding.btnSave.setOnClickListener {
            val playlistName = binding.edtPlaylistName.text?.toString()?.trim()
            val url = binding.edtUrl.text?.toString()?.trim()

            if (playlistName.isNullOrEmpty()) {
                binding.textName.error = getString(R.string.text_enter_playlist_name)
                return@setOnClickListener
            }
            if (url.isNullOrEmpty()) {
                binding.textUrl.error = getString(R.string.text_enter_url)
                return@setOnClickListener
            }
            binding.textName.error = null
            binding.textUrl.error = null

//            if (!AppMonetization.premium.isSubscribed()){
//                navigateFade(R.id.IAPFragment)
//                return@setOnClickListener
//            }

            val isPasscodeEnabled = binding.btnToggleMusic.isChecked

            if (isPasscodeEnabled && !passcodeManager.hasPasscode()) {
                showSetPasscodeDialog(onSuccess = {
                    viewModel.savePlaylist(playlistName, url, true)
                }) {

                }
            } else {
                viewModel.savePlaylist(playlistName, url, isPasscodeEnabled)
            }
        }
    }

    override fun initData() {
        collectLatestFlow(viewModel.listCategoryChannel) { categoryItems ->
            Log.d("check category", "initData: $categoryItems")
            categoryAdapter.setList(categoryItems)
        }

        collectLatestFlow(viewModel.saveState) { state ->
            when (state) {
                is ImportSaveState.Idle -> {
                    setLoadingVisible(false)
                }

                is ImportSaveState.Loading -> {
                    setLoadingVisible(true)
                    binding.tvProgress.text = "${state.progress}%"

                    binding.tvLoadingLabel.text = when {
                        state.progress <= 10 -> getString(R.string.text_connecting)
                        state.progress <= 50 -> getString(R.string.text_downloading)
                        state.progress <= 80 -> getString(R.string.text_parsing_channels)
                        else -> getString(R.string.text_saving)
                    }
                }

                is ImportSaveState.Success -> {
                    setLoadingVisible(false)
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.text_save_playlist_success),
                        Toast.LENGTH_SHORT
                    ).show()
                    popToHomeUrlTab()
                }

                is ImportSaveState.Error -> {
                    setLoadingVisible(false)
                    Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
                    binding.textUrl.error = state.message
                }
            }
        }
    }

    private fun popToHomeUrlTab() {
        findNavController()
            .previousBackStackEntry
            ?.savedStateHandle
            ?.set(HomeFragment.KEY_HOME_TAB, HomeFragment.TAB_URL)

        popBackStack()
    }

    private fun setLoadingVisible(visible: Boolean) {
        binding.layoutLoading.visibility = if (visible) View.VISIBLE else View.GONE
        binding.btnSave.isEnabled = !visible
    }

    fun setUpRVCategory() {
        categoryAdapter.onClickItem({ item, _ ->
            if (!categoryAdapter.isEnabled) return@onClickItem
            binding.edtUrl.setText(item.url)
            binding.edtPlaylistName.setText(item.type)

            activeSelection = SelectionMode.CATEGORY
            setCountryEnabled(false)
        }) { url, _ ->
            val clipboard =
                requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("copied_text", url.url))
            Toast.makeText(requireContext(), "Copied", Toast.LENGTH_SHORT).show()
        }

        binding.rvCategory.adapter = categoryAdapter
        binding.rvCategory.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
    }


    fun updateUI() {
        binding.spinnerCountry.visibility = if (isExpand) View.VISIBLE else View.GONE
        binding.imgArrow.animate().rotation(if (isExpand) 90f else 0f).duration = 200
        binding.rvCategory.visibility = if (isExpandCategory) View.VISIBLE else View.GONE
        binding.imgArrow1.animate().rotation(if (isExpandCategory) 90f else 0f).duration = 200
    }

    private fun showSetPasscodeDialog(onSuccess: () -> Unit, onCancel: () -> Unit) {
        PasscodeDialog(
            context = requireContext(),
            onConfirm = { passcode ->
                passcodeManager.savePasscode(passcode)
                onSuccess()
            },
            onCancel = {
                onCancel()
            }
        ).show()
    }

    private fun setCategoryEnabled(enabled: Boolean) {
        binding.rvCategory.alpha = if (enabled) 1f else 0.4f
        binding.rvCategory.isEnabled = enabled
        categoryAdapter.setEnabledNow(enabled)
        binding.layoutExpandCategory.isEnabled = enabled
        binding.layoutExpandCategory.alpha = if (enabled) 1f else 0.4f
    }

    private fun setCountryEnabled(enabled: Boolean) {
        binding.spinnerCountry.isEnabled = enabled
        binding.spinnerCountry.alpha = if (enabled) 1f else 0.4f
        binding.layoutExpandCountry.isEnabled = enabled
        binding.layoutExpandCountry.alpha = if (enabled) 1f else 0.4f
    }
}