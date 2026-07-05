package com.wfx.warungpos.feature.stock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wfx.warungpos.core.common.SessionManager
import com.wfx.warungpos.core.common.SyncStatus
import com.wfx.warungpos.core.util.DateUtil
import com.wfx.warungpos.core.util.UuidGenerator
import com.wfx.warungpos.domain.model.StockItem
import com.wfx.warungpos.domain.repository.StockRepository
import com.wfx.warungpos.domain.usecase.stock.UpsertStockItemUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class StockFormState(
    val isOpen: Boolean = false,
    val editingId: String? = null,
    val name: String = "",
    val unit: String = "",
    val reorderPoint: String = "",
    val error: String? = null,
)

data class StockUiState(
    val items: List<StockItem> = emptyList(),
    val form: StockFormState = StockFormState(),
)

@HiltViewModel
class StockViewModel @Inject constructor(
    private val stockRepository: StockRepository,
    private val upsertStockItemUseCase: UpsertStockItemUseCase,
    private val sessionManager: SessionManager,
) : ViewModel() {

    private val _form = MutableStateFlow(StockFormState())

    val uiState: StateFlow<StockUiState> = combine(
        stockRepository.observeAllItems(),
        _form,
    ) { items, form ->
        StockUiState(items = items.sortedBy { it.name }, form = form)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StockUiState())

    fun showAddSheet() {
        _form.value = StockFormState(isOpen = true)
    }

    fun showEditSheet(item: StockItem) {
        _form.value = StockFormState(
            isOpen = true,
            editingId = item.id,
            name = item.name,
            unit = item.unit,
            reorderPoint = formatQty(item.reorderPoint),
        )
    }

    fun dismissSheet() {
        _form.value = StockFormState()
    }

    fun onNameChange(value: String) = _form.update { it.copy(name = value, error = null) }
    fun onUnitChange(value: String) = _form.update { it.copy(unit = value, error = null) }
    fun onReorderPointChange(value: String) =
        _form.update { it.copy(reorderPoint = value.filter { c -> c.isDigit() || c == '.' }, error = null) }

    fun save() {
        viewModelScope.launch {
            val form = _form.value
            val reorderPoint = form.reorderPoint.toDoubleOrNull() ?: 0.0
            val existing = form.editingId?.let { id -> uiState.value.items.firstOrNull { it.id == id } }
            val now = DateUtil.nowEpochMs()
            val item = StockItem(
                id = form.editingId ?: UuidGenerator.generate(),
                name = form.name.trim(),
                unit = form.unit.trim(),
                currentQty = existing?.currentQty ?: 0.0,
                reorderPoint = reorderPoint,
                updatedAt = now,
                syncStatus = SyncStatus.PENDING,
                deviceId = sessionManager.deviceId,
            )
            upsertStockItemUseCase(item)
                .onSuccess { dismissSheet() }
                .onFailure { e -> _form.update { it.copy(error = e.message) } }
        }
    }
}

internal fun formatQty(qty: Double): String =
    if (qty == qty.toLong().toDouble()) qty.toLong().toString() else qty.toString()
