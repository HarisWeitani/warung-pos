package com.wfx.warungpos.feature.shift

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wfx.warungpos.domain.exception.InsufficientPermissionsException
import com.wfx.warungpos.domain.exception.OpenBillsBlockShiftCloseException
import com.wfx.warungpos.domain.model.Bill
import com.wfx.warungpos.domain.model.Shift
import com.wfx.warungpos.domain.repository.ExpenseRepository
import com.wfx.warungpos.domain.repository.ReportRepository
import com.wfx.warungpos.domain.repository.ShiftRepository
import com.wfx.warungpos.domain.usecase.shift.CloseShiftUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ShiftCloseViewModel @Inject constructor(
    private val shiftRepository: ShiftRepository,
    private val reportRepository: ReportRepository,
    private val expenseRepository: ExpenseRepository,
    private val closeShiftUseCase: CloseShiftUseCase,
) : ViewModel() {

    data class UiState(
        val shift: Shift? = null,
        val totalRevenue: Long = 0L,
        val totalExpenses: Long = 0L,
        val transactionCount: Int = 0,
        val openBills: List<Bill> = emptyList(),
        val closingFloat: String = "",
        val isLoading: Boolean = false,
        val closedShiftId: String? = null,
        val error: String? = null,
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            shiftRepository.observeOpenShift().collect { shift ->
                _uiState.update { it.copy(shift = shift) }
                if (shift != null) loadSummary(shift.id)
            }
        }
    }

    fun onFloatChange(value: String) {
        _uiState.update { it.copy(closingFloat = value.filter { c -> c.isDigit() }, error = null) }
    }

    private suspend fun loadSummary(shiftId: String) {
        val revenue = reportRepository.getTotalRevenueForShift(shiftId)
        val expenses = expenseRepository.totalForShift(shiftId)
        val txCount = reportRepository.getTransactionCountForShift(shiftId)
        _uiState.update { it.copy(totalRevenue = revenue, totalExpenses = expenses, transactionCount = txCount) }
    }

    fun closeShift() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            closeShiftUseCase(_uiState.value.closingFloat.toLongOrNull() ?: 0L)
                .onSuccess { shiftId ->
                    _uiState.update { it.copy(isLoading = false, closedShiftId = shiftId) }
                }
                .onFailure { e ->
                    when (e) {
                        is OpenBillsBlockShiftCloseException ->
                            _uiState.update { it.copy(isLoading = false, openBills = e.openBills) }
                        is InsufficientPermissionsException ->
                            _uiState.update { it.copy(isLoading = false, error = "Owner access required") }
                        else ->
                            _uiState.update { it.copy(isLoading = false, error = e.message) }
                    }
                }
        }
    }
}
