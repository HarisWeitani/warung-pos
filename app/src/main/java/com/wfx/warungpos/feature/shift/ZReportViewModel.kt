package com.wfx.warungpos.feature.shift

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.wfx.warungpos.core.navigation.ZReportRoute
import com.wfx.warungpos.data.local.mapper.toSnapshot
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
        val voidCount: Int = 0,
        val voidValue: Long = 0L,
        val countedCash: Long = 0L,
        val expectedCash: Long = 0L,
        val variance: Long = 0L,
        /** Whether the figures above came from the persisted immutable snapshot (the normal
         * case for any shift that was actually closed) vs. a live fallback re-derivation (only
         * reached if a closed shift somehow has no Z-report row — see DEFECT-010). */
        val isFromPersistedSnapshot: Boolean = false,
        val isLoading: Boolean = true,
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch { load() }
    }

    private suspend fun load() {
        val shift = shiftRepository.getRecentShifts(50).firstOrNull { it.id == shiftId }

        // DEFECT-010: the Z-report must show the immutable snapshot captured at close time
        // (GenerateZReportUseCase), not figures re-derived live from the DB's current state —
        // in particular countedCash/expectedCash/variance only ever existed in that snapshot,
        // never anywhere queryable live.
        val snapshot = shiftRepository.getZReport(shiftId)?.toSnapshot()
        if (snapshot != null) {
            _uiState.update {
                it.copy(
                    shift = shift,
                    totalRevenue = snapshot.revenue,
                    totalExpenses = snapshot.expenses,
                    transactionCount = snapshot.transactions,
                    paymentBreakdown = snapshot.paymentBreakdown,
                    voidCount = snapshot.voidCount,
                    voidValue = snapshot.voidValue,
                    countedCash = snapshot.countedCash,
                    expectedCash = snapshot.expectedCash,
                    variance = snapshot.variance,
                    isFromPersistedSnapshot = true,
                    isLoading = false,
                )
            }
            return
        }

        // Fallback only: a closed shift with no Z-report row (shouldn't normally happen — both
        // CloseShiftUseCase and EnsureDayOpenUseCase's auto-close always generate one). Cash
        // reconciliation fields have no live source and stay at their zero defaults.
        val revenue = reportRepository.getTotalRevenueForShift(shiftId)
        val expenses = expenseRepository.totalForShift(shiftId)
        val txCount = reportRepository.getTransactionCountForShift(shiftId)
        val breakdown = paymentRepository.getPaymentBreakdownForShift(shiftId)
        val voidStats = reportRepository.getVoidStatsForShift(shiftId)
        _uiState.update {
            it.copy(
                shift = shift,
                totalRevenue = revenue,
                totalExpenses = expenses,
                transactionCount = txCount,
                paymentBreakdown = breakdown,
                voidCount = voidStats.count,
                voidValue = voidStats.totalValue,
                isFromPersistedSnapshot = false,
                isLoading = false,
            )
        }
    }
}
