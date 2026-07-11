package com.wfx.warungpos.data.local.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.wfx.warungpos.data.local.db.WarungDatabase
import com.wfx.warungpos.data.local.entity.BillEntity
import com.wfx.warungpos.data.local.entity.OrderItemEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OrderItemDaoTest {

    private lateinit var db: WarungDatabase
    private lateinit var dao: OrderItemDao
    private lateinit var billDao: BillDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, WarungDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.orderItemDao()
        billDao = db.billDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun bill(id: String, status: String = "OPEN") = BillEntity(
        id = id, status = status, sessionLabel = "Counter - $id",
        createdAt = 0L, paidAt = null, subtotal = 0L, discountTotal = 0L,
        grandTotal = 0L, note = null, shiftId = null, voidReason = null, voidedBy = null,
        updatedAt = 0L, syncStatus = "PENDING", deviceId = "dev-1",
    )

    private fun orderItem(
        id: String,
        billId: String,
        status: String = "ORDERED",
        createdAt: Long = 0L,
    ) = OrderItemEntity(
        id = id, billId = billId, menuItemId = null, nameSnapshot = "Item $id",
        priceSnapshot = 10_000L, quantity = 1, selectedVariantsJson = "[]",
        lineTotal = 10_000L, status = status, voidReason = null, voidNote = null, voidedBy = null,
        createdAt = createdAt, updatedAt = createdAt, syncStatus = "PENDING", deviceId = "dev-1",
    )

    @Test
    fun observeQueue_onlyReturnsOrderedItemsOnOpenBills() = runTest {
        billDao.upsert(bill("bill-open", status = "OPEN"))
        billDao.upsert(bill("bill-paid", status = "PAID"))

        dao.upsert(orderItem("oi-ordered-open", "bill-open", status = "ORDERED"))
        dao.upsert(orderItem("oi-done-open", "bill-open", status = "DONE"))
        dao.upsert(orderItem("oi-void-open", "bill-open", status = "VOID"))
        dao.upsert(orderItem("oi-ordered-paid", "bill-paid", status = "ORDERED"))

        val queue = dao.observeQueue().first()
        assertEquals(1, queue.size)
        assertEquals("oi-ordered-open", queue.first().id)
    }

    @Test
    fun observeQueue_ordersByCreatedAtAscending() = runTest {
        billDao.upsert(bill("bill-open", status = "OPEN"))
        dao.upsert(orderItem("oi-2", "bill-open", createdAt = 2_000L))
        dao.upsert(orderItem("oi-1", "bill-open", createdAt = 1_000L))
        dao.upsert(orderItem("oi-3", "bill-open", createdAt = 3_000L))

        val queue = dao.observeQueue().first()
        assertEquals(listOf("oi-1", "oi-2", "oi-3"), queue.map { it.id })
    }

    @Test
    fun markDone_removesItemFromQueue() = runTest {
        billDao.upsert(bill("bill-open", status = "OPEN"))
        dao.upsert(orderItem("oi-1", "bill-open"))
        assertEquals(1, dao.observeQueue().first().size)

        dao.markDone("oi-1", updatedAt = 1_000L)

        val queue = dao.observeQueue().first()
        assertEquals(0, queue.size)
        val updated = dao.getById("oi-1")
        assertEquals("DONE", updated!!.status)
        assertEquals("PENDING", updated.syncStatus)
    }
}
