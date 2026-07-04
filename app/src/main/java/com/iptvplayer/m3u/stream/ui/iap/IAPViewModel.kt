package com.iptvplayer.m3u.stream.ui.iap

import androidx.lifecycle.LifecycleOwner
import com.iptvplayer.m3u.stream.model.entity.Price
import hoang.dqm.codebase.base.viewmodel.BaseViewModel
import hoang.dqm.codebase.firebase.AppRemoteConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class IAPViewModel: BaseViewModel() {
    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)
        getPriceApp()
    }

    private val _price = MutableStateFlow<Price?>(null)
    val price = _price.asStateFlow()
    fun getPrice(): Price? {
        val list = AppRemoteConfig.getData(AppRemoteConfig.DATA_PRICE, Price::class.java)
        return list
    }
    fun getPriceApp(){
        flowOnIO {
            getPrice()
        }.subscribe {
            _price.value = it
        }
    }

}