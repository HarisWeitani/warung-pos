package com.wfx.warungpos.domain.usecase.payment

import com.wfx.warungpos.core.common.BillStatus
import com.wfx.warungpos.core.common.ShiftStatus
import com.wfx.warungpos.core.common.SyncStatus
import com.wfx.warungpos.domain.exception.BillAlreadyPaidException
import com.wfx.warungpos.domain.exception.InsufficientPaymentException
import com.wfx.warungpos.domain.exception.InsufficientTenderedAmountException
import com.wfx.warungpos.domain.exception.ShiftNotOpenException
import com.wfx.warungpos.domain.model.Bill
import com.wfx.warungpos.domain.model.Shift
import com.wfx.warungpos.fake.FakeBillRepository
import com.wfx.warungpos.fake.FakePaymentRepository
import com.wfx.warungpos.fake.FakeSessionProvider
import com.wfx.warungpos.fake.FakeShiftRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ProcessPaymentUseCaseTest {

    private lateinit var billRepository: FakeBillRepository
    private lateinit var paymentRepository: FakePaymentRepository
    private lateinit var shiftRepository: FakeShiftRepository
    private lateinit var sessionProvider: FakeSessionProvider
    private lateinit var useCase: ProcessPaymentUseCase

    private val bill = Bill(
        id = "bill-1", status = BillStatus.OPEN,
        sessionLabel = "Counter", createdAt = 0L, paidAt = null, subtotal = 45_000L,
        discountTotal = 0L, grandTotal = 45_000L, note = null, shiftId = "shift-1",
        voidReason = null, voidedBy = null, updatedAt = 0L, syncStatus = SyncStatus.SYNCED, deviceId = "dev",
    )

    @Before
    fun setup() {
        billRepository = FakeBillRepository()
        paymentRepository = FakePaymentRepository(billRepository)
        shiftRepository = FakeShiftRepository()
        sessionProvider = FakeSessionProvider()
        useCase = ProcessPaymentUseCase(billRepository, paymentRepository, shiftRepository, sessionProvider)

        billRepository.bills[bill.id] = bill
        shiftRepository.shifts["shift-1"] = Shift(
            id = "shift-1", openedBy = "user-1", closedBy = null, status = ShiftStatus.OPEN,
            openedAt = 0L, closedAt = null, openingFloat = 100_000L, closingFloat = null,
            updatedAt = 0L, syncStatus = SyncStatus.SYNCED, deviceId = "dev",
        )
    }

    @Test
    fun `exact cash payment succeeds and marks bill PAID`() = runTest {
        val result = useCase(bill.id, listOf(PaymentRow("pm_tunai", 45_000L, 45_000L)))
        assertTrue(result.isSuccess)
        assertEquals(BillStatus.PAID, billRepository.getBill(bill.id)!!.status)
    }

    @Test
    fun `overpaid cash succeeds and change equals tendered minus amount`() = runTest {
        val result = useCase(bill.id, listOf(PaymentRow("pm_tunai", 45_000L, 50_000L)))
        assertTrue(result.isSuccess)
        val payment = paymentRepository.payments.values.first()
        assertEquals(5_000L, payment.change)
    }

    @Test
    fun `cash tendered less than amount fails with InsufficientTenderedAmountException`() = runTest {
        val result = useCase(bill.id, listOf(PaymentRow("pm_tunai", 45_000L, 40_000L)))
        assertTrue(result.exceptionOrNull() is InsufficientTenderedAmountException)
    }

    @Test
    fun `split cash plus QRIS summing to total succeeds`() = runTest {
        val rows = listOf(
            PaymentRow("pm_tunai", 20_000L, 20_000L),
            PaymentRow("pm_qris", 25_000L, 25_000L),
        )
        val result = useCase(bill.id, rows)
        assertTrue(result.isSuccess)
        assertEquals(2, paymentRepository.payments.size)
    }

    @Test
    fun `split total less than grandTotal fails with InsufficientPaymentException`() = runTest {
        val rows = listOf(
            PaymentRow("pm_tunai", 10_000L, 10_000L),
            PaymentRow("pm_qris", 20_000L, 20_000L),
        )
        val result = useCase(bill.id, rows)
        assertTrue(result.exceptionOrNull() is InsufficientPaymentException)
    }

    @Test
    fun `bill already PAID fails with BillAlreadyPaidException`() = runTest {
        billRepository.bills[bill.id] = bill.copy(status = BillStatus.PAID)
        val result = useCase(bill.id, listOf(PaymentRow("pm_tunai", 45_000L, 45_000L)))
        assertTrue(result.exceptionOrNull() is BillAlreadyPaidException)
    }

    @Test
    fun `no active shift fails with ShiftNotOpenException`() = runTest {
        shiftRepository.shifts.clear()
        val result = useCase(bill.id, listOf(PaymentRow("pm_tunai", 45_000L, 45_000L)))
        assertTrue(result.exceptionOrNull() is ShiftNotOpenException)
    }
}
