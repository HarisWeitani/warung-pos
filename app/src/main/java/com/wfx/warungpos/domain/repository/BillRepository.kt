package com.wfx.warungpos.domain.repository

import com.wfx.warungpos.domain.model.Bill
import kotlinx.coroutines.flow.Flow

interface BillRepository {
    fun observeOpenBills(): Flow<List<Bill>>
    fun observeOpenBillForTable(tableId: String): Flow<Bill?>
    fun observeBillsForShift(shiftId: String): Flow<List<Bill>>
    suspend fun getBill(id: String): Bill?
    suspend fun saveBill(bill: Bill)
    suspend fun getPaidBillsForShift(shiftId: String): List<Bill>
    suspend fun getPaidBillsInRange(startEpoch: Long, endEpoch: Long): List<Bill>
}
