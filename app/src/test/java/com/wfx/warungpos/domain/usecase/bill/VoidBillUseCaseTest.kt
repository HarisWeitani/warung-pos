package com.wfx.warungpos.domain.usecase.bill

import com.wfx.warungpos.core.common.BillStatus
import com.wfx.warungpos.core.common.SyncStatus
import com.wfx.warungpos.core.common.UserRole
import com.wfx.warungpos.domain.exception.BillNotVoidableException
import com.wfx.warungpos.domain.exception.InsufficientPermissionsException
import com.wfx.warungpos.domain.model.Bill
import com.wfx.warungpos.fake.FakeBillRepository
import com.wfx.warungpos.fake.FakeSessionProvider
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class VoidBillUseCaseTest {

    private lateinit var billRepository: FakeBillRepository
    private lateinit var sessionProvider: FakeSessionProvider
    private lateinit var useCase: VoidBillUseCase

    private val openBill = Bill(
        id = "bill-1", status = BillStatus.OPEN,
        sessionLabel = "Counter", createdAt = 0L, paidAt = null, subtotal = 10_000L,
        discountTotal = 0L, grandTotal = 10_000L, note = null, shiftId = "shift-1",
        voidReason = null, voidedBy = null, updatedAt = 0L, syncStatus = SyncStatus.SYNCED, deviceId = "dev",
    )

    @Before
    fun setup() {
        billRepository = FakeBillRepository()
        sessionProvider = FakeSessionProvider(currentUserRole = UserRole.OWNER)
        useCase = VoidBillUseCase(billRepository, sessionProvider)
        billRepository.bills[openBill.id] = openBill
    }

    @Test
    fun `STAFF role fails with InsufficientPermissionsException`() = runTest {
        sessionProvider.currentUserRole = UserRole.STAFF
        val result = useCase(openBill.id)
        assertTrue(result.exceptionOrNull() is InsufficientPermissionsException)
    }

    @Test
    fun `OWNER voids OPEN bill successfully`() = runTest {
        val result = useCase(openBill.id)
        assertTrue(result.isSuccess)
        assertTrue(billRepository.bills[openBill.id]!!.status == BillStatus.VOID)
    }

    @Test
    fun `voiding already-VOID bill fails with BillNotVoidableException`() = runTest {
        billRepository.bills[openBill.id] = openBill.copy(status = BillStatus.VOID)
        val result = useCase(openBill.id)
        assertTrue(result.exceptionOrNull() is BillNotVoidableException)
    }

    @Test
    fun `voiding PAID bill fails with BillNotVoidableException`() = runTest {
        billRepository.bills[openBill.id] = openBill.copy(status = BillStatus.PAID)
        val result = useCase(openBill.id)
        assertTrue(result.exceptionOrNull() is BillNotVoidableException)
    }
}
