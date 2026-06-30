package com.wfx.warungpos.feature.payment

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.wfx.warungpos.core.common.OrderItemStatus
import com.wfx.warungpos.core.navigation.PaymentRoute
import com.wfx.warungpos.domain.model.Bill
import com.wfx.warungpos.domain.model.OrderItem
import com.wfx.warungpos.domain.model.PaymentMethod
import com.wfx.warungpos.domain.repository.BillRepository
import com.wfx.warungpos.domain.repository.OrderRepository
import com.wfx.warungpos.domain.repository.PaymentRepository
import com.wfx.warungpos.domain.usecase.payment.CalculateChangeUseCase
import com.wfx.warungpos.domain.usecase.payment.PaymentRow
import com.wfx.warungpos.domain.usecase.payment.ProcessPaymentUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PaymentUiState(
    val bill: Bill? = null,
    val orderItems: List<OrderItem> = emptyList(),
    val paymentMethods: List<PaymentMethod> = emptyList(),
    val selectedMethodId: String? = null,
    val tenderAmount: String = "",
    val change: Long = 0L,
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class PaymentViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val billRepository: BillRepository,
    private val orderRepository: OrderRepository,
    private val paymentRepository: PaymentRepository,
    private val processPaymentUseCase: ProcessPaymentUseCase,
    private val calculateChangeUseCase: CalculateChangeUseCase,
) : ViewModel() {

    private val billId: String = savedStateHandle.toRoute<PaymentRoute>().billId

    private val _uiState = MutableStateFlow(PaymentUiState())
    val uiState: StateFlow<PaymentUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                billRepository.observeBillById(billId),
                orderRepository.observeOrderItems(billId),
                paymentRepository.observeActivePaymentMethods(),
            ) { bill, items, methods ->
                Triple(bill, items, methods)
            }.collect { (bill, items, methods) ->
                _uiState.update { state ->
                    val selectedId = state.selectedMethodId ?: methods.firstOrNull()?.id
                    val tender = state.tenderAmount.toLongOrNull() ?: 0L
                    state.copy(
                        bill = bill,
                        orderItems = items.filter { it.status != OrderItemStatus.VOID },
                        paymentMethods = methods,
                        selectedMethodId = selectedId,
                        change = maxOf(0L, calculateChangeUseCase(tender, bill?.grandTotal ?: 0L)),
                    )
                }
            }
        }
    }

    fun selectMethod(id: String) {
        _uiState.update { it.copy(selectedMethodId = id) }
    }

    fun onTenderChange(value: String) {
        val filtered = value.filter { it.isDigit() }
        val tender = filtered.toLongOrNull() ?: 0L
        _uiState.update { state ->
            state.copy(
                tenderAmount = filtered,
                change = maxOf(0L, calculateChangeUseCase(tender, state.bill?.grandTotal ?: 0L)),
            )
        }
    }

    fun confirmPayment() {
        viewModelScope.launch {
            val state = _uiState.value
            val bill = state.bill ?: return@launch
            val methodId = state.selectedMethodId ?: return@launch
            val tenderLong = state.tenderAmount.toLongOrNull() ?: bill.grandTotal
            _uiState.update { it.copy(isLoading = true, error = null) }
            processPaymentUseCase(
                billId = billId,
                rows = listOf(PaymentRow(methodId = methodId, amount = bill.grandTotal, tenderedAmount = tenderLong)),
            )
                .onSuccess { _uiState.update { it.copy(isLoading = false, isSuccess = true) } }
                .onFailure { e -> _uiState.update { it.copy(isLoading = false, error = e.message) } }
        }
    }
}
