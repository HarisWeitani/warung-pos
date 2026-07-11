package com.wfx.warungpos.feature.stock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wfx.warungpos.core.common.SessionManager
import com.wfx.warungpos.core.common.SyncStatus
import com.wfx.warungpos.core.util.DateUtil
import com.wfx.warungpos.core.util.UuidGenerator
import com.wfx.warungpos.core.util.filterDecimalInput
import com.wfx.warungpos.domain.model.StockBatch
import com.wfx.warungpos.domain.model.StockItem
import com.wfx.warungpos.domain.repository.StockRepository
import com.wfx.warungpos.domain.usecase.stock.ReceiveStockBatchUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class StockBatchFormState(
    val isOpen: Boolean = false,
    val stockItemId: String? = null,
    val qty: String = "",
    val costPerUnit: String = "",
    val error: String? = null,
)

data class StockBatchUiState(
    val items: List<StockItem> = emptyList(),
    val batches: List<StockBatch> = emptyList(),
    val form: StockBatchFormState = StockBatchFormState(),
) {
    val itemNames: Map<String, String> get() = items.associate { it.id to it.name }
}

@HiltViewModel
class StockBatchViewModel @Inject constructor(
    private val stockRepository: StockRepository,
    private val receiveStockBatchUseCase: ReceiveStockBatchUseCase,
    private val sessionManager: SessionManager,
) : ViewModel() {

    private val _form = MutableStateFlow(StockBatchFormState())

    val uiState: StateFlow<StockBatchUiState> = combine(
        stockRepository.observeAllItems(),
        stockRepository.observeAllBatches(),
        _form,
    ) { items, batches, form ->
        StockBatchUiState(items = items.sortedBy { it.name }, batches = batches, form = form)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StockBatchUiState())

    fun showAddSheet() {
        val firstItemId = uiState.value.items.firstOrNull()?.id
        _form.value = StockBatchFormState(isOpen = true, stockItemId = firstItemId)
    }

    fun dismissSheet() {
        _form.value = StockBatchFormState()
    }

    fun onStockItemChange(id: String) = _form.update { it.copy(stockItemId = id, error = null) }
    fun onQtyChange(value: String) =
        _form.update { it.copy(qty = filterDecimalInput(value), error = null) }
    fun onCostChange(value: String) = _form.update { it.copy(costPerUnit = value.filter { c -> c.isDigit() }, error = null) }

    fun save() {
        viewModelScope.launch {
            val form = _form.value
            val stockItemId = form.stockItemId ?: return@launch
            val now = DateUtil.nowEpochMs()
            val batch = StockBatch(
                id = UuidGenerator.generate(),
                stockItemId = stockItemId,
                qty = form.qty.toDoubleOrNull() ?: 0.0,
                costPerUnit = form.costPerUnit.toLongOrNull() ?: 0L,
                receivedAt = now,
                expiresAt = null,
                updatedAt = now,
                syncStatus = SyncStatus.PENDING,
                deviceId = sessionManager.deviceId,
            )
            receiveStockBatchUseCase(batch)
                .onSuccess { dismissSheet() }
                .onFailure { e -> _form.update { it.copy(error = e.message) } }
        }
    }
}
