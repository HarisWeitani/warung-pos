package com.wfx.warungpos.domain.usecase.bill

import com.wfx.warungpos.core.common.OrderItemStatus
import com.wfx.warungpos.core.common.SyncStatus
import com.wfx.warungpos.core.common.VoidReason
import com.wfx.warungpos.domain.model.OrderItem
import com.wfx.warungpos.fake.FakeBillRepository
import com.wfx.warungpos.fake.FakeOrderRepository
import com.wfx.warungpos.fake.FakeSessionProvider
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class VoidOrderItemUseCaseTest {

    private lateinit var orderRepository: FakeOrderRepository
    private lateinit var billRepository: FakeBillRepository
    private lateinit var sessionProvider: FakeSessionProvider
    private lateinit var useCase: VoidOrderItemUseCase

    private val item = OrderItem(
        id = "item-1", billId = "bill-1", menuItemId = "menu-1", nameSnapshot = "Nasi Goreng",
        priceSnapshot = 15_000L, quantity = 1, selectedVariants = emptyList(), lineTotal = 15_000L,
        status = OrderItemStatus.ORDERED, voidReason = null, voidNote = null, voidedBy = null,
        createdAt = 0L, updatedAt = 0L, syncStatus = SyncStatus.SYNCED, deviceId = "dev",
    )

    @Before
    fun setup() {
        orderRepository = FakeOrderRepository()
        billRepository = FakeBillRepository()
        sessionProvider = FakeSessionProvider()
        useCase = VoidOrderItemUseCase(orderRepository, billRepository, sessionProvider)
        orderRepository.items[item.id] = item
    }

    @Test
    fun `VoidReason OTHER with blank note fails with IllegalArgumentException`() = runTest {
        val result = useCase(item.id, VoidReason.OTHER, "")
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
    }

    @Test
    fun `VoidReason OTHER with null note fails with IllegalArgumentException`() = runTest {
        val result = useCase(item.id, VoidReason.OTHER, null)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
    }

    @Test
    fun `VoidReason OTHER with valid note succeeds`() = runTest {
        val result = useCase(item.id, VoidReason.OTHER, "Customer changed mind")
        assertTrue(result.isSuccess)
        assertTrue(orderRepository.items[item.id]!!.status == OrderItemStatus.VOID)
    }

    @Test
    fun `DEFECT-006 regression - the note is actually persisted, not discarded`() = runTest {
        useCase(item.id, VoidReason.OTHER, "Customer changed mind")
        assertEquals("Customer changed mind", orderRepository.items[item.id]!!.voidNote)
    }

    @Test
    fun `VoidReason KITCHEN_ERROR with no note succeeds`() = runTest {
        val result = useCase(item.id, VoidReason.KITCHEN_ERROR, null)
        assertTrue(result.isSuccess)
        assertTrue(orderRepository.items[item.id]!!.status == OrderItemStatus.VOID)
    }
}
