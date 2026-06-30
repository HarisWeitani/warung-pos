package com.wfx.warungpos.feature.order

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wfx.warungpos.core.common.BillStatus
import com.wfx.warungpos.core.common.BillType
import com.wfx.warungpos.core.common.SessionManager
import com.wfx.warungpos.core.common.SyncStatus
import com.wfx.warungpos.core.util.DateUtil
import com.wfx.warungpos.core.util.UuidGenerator
import com.wfx.warungpos.domain.model.Bill
import com.wfx.warungpos.domain.model.Shift
import com.wfx.warungpos.domain.repository.BillRepository
import com.wfx.warungpos.domain.repository.ShiftRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OrderUiState(
    val openBills: List<Bill> = emptyList(),
    val openShift: Shift? = null,
)

@HiltViewModel
class OrderViewModel @Inject constructor(
    private val billRepository: BillRepository,
    private val shiftRepository: ShiftRepository,
    private val sessionManager: SessionManager,
) : ViewModel() {

    val uiState: StateFlow<OrderUiState> = combine(
        billRepository.observeOpenBills(),
        shiftRepository.observeOpenShift(),
    ) { bills, shift ->
        OrderUiState(openBills = bills, openShift = shift)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), OrderUiState())

    private val _navToBill = Channel<String>(Channel.BUFFERED)
    val navToBill: Flow<String> = _navToBill.receiveAsFlow()

    fun createUpfrontBill() {
        viewModelScope.launch {
            val shift = shiftRepository.getOpenShift() ?: return@launch
            val now = DateUtil.nowEpochMs()
            val bill = Bill(
                id = UuidGenerator.generate(),
                tableId = null,
                type = BillType.UPFRONT,
                status = BillStatus.OPEN,
                sessionLabel = "Counter - ${DateUtil.toDisplayTime(now)}",
                createdAt = now,
                paidAt = null,
                subtotal = 0L,
                discountTotal = 0L,
                grandTotal = 0L,
                note = null,
                shiftId = shift.id,
                voidReason = null,
                voidedBy = null,
                updatedAt = now,
                syncStatus = SyncStatus.PENDING,
                deviceId = sessionManager.deviceId,
            )
            billRepository.saveBill(bill)
            _navToBill.send(bill.id)
        }
    }
}
