package com.wfx.warungpos.domain.usecase.bill

import com.wfx.warungpos.core.common.BillStatus
import com.wfx.warungpos.core.common.OrderItemStatus
import com.wfx.warungpos.core.common.SyncStatus
import com.wfx.warungpos.core.common.UserRole
import com.wfx.warungpos.core.common.VoidReason
import com.wfx.warungpos.domain.exception.BillNotVoidableException
import com.wfx.warungpos.domain.exception.InsufficientPermissionsException
import com.wfx.warungpos.domain.model.Bill
import com.wfx.warungpos.domain.model.OrderItem
import com.wfx.warungpos.fake.FakeBillRepository
import com.wfx.warungpos.fake.FakeOrderRepository
import com.wfx.warungpos.fake.FakeSessionProvider
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class VoidBillUseCaseTest {

    private lateinit var billRepository: FakeBillRepository
    private lateinit var orderRepository: FakeOrderRepository
    private lateinit var sessionProvider: FakeSessionProvider
    private lateinit var useCase: VoidBillUseCase

    private val openBill = Bill(
        id = "bill-1", status = BillStatus.OPEN,
        sessionLabel = "Counter", createdAt = 0L, paidAt = null, subtotal = 10_000L,
        discountTotal = 0L, grandTotal = 10_000L, note = null, shiftId = "shift-1",
        voidReason = null, voidedBy = null, updatedAt = 0L, syncStatus = SyncStatus.SYNCED, deviceId = "dev",
    )

    private fun orderItem(id: String, billId: String, status: OrderItemStatus = OrderItemStatus.ORDERED) = OrderItem(
        id = id, billId = billId, menuItemId = "menu-1", nameSnapshot = "Item",
        priceSnapshot = 5_000L, quantity = 1, selectedVariants = emptyList(), lineTotal = 5_000L,
        status = status, voidReason = null, voidNote = null, voidedBy = null, createdAt = 0L, updatedAt = 0L,
        syncStatus = SyncStatus.SYNCED, deviceId = "dev",
    )

    @Before
    fun setup() {
        billRepository = FakeBillRepository()
        orderRepository = FakeOrderRepository()
        sessionProvider = FakeSessionProvider(currentUserRole = UserRole.OWNER)
        useCase = VoidBillUseCase(billRepository, orderRepository, sessionProvider)
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

    @Test
    fun `DEFECT-009 regression - voiding a bill cascades VOID status to its active order_items`() = runTest {
        orderRepository.items["item-1"] = orderItem("item-1", openBill.id)
        orderRepository.items["item-2"] = orderItem("item-2", openBill.id, status = OrderItemStatus.DONE)
        // Already-voided items must not be touched again (e.g. no double-stamping voidedBy).
        orderRepository.items["item-3"] = orderItem("item-3", openBill.id, status = OrderItemStatus.VOID)

        val result = useCase(openBill.id)

        assertTrue(result.isSuccess)
        assertEquals(OrderItemStatus.VOID, orderRepository.items["item-1"]!!.status)
        assertEquals(VoidReason.BILL_VOID, orderRepository.items["item-1"]!!.voidReason)
        assertEquals(OrderItemStatus.VOID, orderRepository.items["item-2"]!!.status)
        assertEquals(VoidReason.BILL_VOID, orderRepository.items["item-2"]!!.voidReason)
    }
}
