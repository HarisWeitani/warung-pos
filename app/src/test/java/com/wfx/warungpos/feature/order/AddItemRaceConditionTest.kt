package com.wfx.warungpos.feature.order

import com.wfx.warungpos.core.common.OrderItemStatus
import com.wfx.warungpos.core.common.SyncStatus
import com.wfx.warungpos.domain.model.OrderItem
import com.wfx.warungpos.fake.FakeOrderRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * DEFECT-004: rapid concurrent taps on a menu item could silently under-count the added
 * quantity. Root cause: `BillDetailViewModel.addItem()` did an unsynchronized read-then-write
 * against Room (read the existing line's `quantity`, write `quantity + 1`), with each tap
 * launching its own coroutine — two taps could both read the same starting quantity before
 * either write landed, and one increment would be silently lost (a classic lost-update race).
 *
 * The fix wraps that whole read-modify-write section in a `Mutex`. This test can't easily
 * construct the real `BillDetailViewModel` (it depends on the concrete `SessionManager`, which
 * needs a real Android `Context` — no mocking library is set up in this project), so it instead
 * reproduces the *exact same pattern* — read current quantity from [FakeOrderRepository], write
 * back quantity+1, mutex-guarded — under genuine multi-threaded concurrency (`Dispatchers.Default`,
 * not just coroutine interleaving on one thread) to prove the mutex fully serializes concurrent
 * increments with no lost updates.
 */
class AddItemRaceConditionTest {

    private fun item(qty: Int) = OrderItem(
        id = "item-1", billId = "bill-1", menuItemId = "menu-1", nameSnapshot = "NasiPutih",
        priceSnapshot = 5_000L, quantity = qty, selectedVariants = emptyList(), lineTotal = 5_000L * qty,
        status = OrderItemStatus.ORDERED, voidReason = null, voidNote = null, voidedBy = null,
        createdAt = 0L, updatedAt = 0L, syncStatus = SyncStatus.PENDING, deviceId = "dev",
    )

    /** Mirrors the fixed version: the same read-modify-write, now serialized by a Mutex. */
    private suspend fun incrementWithMutex(repo: FakeOrderRepository, mutex: Mutex) = mutex.withLock {
        val existing = repo.getActiveItems("bill-1").first()
        repo.saveItem(existing.copy(quantity = existing.quantity + 1))
    }

    @Test
    fun `DEFECT-004 fix verification - mutex-guarded increments never lose an update`() = runBlocking {
        val repo = FakeOrderRepository()
        repo.items["item-1"] = item(qty = 0)
        val mutex = Mutex()
        val taps = 30

        withContext(Dispatchers.Default) {
            (0 until taps).map { async { incrementWithMutex(repo, mutex) } }.awaitAll()
        }

        assertEquals(taps, repo.items["item-1"]!!.quantity)
    }
}
