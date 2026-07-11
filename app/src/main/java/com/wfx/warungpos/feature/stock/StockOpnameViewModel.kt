package com.wfx.warungpos.feature.stock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wfx.warungpos.core.common.SyncStatus
import com.wfx.warungpos.core.common.VarianceReason
import com.wfx.warungpos.core.util.filterDecimalInput
import com.wfx.warungpos.domain.model.StockOpname
import com.wfx.warungpos.domain.model.StockOpnameLine
import com.wfx.warungpos.domain.repository.StockRepository
import com.wfx.warungpos.domain.usecase.stock.StartStockOpnameUseCase
import com.wfx.warungpos.domain.usecase.stock.SubmitStockOpnameUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OpnameLineUi(
    val lineId: String,
    val stockItemId: String,
    val name: String,
    val unit: String,
    val systemQty: Double,
    val countedQty: String,
    val reason: VarianceReason?,
) {
    val variance: Double get() = (countedQty.toDoubleOrNull() ?: systemQty) - systemQty
}

data class StockOpnameUiState(
    val inProgress: StockOpname? = null,
    val lines: List<OpnameLineUi> = emptyList(),
    val isLoading: Boolean = false,
    val isSubmitting: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class StockOpnameViewModel @Inject constructor(
    private val stockRepository: StockRepository,
    private val startStockOpnameUseCase: StartStockOpnameUseCase,
    private val submitStockOpnameUseCase: SubmitStockOpnameUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(StockOpnameUiState())
    val uiState: StateFlow<StockOpnameUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            stockRepository.observeInProgressOpname().collect { opname ->
                if (opname == null) {
                    _uiState.value = StockOpnameUiState()
                } else if (_uiState.value.inProgress?.id != opname.id) {
                    loadLines(opname)
                }
            }
        }
    }

    private suspend fun loadLines(opname: StockOpname) {
        val lines = stockRepository.getLinesForOpname(opname.id)
        val items = stockRepository.getAllItemsOnce().associateBy { it.id }
        val lineUis = lines.map { line ->
            val item = items[line.stockItemId]
            OpnameLineUi(
                lineId = line.id,
                stockItemId = line.stockItemId,
                name = item?.name ?: line.stockItemId,
                unit = item?.unit ?: "",
                systemQty = line.systemQty,
                countedQty = formatQty(line.countedQty),
                reason = line.varianceReason,
            )
        }.sortedBy { it.name }
        _uiState.update { it.copy(inProgress = opname, lines = lineUis) }
    }

    fun startOpname() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val result = startStockOpnameUseCase()
            result.onFailure { e -> _uiState.update { it.copy(error = e.message) } }
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun onCountedQtyChange(stockItemId: String, value: String) {
        val filtered = filterDecimalInput(value)
        _uiState.update { state ->
            state.copy(lines = state.lines.map { if (it.stockItemId == stockItemId) it.copy(countedQty = filtered) else it }, error = null)
        }
        persistLineDraft(stockItemId)
    }

    fun onReasonChange(stockItemId: String, reason: VarianceReason) {
        _uiState.update { state ->
            state.copy(lines = state.lines.map { if (it.stockItemId == stockItemId) it.copy(reason = reason) else it }, error = null)
        }
        persistLineDraft(stockItemId)
    }

    /**
     * DEFECT-013: counted-quantity/reason edits used to live only in `_uiState` — navigating
     * away and back (even just to More and back) recreated this ViewModel, whose `init` block
     * reloads lines straight from the DB, silently discarding anything not yet submitted. Every
     * edit is now written through to the `stock_opname_lines` row immediately (the same
     * `saveLine` used at final submit), so `loadLines()` reads back the latest draft instead of
     * whatever was there when the opname started.
     *
     * Skipped when the counted-qty text doesn't parse yet (e.g. a bare "" while the user is
     * mid-retype, or a lone "." before they've typed a digit after it) — there's no valid Double
     * to persist in that instant, and persisting the fallback systemQty would overwrite a
     * previously-saved real count with a wrong value. The very next valid keystroke persists
     * normally, so only a genuinely-incomplete in-flight edit is ever at risk, not a completed one.
     */
    private fun persistLineDraft(stockItemId: String) {
        val opname = _uiState.value.inProgress ?: return
        val ui = _uiState.value.lines.firstOrNull { it.stockItemId == stockItemId } ?: return
        val countedQty = ui.countedQty.toDoubleOrNull() ?: return
        viewModelScope.launch {
            stockRepository.saveLine(
                StockOpnameLine(
                    id = ui.lineId,
                    opnameId = opname.id,
                    stockItemId = ui.stockItemId,
                    systemQty = ui.systemQty,
                    countedQty = countedQty,
                    variance = countedQty - ui.systemQty,
                    varianceReason = ui.reason,
                    updatedAt = 0L,
                    syncStatus = SyncStatus.PENDING,
                    deviceId = "",
                )
            )
        }
    }

    fun submit() {
        viewModelScope.launch {
            val state = _uiState.value
            val opname = state.inProgress ?: return@launch
            _uiState.update { it.copy(isSubmitting = true, error = null) }

            val domainLines = state.lines.map { ui ->
                StockOpnameLine(
                    id = ui.lineId,
                    opnameId = opname.id,
                    stockItemId = ui.stockItemId,
                    systemQty = ui.systemQty,
                    countedQty = ui.countedQty.toDoubleOrNull() ?: ui.systemQty,
                    variance = ui.variance,
                    varianceReason = ui.reason,
                    updatedAt = 0L,
                    syncStatus = SyncStatus.PENDING,
                    deviceId = "",
                )
            }
            submitStockOpnameUseCase(domainLines)
                .onSuccess { _uiState.value = StockOpnameUiState() }
                .onFailure { e -> _uiState.update { it.copy(isSubmitting = false, error = e.message) } }
        }
    }
}
