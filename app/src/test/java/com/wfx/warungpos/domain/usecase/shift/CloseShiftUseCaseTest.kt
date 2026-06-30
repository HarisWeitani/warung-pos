package com.wfx.warungpos.domain.usecase.shift

import com.wfx.warungpos.core.common.BillStatus
import com.wfx.warungpos.core.common.BillType
import com.wfx.warungpos.core.common.ShiftStatus
import com.wfx.warungpos.core.common.SyncStatus
import com.wfx.warungpos.core.common.UserRole
import com.wfx.warungpos.domain.exception.InsufficientPermissionsException
import com.wfx.warungpos.domain.exception.OpenBillsBlockShiftCloseException
import com.wfx.warungpos.domain.model.Bill
import com.wfx.warungpos.domain.model.Shift
import com.wfx.warungpos.fake.FakeBillRepository
import com.wfx.warungpos.fake.FakeExpenseRepository
import com.wfx.warungpos.fake.FakePaymentRepository
import com.wfx.warungpos.fake.FakeReportRepository
import com.wfx.warungpos.fake.FakeSessionProvider
import com.wfx.warungpos.fake.FakeShiftRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CloseShiftUseCaseTest {

    private lateinit var shiftRepository: FakeShiftRepository
    private lateinit var billRepository: FakeBillRepository
    private lateinit var paymentRepository: FakePaymentRepository
    private lateinit var expenseRepository: FakeExpenseRepository
    private lateinit var reportRepository: FakeReportRepository
    private lateinit var sessionProvider: FakeSessionProvider
    private lateinit var useCase: CloseShiftUseCase

    private val openShift = Shift(
        id = "shift-1", openedBy = "user-1", closedBy = null, status = ShiftStatus.OPEN,
        openedAt = 0L, closedAt = null, openingFloat = 100_000L, closingFloat = null,
        updatedAt = 0L, syncStatus = SyncStatus.SYNCED, deviceId = "dev",
    )

    @Before
    fun setup() {
        shiftRepository = FakeShiftRepository()
        billRepository = FakeBillRepository()
        paymentRepository = FakePaymentRepository()
        expenseRepository = FakeExpenseRepository()
        reportRepository = FakeReportRepository()
        sessionProvider = FakeSessionProvider(currentUserRole = UserRole.OWNER)
        val generateZReportUseCase = GenerateZReportUseCase(shiftRepository, reportRepository, expenseRepository, paymentRepository)
        useCase = CloseShiftUseCase(shiftRepository, billRepository, paymentRepository, expenseRepository, sessionProvider, generateZReportUseCase)

        shiftRepository.shifts[openShift.id] = openShift
    }

    @Test
    fun `staff role fails with InsufficientPermissionsException`() = runTest {
        sessionProvider.currentUserRole = UserRole.STAFF
        val result = useCase(150_000L)
        assertTrue(result.exceptionOrNull() is InsufficientPermissionsException)
    }

    @Test
    fun `open bills block shift close with correct bill list`() = runTest {
        val openBill = Bill(
            id = "bill-1", tableId = null, type = BillType.UPFRONT, status = BillStatus.OPEN,
            sessionLabel = "Counter", createdAt = 0L, paidAt = null, subtotal = 0L, discountTotal = 0L,
            grandTotal = 0L, note = null, shiftId = openShift.id, voidReason = null, voidedBy = null,
            updatedAt = 0L, syncStatus = SyncStatus.SYNCED, deviceId = "dev",
        )
        billRepository.bills[openBill.id] = openBill

        val result = useCase(150_000L)
        val exception = result.exceptionOrNull()
        assertTrue(exception is OpenBillsBlockShiftCloseException)
        assertEquals(1, (exception as OpenBillsBlockShiftCloseException).openBills.size)
        assertEquals(openBill.id, exception.openBills.first().id)
    }

    @Test
    fun `no open bills and OWNER succeeds and generates Z-report`() = runTest {
        val result = useCase(150_000L)
        assertTrue(result.isSuccess)
        assertNotNull(shiftRepository.zReports[openShift.id])
        assertEquals(ShiftStatus.CLOSED, shiftRepository.shifts[openShift.id]!!.status)
    }

    @Test
    fun `expectedCash formula equals openingFloat plus cashPayments minus cashExpenses`() = runTest {
        paymentRepository.cashTotalForShift = 75_000L
        expenseRepository.totalForShiftValue = 20_000L
        useCase(150_000L)
        val snapshot = shiftRepository.zReports[openShift.id]!!.snapshotJson
        assertTrue(snapshot.contains("\"expectedCash\":155000"))
    }

    @Test
    fun `variance equals countedCash minus expectedCash`() = runTest {
        paymentRepository.cashTotalForShift = 75_000L
        expenseRepository.totalForShiftValue = 20_000L
        useCase(150_000L)
        val snapshot = shiftRepository.zReports[openShift.id]!!.snapshotJson
        assertTrue(snapshot.contains("\"variance\":-5000"))
    }
}
