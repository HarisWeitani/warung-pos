package com.wfx.warungpos.domain.usecase.stock

import com.wfx.warungpos.core.common.OpnameStatus
import com.wfx.warungpos.core.common.OrderItemStatus
import com.wfx.warungpos.core.common.SyncStatus
import com.wfx.warungpos.core.common.VarianceReason
import com.wfx.warungpos.domain.exception.MissingVarianceReasonException
import com.wfx.warungpos.domain.exception.OpnameAlreadyInProgressException
import com.wfx.warungpos.domain.exception.OpnameNotInProgressException
import com.wfx.warungpos.domain.model.MenuItemIngredient
import com.wfx.warungpos.domain.model.OrderItem
import com.wfx.warungpos.domain.model.StockBatch
import com.wfx.warungpos.domain.model.StockItem
import com.wfx.warungpos.fake.FakeOrderRepository
import com.wfx.warungpos.fake.FakeSessionProvider
import com.wfx.warungpos.fake.FakeStockRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

private fun stockItem(id: String, qty: Double = 10.0, reorderPoint: Double = 2.0) = StockItem(
    id = id, name = "Item $id", unit = "kg", currentQty = qty, reorderPoint = reorderPoint,
    updatedAt = 0L, syncStatus = SyncStatus.SYNCED, deviceId = "dev",
)

class UpsertStockItemUseCaseTest {
    private val repo = FakeStockRepository()
    private val useCase = UpsertStockItemUseCase(repo)

    @Test
    fun `blank name fails`() = runTest {
        val result = useCase(stockItem("s1").copy(name = " "))
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
    }

    @Test
    fun `blank unit fails`() = runTest {
        val result = useCase(stockItem("s1").copy(unit = ""))
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
    }

    @Test
    fun `negative reorder point fails`() = runTest {
        val result = useCase(stockItem("s1").copy(reorderPoint = -1.0))
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
    }

    @Test
    fun `valid item saves successfully`() = runTest {
        val result = useCase(stockItem("s1"))
        assertTrue(result.isSuccess)
        assertEquals("Item s1", repo.items["s1"]!!.name)
    }
}

class ReceiveStockBatchUseCaseTest {
    private val repo = FakeStockRepository()
    private val useCase = ReceiveStockBatchUseCase(repo)

    private fun batch(stockItemId: String, qty: Double = 5.0, cost: Long = 1_000L) = StockBatch(
        id = "b1", stockItemId = stockItemId, qty = qty, costPerUnit = cost,
        receivedAt = 0L, expiresAt = null, updatedAt = 0L, syncStatus = SyncStatus.SYNCED, deviceId = "dev",
    )

    @Test
    fun `zero or negative qty fails`() = runTest {
        repo.items["s1"] = stockItem("s1")
        val result = useCase(batch("s1", qty = 0.0))
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
    }

    @Test
    fun `unknown stock item fails`() = runTest {
        val result = useCase(batch("missing"))
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
    }

    @Test
    fun `receiving a batch increments currentQty by batch qty`() = runTest {
        repo.items["s1"] = stockItem("s1", qty = 10.0)
        val result = useCase(batch("s1", qty = 5.0))
        assertTrue(result.isSuccess)
        assertEquals(15.0, repo.items["s1"]!!.currentQty, 0.0001)
        assertEquals(1, repo.batches.size)
    }
}

class StartStockOpnameUseCaseTest {
    private val repo = FakeStockRepository()
    private val sessionProvider = FakeSessionProvider()
    private val useCase = StartStockOpnameUseCase(repo, sessionProvider)

    @Before
    fun setup() {
        repo.items["s1"] = stockItem("s1", qty = 10.0)
        repo.items["s2"] = stockItem("s2", qty = 3.0)
    }

    @Test
    fun `starting an opname snapshots every stock item as a line`() = runTest {
        val result = useCase()
        assertTrue(result.isSuccess)
        val opnameId = result.getOrThrow()
        val lines = repo.getLinesForOpname(opnameId)
        assertEquals(2, lines.size)
        assertTrue(lines.all { it.systemQty == it.countedQty })
    }

    @Test
    fun `starting a second opname while one is in progress fails`() = runTest {
        useCase()
        val result = useCase()
        assertTrue(result.exceptionOrNull() is OpnameAlreadyInProgressException)
    }
}

class SubmitStockOpnameUseCaseTest {
    private val repo = FakeStockRepository()
    private val sessionProvider = FakeSessionProvider()
    private val startUseCase = StartStockOpnameUseCase(repo, sessionProvider)
    private val useCase = SubmitStockOpnameUseCase(repo)

    @Before
    fun setup() {
        repo.items["s1"] = stockItem("s1", qty = 10.0)
    }

    @Test
    fun `submitting with no in-progress opname fails`() = runTest {
        val result = useCase(emptyList())
        assertTrue(result.exceptionOrNull() is OpnameNotInProgressException)
    }

    @Test
    fun `non-zero variance without a reason fails`() = runTest {
        val opnameId = startUseCase().getOrThrow()
        val line = repo.getLinesForOpname(opnameId).first().copy(countedQty = 8.0)
        val result = useCase(listOf(line))
        assertTrue(result.exceptionOrNull() is MissingVarianceReasonException)
    }

    @Test
    fun `submit sets currentQty to countedQty and completes the opname`() = runTest {
        val opnameId = startUseCase().getOrThrow()
        val line = repo.getLinesForOpname(opnameId).first().copy(
            countedQty = 8.0, varianceReason = VarianceReason.COUNT_ERROR,
        )
        val result = useCase(listOf(line))
        assertTrue(result.isSuccess)
        assertEquals(8.0, repo.items["s1"]!!.currentQty, 0.0001)
        assertEquals(OpnameStatus.COMPLETED, repo.opnames[opnameId]!!.status)
    }

    @Test
    fun `zero variance does not require a reason`() = runTest {
        val opnameId = startUseCase().getOrThrow()
        val line = repo.getLinesForOpname(opnameId).first()
        val result = useCase(listOf(line))
        assertTrue(result.isSuccess)
    }

    @Test
    fun `deductions queued during the session apply on top of the counted baseline on submit`() = runTest {
        val opnameId = startUseCase().getOrThrow()
        // A sale happens mid-session: queued, not applied immediately.
        repo.queueDeduction(opnameId, "s1", 3.0)
        assertEquals(10.0, repo.items["s1"]!!.currentQty, 0.0001)

        val line = repo.getLinesForOpname(opnameId).first().copy(countedQty = 8.0, varianceReason = VarianceReason.COUNT_ERROR)
        val result = useCase(listOf(line))

        assertTrue(result.isSuccess)
        assertEquals(5.0, repo.items["s1"]!!.currentQty, 0.0001) // 8 counted - 3 queued
        assertTrue(repo.pendingDeductions.isEmpty())
    }
}

class DeductStockForBillUseCaseTest {
    private val stockRepo = FakeStockRepository()
    private val orderRepo = FakeOrderRepository()
    private val sessionProvider = FakeSessionProvider()
    private val startOpnameUseCase = StartStockOpnameUseCase(stockRepo, sessionProvider)
    private val useCase = DeductStockForBillUseCase(orderRepo, stockRepo)

    private fun orderItem(id: String, billId: String, menuItemId: String, qty: Int) = OrderItem(
        id = id, billId = billId, menuItemId = menuItemId, nameSnapshot = "Item", priceSnapshot = 1_000L,
        quantity = qty, selectedVariants = emptyList(), lineTotal = 1_000L, status = OrderItemStatus.ORDERED,
        voidReason = null, voidedBy = null, createdAt = 0L, updatedAt = 0L, syncStatus = SyncStatus.SYNCED, deviceId = "dev",
    )

    @Before
    fun setup() {
        stockRepo.items["s1"] = stockItem("s1", qty = 10.0)
        stockRepo.ingredients.add(
            MenuItemIngredient(
                menuItemId = "menu1", stockItemId = "s1", qtyPerServing = 2.0,
                updatedAt = 0L, syncStatus = SyncStatus.SYNCED, deviceId = "dev",
            )
        )
        orderRepo.items["oi1"] = orderItem("oi1", "bill1", "menu1", qty = 3)
    }

    @Test
    fun `deducts immediately when no opname is in progress`() = runTest {
        useCase("bill1")
        assertEquals(4.0, stockRepo.items["s1"]!!.currentQty, 0.0001) // 10 - 2*3
    }

    @Test
    fun `queues instead of deducting while an opname is in progress`() = runTest {
        startOpnameUseCase()
        useCase("bill1")

        assertEquals(10.0, stockRepo.items["s1"]!!.currentQty, 0.0001) // untouched
        assertEquals(1, stockRepo.pendingDeductions.size)
        assertEquals(6.0, stockRepo.pendingDeductions.first().third, 0.0001) // 2*3
    }
}
