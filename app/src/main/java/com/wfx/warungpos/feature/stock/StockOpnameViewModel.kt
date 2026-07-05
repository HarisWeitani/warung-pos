package com.wfx.warungpos.feature.stock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wfx.warungpos.core.common.SyncStatus
import com.wfx.warungpos.core.common.VarianceReason
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
        val filtered = value.filter { it.isDigit() || it == '.' }
        _uiState.update { state ->
            state.copy(lines = state.lines.map { if (it.stockItemId == stockItemId) it.copy(countedQty = filtered) else it }, error = null)
        }
    }

    fun onReasonChange(stockItemId: String, reason: VarianceReason) {
        _uiState.update { state ->
            state.copy(lines = state.lines.map { if (it.stockItemId == stockItemId) it.copy(reason = reason) else it }, error = null)
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
