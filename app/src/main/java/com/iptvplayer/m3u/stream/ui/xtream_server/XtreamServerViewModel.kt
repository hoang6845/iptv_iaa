package com.iptvplayer.m3u.stream.ui.xtream_server

import androidx.lifecycle.viewModelScope
import com.iptvplayer.m3u.stream.R
import com.iptvplayer.m3u.stream.api.MovieRepository
import com.iptvplayer.m3u.stream.model.entity.LoginResponse
import com.iptvplayer.m3u.stream.model.entity.XtreamAccountState
import hoang.dqm.codebase.base.viewmodel.BaseViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class XtreamServerViewModel : BaseViewModel() {

    private val _accountState =
        MutableStateFlow<XtreamAccountState>(XtreamAccountState.Idle)

    val accountState: StateFlow<XtreamAccountState> =
        _accountState.asStateFlow()

    private var checkAccountJob: Job? = null

    fun checkAccount(
        repository: MovieRepository,
        username: String,
        password: String,
        urlServer: String,
        isDuplicate: Boolean
    ) {
        val safeUsername = username.trim()
        val safePassword = password.trim()
        val safeUrl = normalizeUrl(urlServer)

        if (_accountState.value is XtreamAccountState.Loading) {
            return
        }

        if (isDuplicate) {
            _accountState.value =
                XtreamAccountState.Error(context.getString(R.string.text_this_account_already_exists))
            return
        }

        if (safeUrl.isBlank()) {
            _accountState.value =
                XtreamAccountState.Error(context.getString(R.string.text_please_enter_server_url))
            return
        }

        if (!safeUrl.startsWith("http://") && !safeUrl.startsWith("https://")) {
            _accountState.value =
                XtreamAccountState.Error(context.getString(R.string.text_invalid_server_url))
            return
        }

        if (safeUsername.isBlank()) {
            _accountState.value =
                XtreamAccountState.Error(context.getString(R.string.text_please_enter_username))
            return
        }

        if (safePassword.isBlank()) {
            _accountState.value =
                XtreamAccountState.Error(context.getString(R.string.text_please_enter_password))
            return
        }

        checkAccountJob?.cancel()
        checkAccountJob = viewModelScope.launch {
            _accountState.value = XtreamAccountState.Loading

            try {
                repository.checkUser(safeUsername, safePassword)
                    .onSuccess { login ->
                        val userInfo = login.userInfo

                        if (userInfo.auth != 1) {
                            _accountState.value =
                                XtreamAccountState.Error("Incorrect username or password")
                            return@onSuccess
                        }

                        if (!userInfo.status.equals("Active", ignoreCase = true)) {
                            _accountState.value =
                                XtreamAccountState.Error("Account is inactive")
                            return@onSuccess
                        }

                        _accountState.value =
                            XtreamAccountState.Success(
                                LoginResponse(
                                    userInfo = userInfo,
                                    serverInfo = login.serverInfo,
                                    url = safeUrl
                                )
                            )
                    }
                    .onFailure { error ->
                        _accountState.value =
                            XtreamAccountState.Error(
                                error.message ?: "Unable to connect to the server"
                            )
                    }
            } catch (e: Exception) {
                _accountState.value =
                    XtreamAccountState.Error(
                        e.message ?: "Unable to connect to the server"
                    )
            }
        }
    }

    fun resetAccountState() {
        _accountState.value = XtreamAccountState.Idle
    }

    override fun onCleared() {
        checkAccountJob?.cancel()
        super.onCleared()
    }

    private fun normalizeUrl(url: String): String {
        val trimmedUrl = url.trim()
        if (trimmedUrl.isBlank()) return ""

        return if (trimmedUrl.endsWith("/")) {
            trimmedUrl
        } else {
            "$trimmedUrl/"
        }
    }
}