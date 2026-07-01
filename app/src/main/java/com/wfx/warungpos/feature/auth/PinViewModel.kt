package com.wfx.warungpos.feature.auth

import androidx.lifecycle.ViewModel
import com.wfx.warungpos.core.common.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

private const val MIN_PIN_LENGTH = 4

@HiltViewModel
class PinViewModel @Inject constructor(
    private val sessionManager: SessionManager,
) : ViewModel() {

    enum class Mode { REGISTER, UNLOCK }

    data class UiState(
        val mode: Mode,
        val username: String = "",
        val pin: String = "",
        val confirmPin: String = "",
        val existingUsername: String = "",
        val error: String? = null,
    )

    private val _uiState = MutableStateFlow(
        UiState(
            mode = if (sessionManager.isRegistered) Mode.UNLOCK else Mode.REGISTER,
            existingUsername = sessionManager.username.value,
        )
    )
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    fun onUsernameChange(value: String) = _uiState.update { it.copy(username = value, error = null) }

    fun onPinChange(value: String) =
        _uiState.update { it.copy(pin = value.filter { c -> c.isDigit() }, error = null) }

    fun onConfirmPinChange(value: String) =
        _uiState.update { it.copy(confirmPin = value.filter { c -> c.isDigit() }, error = null) }

    fun submit() {
        val state = _uiState.value
        when (state.mode) {
            Mode.REGISTER -> {
                if (state.username.isBlank()) {
                    _uiState.update { it.copy(error = "Please enter a username") }
                    return
                }
                if (state.pin.length < MIN_PIN_LENGTH) {
                    _uiState.update { it.copy(error = "PIN must be at least $MIN_PIN_LENGTH digits") }
                    return
                }
                if (state.pin != state.confirmPin) {
                    _uiState.update { it.copy(error = "PINs do not match") }
                    return
                }
                sessionManager.register(state.username, state.pin)
            }
            Mode.UNLOCK -> {
                if (!sessionManager.unlock(state.pin)) {
                    _uiState.update { it.copy(pin = "", error = "Incorrect PIN") }
                }
            }
        }
    }
}
