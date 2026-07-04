//package com.iptvplayer.m3u.stream.ui.language
//
//import android.util.Log
//import androidx.appcompat.app.AppCompatDelegate
//import androidx.lifecycle.LifecycleOwner
//import androidx.lifecycle.MutableLiveData
//import hoang.dqm.codebase.base.viewmodel.BaseViewModel
//import hoang.dqm.codebase.data.Language
//import hoang.dqm.codebase.data.LanguageApp
//import com.iptvplayer.m3u.stream.utils.CommonAppSharePref
//
//class LanguageAppViewModel: BaseViewModel() {
//    private val appSharePref: CommonAppSharePref by lazy {
//        CommonAppSharePref(context)
//    }
//
//    val languageLiveData by lazy { MutableLiveData<List<LanguageAppSelector>>() }
//    var mLanguageAppSelector: LanguageAppSelector? = null
//
//    override fun onCreate(owner: LifecycleOwner) {
//        super.onCreate(owner)
//        getLang()
//    }
//
//    private fun getLang() {
//        launchHandler {
//            flowOnIO {
//                val languageCode = appSharePref.languageCode ?: Language.ENGLISH.countryCode
//                LanguageApp.entries.flatMapIndexed { _: Int, language: LanguageApp ->
//                    arrayListOf<LanguageAppSelector>().also { list ->
//                        val isCheck = languageCode == language.languageCode
//                        val item = LanguageAppSelector(language, isCheck = isCheck)
//                        list.add(item)
//                        if (item.isCheck) {
//                            mLanguageAppSelector = item
//                        }
//                    }
//                }
//            }.subscribe {
//                languageLiveData.value = it
//            }
//        }
//
//        Log.d("LANG_DEBUG", "Current language: ${AppCompatDelegate.getApplicationLocales()[0]}")
//    }
//
//    fun saveLang(onDone: () -> Unit) {
//        launchHandler {
//            flowOnIO {
//                val languageCode =
//                    mLanguageAppSelector?.language?.languageCode ?: Language.ENGLISH.countryCode
//                appSharePref.languageCode = languageCode
//                appSharePref.applyLanguage(languageCode)
//            }.subscribe {
//                onDone.invoke()
//            }
//        }
//    }
//}