package com.wfx.warungpos.feature.shift

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wfx.warungpos.domain.exception.InsufficientPermissionsException
import com.wfx.warungpos.domain.exception.OpenBillsBlockShiftCloseException
import com.wfx.warungpos.domain.model.Bill
import com.wfx.warungpos.domain.model.Shift
import com.wfx.warungpos.domain.repository.BillRepository
import com.wfx.warungpos.domain.repository.ExpenseRepository
import com.wfx.warungpos.domain.repository.ReportRepository
import com.wfx.warungpos.domain.repository.ShiftRepository
import com.wfx.warungpos.domain.usecase.shift.CloseShiftUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ShiftCloseViewModel @Inject constructor(
    private val shiftRepository: ShiftRepository,
    private val reportRepository: ReportRepository,
    private val expenseRepository: ExpenseRepository,
    private val billRepository: BillRepository,
    private val closeShiftUseCase: CloseShiftUseCase,
) : ViewModel() {

    /** DEFECT-016: a shift other than the current one that's still OPEN — most often left
     * behind by another device. Surfaced here (rather than silently ignored) so it, and any
     * bill still attached to it, is never permanently unreachable. */
    data class OtherOpenShift(
        val shift: Shift,
        val revenue: Long = 0L,
        val expenses: Long = 0L,
        val openBillCount: Int = 0,
        val closingFloat: String = "",
        val isClosing: Boolean = false,
        val error: String? = null,
    )

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
        val otherOpenShifts: List<OtherOpenShift> = emptyList(),
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            // Combined (not two independent collectors) so "other shifts" is always computed
            // against the *same* emission's current-shift id — collecting separately risked a
            // window where the current shift briefly appeared duplicated in the others list too.
            combine(
                shiftRepository.observeOpenShift(),
                shiftRepository.observeAllOpenShifts(),
            ) { current, all -> current to all }.collect { (current, all) ->
                _uiState.update { it.copy(shift = current) }
                if (current != null) loadSummary(current.id)
                loadOtherShiftSummaries(all.filter { it.id != current?.id })
            }
        }
    }

    private suspend fun loadSummary(shiftId: String) {
        val revenue = reportRepository.getTotalRevenueForShift(shiftId)
        val expenses = expenseRepository.totalForShift(shiftId)
        val txCount = reportRepository.getTransactionCountForShift(shiftId)
        _uiState.update { it.copy(totalRevenue = revenue, totalExpenses = expenses, transactionCount = txCount) }
    }

    private suspend fun loadOtherShiftSummaries(others: List<Shift>) {
        val previous = _uiState.value.otherOpenShifts
        val rows = others.map { s ->
            // Preserve any in-progress float text/error for a row that already existed —
            // otherwise every reactive re-emission (e.g. the *current* shift's revenue changing)
            // would wipe out whatever the owner was mid-typing into a different row.
            val existing = previous.find { it.shift.id == s.id }
            OtherOpenShift(
                shift = s,
                revenue = reportRepository.getTotalRevenueForShift(s.id),
                expenses = expenseRepository.totalForShift(s.id),
                openBillCount = billRepository.getOpenBillsForShift(s.id).size,
                closingFloat = existing?.closingFloat ?: "",
            )
        }
        _uiState.update { it.copy(otherOpenShifts = rows) }
    }

    fun onFloatChange(value: String) {
        _uiState.update { it.copy(closingFloat = value.filter { c -> c.isDigit() }, error = null) }
    }

    fun onOtherShiftFloatChange(shiftId: String, value: String) {
        val filtered = value.filter { it.isDigit() }
        _uiState.update { state ->
            state.copy(
                otherOpenShifts = state.otherOpenShifts.map {
                    if (it.shift.id == shiftId) it.copy(closingFloat = filtered, error = null) else it
                },
            )
        }
    }

    fun closeShift() {
        // Same rapid-double-tap guard as closeOtherShift — see its doc comment.
        if (_uiState.value.isLoading) return
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

    /** Closes an [OtherOpenShift] row in place — no navigation, unlike [closeShift]. The row
     * disappears on its own once [shiftRepository]'s reactive query picks up the CLOSED status.
     *
     * Edge case: unlike opening a shift ([ShiftDao.openIfNoneOpen]), closing one is a plain
     * check-then-act with no DB-level atomicity — nothing previously stopped the same shift from
     * being closed twice (e.g. a rapid double-tap), which would run [closeShiftUseCase] and
     * [GenerateZReportUseCase] a second time for a shift already CLOSED. The [isClosing] check
     * below is enough to prevent that in practice: Android dispatches click events serially on
     * the main thread, so a second call observes the [isClosing] flag the first call already set
     * before it can launch its own attempt. This does not close the (much rarer, informational-
     * only) case of two *different devices* tapping Close on the same shift within the sync
     * propagation window — that residual risk is accepted, consistent with this app's offline-
     * first design not enforcing any cross-device lock on shift state. */
    fun closeOtherShift(shiftId: String) {
        val row = _uiState.value.otherOpenShifts.find { it.shift.id == shiftId } ?: return
        if (row.isClosing) return
        viewModelScope.launch {
            updateOtherShiftRow(shiftId) { it.copy(isClosing = true, error = null) }
            closeShiftUseCase(row.closingFloat.toLongOrNull() ?: 0L, shiftId)
                .onFailure { e ->
                    val message = when (e) {
                        is OpenBillsBlockShiftCloseException ->
                            "${e.openBills.size} open bill(s) must be resolved first"
                        is InsufficientPermissionsException -> "Owner access required"
                        else -> e.message
                    }
                    updateOtherShiftRow(shiftId) { it.copy(isClosing = false, error = message) }
                }
        }
    }

    private fun updateOtherShiftRow(shiftId: String, transform: (OtherOpenShift) -> OtherOpenShift) {
        _uiState.update { state ->
            state.copy(
                otherOpenShifts = state.otherOpenShifts.map { if (it.shift.id == shiftId) transform(it) else it },
            )
        }
    }
}
