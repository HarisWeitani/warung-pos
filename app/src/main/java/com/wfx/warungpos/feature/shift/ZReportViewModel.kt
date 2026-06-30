package com.wfx.warungpos.feature.shift

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.wfx.warungpos.core.navigation.ZReportRoute
import com.wfx.warungpos.domain.model.PaymentBreakdown
import com.wfx.warungpos.domain.model.Shift
import com.wfx.warungpos.domain.repository.ExpenseRepository
import com.wfx.warungpos.domain.repository.PaymentRepository
import com.wfx.warungpos.domain.repository.ReportRepository
import com.wfx.warungpos.domain.repository.ShiftRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ZReportViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val shiftRepository: ShiftRepository,
    private val reportRepository: ReportRepository,
    private val paymentRepository: PaymentRepository,
    private val expenseRepository: ExpenseRepository,
) : ViewModel() {

    private val shiftId: String = savedStateHandle.toRoute<ZReportRoute>().shiftId

    data class UiState(
        val shift: Shift? = null,
        val totalRevenue: Long = 0L,
        val totalExpenses: Long = 0L,
        val transactionCount: Int = 0,
        val paymentBreakdown: List<PaymentBreakdown> = emptyList(),
        val isLoading: Boolean = true,
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch { load() }
    }

    private suspend fun load() {
        val shift = shiftRepository.getRecentShifts(50).firstOrNull { it.id == shiftId }
        val revenue = reportRepository.getTotalRevenueForShift(shiftId)
        val expenses = expenseRepository.totalForShift(shiftId)
        val txCount = reportRepository.getTransactionCountForShift(shiftId)
        val breakdown = paymentRepository.getPaymentBreakdownForShift(shiftId)
        _uiState.update {
            it.copy(
                shift = shift,
                totalRevenue = revenue,
                totalExpenses = expenses,
                transactionCount = txCount,
                paymentBreakdown = breakdown,
                isLoading = false,
            )
        }
    }
}
