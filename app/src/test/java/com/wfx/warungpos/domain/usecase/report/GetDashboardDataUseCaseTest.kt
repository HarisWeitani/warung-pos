package com.wfx.warungpos.domain.usecase.report

import com.wfx.warungpos.core.common.BillStatus
import com.wfx.warungpos.core.common.SyncStatus
import com.wfx.warungpos.core.util.DateUtil
import com.wfx.warungpos.domain.model.Bill
import com.wfx.warungpos.domain.model.PaymentBreakdown
import com.wfx.warungpos.fake.FakeBillRepository
import com.wfx.warungpos.fake.FakeExpenseRepository
import com.wfx.warungpos.fake.FakePaymentRepository
import com.wfx.warungpos.fake.FakeReportRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GetDashboardDataUseCaseTest {

    private lateinit var billRepository: FakeBillRepository
    private lateinit var reportRepository: FakeReportRepository
    private lateinit var expenseRepository: FakeExpenseRepository
    private lateinit var paymentRepository: FakePaymentRepository
    private lateinit var useCase: GetDashboardDataUseCase

    @Before
    fun setup() {
        billRepository = FakeBillRepository()
        reportRepository = FakeReportRepository()
        expenseRepository = FakeExpenseRepository()
        paymentRepository = FakePaymentRepository()
        useCase = GetDashboardDataUseCase(billRepository, reportRepository, expenseRepository, paymentRepository)
    }

    private fun paidBillToday(id: String, total: Long) = Bill(
        id = id, status = BillStatus.PAID,
        sessionLabel = "Counter", createdAt = DateUtil.nowEpochMs(), paidAt = DateUtil.nowEpochMs(),
        subtotal = total, discountTotal = 0L, grandTotal = total, note = null, shiftId = "shift-1",
        voidReason = null, voidedBy = null, updatedAt = DateUtil.nowEpochMs(), syncStatus = SyncStatus.SYNCED, deviceId = "dev",
    )

    @Test
    fun aggregatesRevenueAndTransactionCountForToday() = runTest {
        billRepository.bills["bill-1"] = paidBillToday("bill-1", 10_000L)
        billRepository.bills["bill-2"] = paidBillToday("bill-2", 20_000L)
        reportRepository.revenueForShift = 30_000L

        val result = useCase().getOrThrow()
        assertEquals(30_000L, result.totalRevenue)
        assertEquals(2, result.transactionCount)
    }

    @Test
    fun includesExpensesPaymentBreakdownAndBestSellers() = runTest {
        expenseRepository.expenses["exp-1"] = com.wfx.warungpos.domain.model.Expense(
            id = "exp-1", shiftId = "shift-1", category = com.wfx.warungpos.core.common.ExpenseCategory.SUPPLIES,
            amount = 5_000L, note = null, createdBy = "user-1",
            createdAt = DateUtil.nowEpochMs(), updatedAt = DateUtil.nowEpochMs(), syncStatus = SyncStatus.SYNCED, deviceId = "dev",
        )
        paymentRepository.paymentBreakdown = listOf(PaymentBreakdown("pm_tunai", 10_000L))

        val result = useCase().getOrThrow()
        assertEquals(5_000L, result.totalExpenses)
        assertEquals(1, result.paymentBreakdown.size)
    }

    @Test
    fun noActivityToday_returnsZeroedDashboard() = runTest {
        val result = useCase().getOrThrow()
        assertEquals(0L, result.totalRevenue)
        assertEquals(0, result.transactionCount)
        assertTrue(result.bestSellers.isEmpty())
    }
}
