package com.wfx.warungpos.feature.kitchen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wfx.warungpos.domain.model.OrderItem
import com.wfx.warungpos.domain.repository.BillRepository
import com.wfx.warungpos.domain.repository.OrderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class KitchenQueueRow(
    val orderItem: OrderItem,
    val sessionLabel: String,
)

data class KitchenQueueUiState(
    val rows: List<KitchenQueueRow> = emptyList(),
)

@HiltViewModel
class KitchenQueueViewModel @Inject constructor(
    private val orderRepository: OrderRepository,
    private val billRepository: BillRepository,
) : ViewModel() {

    val uiState: StateFlow<KitchenQueueUiState> = combine(
        orderRepository.observeQueue(),
        billRepository.observeOpenBills(),
    ) { items, bills ->
        val labelByBillId = bills.associate { it.id to it.sessionLabel }
        KitchenQueueUiState(
            rows = items.map { item -> KitchenQueueRow(item, labelByBillId[item.billId] ?: "Unknown") }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), KitchenQueueUiState())

    fun markDone(orderItemId: String) {
        viewModelScope.launch { orderRepository.markItemDone(orderItemId) }
    }
}
