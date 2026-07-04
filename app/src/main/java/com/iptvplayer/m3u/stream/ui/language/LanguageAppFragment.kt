//package com.iptvplayer.m3u.stream.ui.language
//
//import androidx.core.view.isVisible
//import com.iptvplayer.m3u.stream.databinding.FragmentLanguageAppBinding
//import hoang.dqm.codebase.base.activity.BaseFragment
//import hoang.dqm.codebase.base.activity.popBackStack
//import hoang.dqm.codebase.data.Language
//import hoang.dqm.codebase.service.session.isFirst
//import com.iptvplayer.m3u.stream.utils.CommonAppSharePref
//import hoang.dqm.codebase.utils.RecyclerUtils
//import hoang.dqm.codebase.utils.singleClick
//
//
//class LanguageAppFragment : BaseFragment<FragmentLanguageAppBinding, LanguageAppViewModel>() {
//    private val appSharePref: CommonAppSharePref by lazy {
//        CommonAppSharePref(requireContext())
//    }
//    private val languageAdapter by lazy { LanguageAdapter() }
//    override fun initView() {
//        adjustInsetsForBottomNavigation(binding.clContainer)
//        binding.imvBack.isVisible = isFirst().not()
//        RecyclerUtils.setLinearLayoutManager(
//            requireActivity(),
//            binding.rcvLanguage,
//            languageAdapter
//        )
//        languageAdapter.setOnClickItemRecyclerView { language, _ ->
//            languageAdapter.setSelectLang(language)
//            viewModel.mLanguageAppSelector = language
//        }
//    }
//
//    override fun initListener() {
//        binding.imvBack.singleClick {
//            popBackStack()
//        }
//        binding.btnDone.singleClick {
//            val current = appSharePref.languageCode ?: Language.ENGLISH.countryCode
//            val checked =
//                languageAdapter.dataList.find { it.isCheck }?.language?.languageCode ?: "en"
//            if (current == checked) {
//                popBackStack()
//                return@singleClick
//            } else {
//                updateNewLang()
//            }
//        }
//    }
//
//    private fun updateNewLang() {
//        viewModel.saveLang {
//            activity?.let {
//                it.recreate()
//                popBackStack()
//            }
//        }
//    }
//
//    override fun initData() {
//        viewModel.languageLiveData.observe {
//            languageAdapter.addData(it)
//        }
//    }
//}