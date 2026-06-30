package com.wfx.warungpos.feature.shift

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wfx.warungpos.domain.usecase.shift.OpenShiftUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ShiftOpenViewModel @Inject constructor(
    private val openShiftUseCase: OpenShiftUseCase,
) : ViewModel() {

    data class UiState(
        val openingFloat: String = "",
        val isLoading: Boolean = false,
        val isSuccess: Boolean = false,
        val error: String? = null,
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    fun onFloatChange(value: String) {
        _uiState.update { it.copy(openingFloat = value.filter { c -> c.isDigit() }, error = null) }
    }

    fun openShift() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            openShiftUseCase(_uiState.value.openingFloat.toLongOrNull() ?: 0L)
                .onSuccess { _uiState.update { it.copy(isLoading = false, isSuccess = true) } }
                .onFailure { e ->
                    // Already open → treat as success (idempotent)
                    if (e is IllegalStateException) {
                        _uiState.update { it.copy(isLoading = false, isSuccess = true) }
                    } else {
                        _uiState.update { it.copy(isLoading = false, error = e.message) }
                    }
                }
        }
    }
}
