package com.wfx.warungpos.feature.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wfx.warungpos.core.util.DateUtil
import com.wfx.warungpos.domain.model.BestSeller
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

data class ReportsUiState(
    val shift: Shift? = null,
    val totalRevenue: Long = 0L,
    val totalExpenses: Long = 0L,
    val transactionCount: Int = 0,
    val bestSellers: List<BestSeller> = emptyList(),
    val paymentBreakdown: List<PaymentBreakdown> = emptyList(),
    val isLoading: Boolean = false,
)

@HiltViewModel
class ReportsViewModel @Inject constructor(
    private val shiftRepository: ShiftRepository,
    private val reportRepository: ReportRepository,
    private val paymentRepository: PaymentRepository,
    private val expenseRepository: ExpenseRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReportsUiState())
    val uiState: StateFlow<ReportsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            shiftRepository.observeOpenShift().collect { shift ->
                _uiState.update { it.copy(shift = shift) }
                if (shift != null) loadData(shift.id)
            }
        }
    }

    fun refresh() {
        val shiftId = _uiState.value.shift?.id ?: return
        viewModelScope.launch { loadData(shiftId) }
    }

    private suspend fun loadData(shiftId: String) {
        _uiState.update { it.copy(isLoading = true) }
        val revenue = reportRepository.getTotalRevenueForShift(shiftId)
        val expenses = expenseRepository.totalForShift(shiftId)
        val txCount = reportRepository.getTransactionCountForShift(shiftId)
        val now = DateUtil.nowEpochMs()
        val bestSellers = reportRepository.getBestSellers(
            startEpoch = DateUtil.startOfDay(now),
            endEpoch = DateUtil.endOfDay(now),
            limit = 5,
        )
        val payBreakdown = paymentRepository.getPaymentBreakdownForShift(shiftId)
        _uiState.update {
            it.copy(
                isLoading = false,
                totalRevenue = revenue,
                totalExpenses = expenses,
                transactionCount = txCount,
                bestSellers = bestSellers,
                paymentBreakdown = payBreakdown,
            )
        }
    }
}
