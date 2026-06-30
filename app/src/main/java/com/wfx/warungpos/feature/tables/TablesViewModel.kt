package com.wfx.warungpos.feature.tables

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
import com.wfx.warungpos.domain.model.Table
import com.wfx.warungpos.domain.repository.BillRepository
import com.wfx.warungpos.domain.repository.ShiftRepository
import com.wfx.warungpos.domain.repository.TableRepository
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

data class TableWithBill(val table: Table, val openBill: Bill?)

data class TablesUiState(
    val tables: List<TableWithBill> = emptyList(),
    val openShift: Shift? = null,
)

@HiltViewModel
class TablesViewModel @Inject constructor(
    private val tableRepository: TableRepository,
    private val billRepository: BillRepository,
    private val shiftRepository: ShiftRepository,
    private val sessionManager: SessionManager,
) : ViewModel() {

    val uiState: StateFlow<TablesUiState> = combine(
        tableRepository.observeActiveTables(),
        billRepository.observeOpenBills(),
        shiftRepository.observeOpenShift(),
    ) { tables, bills, shift ->
        TablesUiState(
            tables = tables.map { table ->
                TableWithBill(
                    table = table,
                    openBill = bills.firstOrNull { it.tableId == table.id },
                )
            },
            openShift = shift,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TablesUiState())

    private val _navToBill = Channel<String>(Channel.BUFFERED)
    val navToBill: Flow<String> = _navToBill.receiveAsFlow()

    fun onTableTapped(tableId: String, existingBillId: String?) {
        if (existingBillId != null) {
            viewModelScope.launch { _navToBill.send(existingBillId) }
            return
        }
        viewModelScope.launch {
            val shift = shiftRepository.getOpenShift() ?: return@launch
            val table = tableRepository.getTable(tableId) ?: return@launch
            val now = DateUtil.nowEpochMs()
            val bill = Bill(
                id = UuidGenerator.generate(),
                tableId = tableId,
                type = BillType.OPEN_BILL,
                status = BillStatus.OPEN,
                sessionLabel = table.label ?: "Table",
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
