package com.wfx.warungpos.fake

import com.wfx.warungpos.core.common.BillStatus
import com.wfx.warungpos.domain.model.Bill
import com.wfx.warungpos.domain.repository.BillRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class FakeBillRepository : BillRepository {
    val bills = mutableMapOf<String, Bill>()

    override fun observeOpenBills(): Flow<List<Bill>> =
        flowOf(bills.values.filter { it.status == BillStatus.OPEN })

    override fun observeBillById(id: String): Flow<Bill?> = flowOf(bills[id])

    override fun observeBillsForShift(shiftId: String): Flow<List<Bill>> =
        flowOf(bills.values.filter { it.shiftId == shiftId })

    override suspend fun getBill(id: String): Bill? = bills[id]

    override suspend fun saveBill(bill: Bill) {
        bills[bill.id] = bill
    }

    override suspend fun getOpenBills(): List<Bill> = bills.values.filter { it.status == BillStatus.OPEN }

    override suspend fun getOpenBillsForShift(shiftId: String): List<Bill> =
        bills.values.filter { it.status == BillStatus.OPEN && it.shiftId == shiftId }

    override suspend fun getPaidBillsForShift(shiftId: String): List<Bill> =
        bills.values.filter { it.shiftId == shiftId && it.status == BillStatus.PAID }

    override suspend fun getPaidBillsInRange(startEpoch: Long, endEpoch: Long): List<Bill> =
        bills.values.filter { it.status == BillStatus.PAID && (it.paidAt ?: 0L) in startEpoch..endEpoch }
}
