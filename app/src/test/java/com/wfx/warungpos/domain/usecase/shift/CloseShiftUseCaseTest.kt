package com.wfx.warungpos.domain.usecase.shift

import com.wfx.warungpos.core.common.BillStatus
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
            id = "bill-1", status = BillStatus.OPEN,
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

    // DEFECT-016: explicit shiftId lets an "other" (non-current) OPEN shift be closed directly,
    // rather than always operating on whatever getOpenShift() considers current.
    private val otherOpenShift = Shift(
        id = "shift-orphan", openedBy = "user-1", closedBy = null, status = ShiftStatus.OPEN,
        openedAt = -100L, closedAt = null, openingFloat = 0L, closingFloat = null,
        updatedAt = -100L, syncStatus = SyncStatus.SYNCED, deviceId = "dev-2",
    )

    @Test
    fun `explicit shiftId closes that shift, not the current one`() = runTest {
        shiftRepository.shifts[otherOpenShift.id] = otherOpenShift

        val result = useCase(50_000L, shiftId = otherOpenShift.id)

        assertTrue(result.isSuccess)
        assertEquals(otherOpenShift.id, result.getOrNull())
        assertEquals(ShiftStatus.CLOSED, shiftRepository.shifts[otherOpenShift.id]!!.status)
        // The shift getOpenShift() considers current must be untouched.
        assertEquals(ShiftStatus.OPEN, shiftRepository.shifts[openShift.id]!!.status)
        assertNotNull(shiftRepository.zReports[otherOpenShift.id])
    }

    @Test
    fun `explicit shiftId still blocks on that shift's own open bills`() = runTest {
        shiftRepository.shifts[otherOpenShift.id] = otherOpenShift
        val strandedBill = Bill(
            id = "bill-stranded", status = BillStatus.OPEN,
            sessionLabel = "Counter", createdAt = 0L, paidAt = null, subtotal = 55_000L, discountTotal = 0L,
            grandTotal = 55_000L, note = null, shiftId = otherOpenShift.id, voidReason = null, voidedBy = null,
            updatedAt = 0L, syncStatus = SyncStatus.SYNCED, deviceId = "dev-2",
        )
        billRepository.bills[strandedBill.id] = strandedBill

        val result = useCase(50_000L, shiftId = otherOpenShift.id)

        val exception = result.exceptionOrNull()
        assertTrue(exception is OpenBillsBlockShiftCloseException)
        assertEquals(strandedBill.id, (exception as OpenBillsBlockShiftCloseException).openBills.first().id)
        // A bill on shift-orphan must never block closing the unrelated current shift.
        assertTrue(useCase(50_000L).isSuccess)
    }

    @Test
    fun `unknown shiftId fails with ShiftNotOpenException`() = runTest {
        val result = useCase(50_000L, shiftId = "does-not-exist")
        assertTrue(result.exceptionOrNull() is com.wfx.warungpos.domain.exception.ShiftNotOpenException)
    }

    @Test
    fun `staff role is blocked from closing an explicit shiftId too, not just the current shift`() = runTest {
        shiftRepository.shifts[otherOpenShift.id] = otherOpenShift
        sessionProvider.currentUserRole = UserRole.STAFF

        val result = useCase(50_000L, shiftId = otherOpenShift.id)

        assertTrue(result.exceptionOrNull() is InsufficientPermissionsException)
        assertEquals(ShiftStatus.OPEN, shiftRepository.shifts[otherOpenShift.id]!!.status)
    }
}
