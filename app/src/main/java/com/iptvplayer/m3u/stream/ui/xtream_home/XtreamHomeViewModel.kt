package com.iptvplayer.m3u.stream.ui.xtream_home

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.viewModelScope
import com.iptvplayer.m3u.stream.model.dao.ServerDao
import com.iptvplayer.m3u.stream.model.entity.XtreamAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import hoang.dqm.codebase.base.viewmodel.BaseViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class XtreamHomeViewModel @Inject constructor(
    private val serverDao: ServerDao,

): BaseViewModel() {
    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)
    }

    val listXtreamAuth: StateFlow<List<XtreamAuth>> = serverDao.getAll()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
}