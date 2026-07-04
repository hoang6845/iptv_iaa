package com.iptvplayer.m3u.stream.ui.passcode_xtream

import com.iptvplayer.m3u.stream.utils.PasscodeManagerXtream
import dagger.hilt.android.lifecycle.HiltViewModel
import hoang.dqm.codebase.base.viewmodel.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class PasscodeXtreamViewModel @Inject constructor(
    private val passcodeManager: PasscodeManagerXtream
) : BaseViewModel() {

    private val PIN_LENGTH = 4

    private var pendingNewPin: String = ""
    private val _uiState = MutableStateFlow(PasscodeUiState())
    val uiState: StateFlow<PasscodeUiState> = _uiState.asStateFlow()

    private fun resolveInitialState(): PasscodeUiState {
        val initialMode = if (passcodeManager.hasPasscode()) PasscodeMode.VERIFY else PasscodeMode.SET_NEW
        return PasscodeUiState(mode = initialMode)
    }

    fun startChangePasscode() {
        pendingNewPin = ""
        _uiState.value = PasscodeUiState(mode = PasscodeMode.VERIFY_OLD)
    }
    init {
        val mode = if (passcodeManager.hasPasscode()) PasscodeMode.VERIFY else PasscodeMode.SET_NEW
        _uiState.update { it.copy(mode = mode) }
    }

    fun onBiometricSuccess() {
        when (_uiState.value.mode) {
            PasscodeMode.VERIFY     -> _uiState.update { it.copy(event = PasscodeEvent.Success) }
            PasscodeMode.VERIFY_OLD -> _uiState.value = PasscodeUiState(mode = PasscodeMode.SET_NEW)
            else                    -> Unit
        }
    }

    fun onDigitPressed(digit: String) {
        val current = _uiState.value
        if (current.pin.length >= PIN_LENGTH) return

        val newPin = current.pin + digit
        _uiState.update { it.copy(pin = newPin, event = null) }

        if (newPin.length == PIN_LENGTH) {
            processFullPin(newPin, current.mode)
        }
    }

    fun onBackspacePressed() {
        val current = _uiState.value
        if (current.pin.isEmpty()) return
        _uiState.update { it.copy(pin = current.pin.dropLast(1), event = null) }
    }

    fun consumeEvent() {
        _uiState.update { it.copy(event = null) }
    }

//    fun resetPin() {
//        _uiState.update {
//            it.copy(
//                mode = PasscodeMode.SET_NEW,
//                firstPin = "",
//                pin = "",
//                event = null
//            )
//        }
//    }

    fun resetPin() {
        pendingNewPin = ""
        val resetMode = if (passcodeManager.hasPasscode()) PasscodeMode.VERIFY_OLD else PasscodeMode.SET_NEW
        _uiState.value = PasscodeUiState(mode = resetMode)
    }


    private fun processFullPin(pin: String, mode: PasscodeMode) {
        when (mode) {

            PasscodeMode.VERIFY -> {
                if (pin == passcodeManager.getPasscode()) {
                    _uiState.update { it.copy(event = PasscodeEvent.Success) }
                } else {
                    _uiState.update { it.copy(pin = "", event = PasscodeEvent.WrongPin) }
                }
            }

            PasscodeMode.VERIFY_OLD -> {
                if (pin == passcodeManager.getPasscode()) {
                    _uiState.value = PasscodeUiState(mode = PasscodeMode.SET_NEW)
                } else {
                    _uiState.update { it.copy(pin = "", event = PasscodeEvent.WrongOldPin) }
                }
            }

            PasscodeMode.SET_NEW -> {
                pendingNewPin = pin
                _uiState.value = PasscodeUiState(mode = PasscodeMode.CONFIRM_NEW)
            }

            PasscodeMode.CONFIRM_NEW -> {
                if (pin == pendingNewPin) {
                    passcodeManager.savePasscode(pin)
                    pendingNewPin = ""
                    _uiState.update { it.copy(event = PasscodeEvent.ChangeSuccess) }
                } else {
                    pendingNewPin = ""
                    _uiState.update {
                        it.copy(pin = "", event = PasscodeEvent.PinMismatch)
                    }
                }
            }
        }
    }
}

// ── Data classes ─────────────────────────────────────────────────────────────

data class PasscodeUiState(
    val mode: PasscodeMode = PasscodeMode.VERIFY,
    val pin: String = "",
    val firstPin: String = "",
    val event: PasscodeEvent? = null
)

enum class PasscodeMode {
    VERIFY,       // Has saved PIN → ask user to enter it
    SET_NEW,      // No PIN yet → ask user to create one
    CONFIRM_NEW,   // Re-enter to confirm new PIN
    VERIFY_OLD
}

sealed class PasscodeEvent {
    object Success      : PasscodeEvent()
    object WrongPin     : PasscodeEvent()
    object PinMismatch  : PasscodeEvent()
    object WrongOldPin : PasscodeEvent()
    object ChangeSuccess : PasscodeEvent()
}