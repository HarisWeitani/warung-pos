package com.wfx.warungpos.data.local.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.wfx.warungpos.data.local.db.WarungDatabase
import com.wfx.warungpos.data.local.entity.BillEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BillDaoTest {

    private lateinit var db: WarungDatabase
    private lateinit var dao: BillDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, WarungDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.billDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun bill(
        id: String,
        status: String = "OPEN",
        createdAt: Long = 0L,
        paidAt: Long? = null,
        grandTotal: Long = 10_000L,
    ) = BillEntity(
        id = id, status = status, sessionLabel = "Counter",
        createdAt = createdAt, paidAt = paidAt, subtotal = grandTotal, discountTotal = 0L,
        grandTotal = grandTotal, note = null, shiftId = null, voidReason = null, voidedBy = null,
        updatedAt = createdAt, syncStatus = "PENDING", deviceId = "dev-1",
    )

    @Test
    fun insertOpenBill_observeOpenBills_emitsIt() = runTest {
        dao.upsert(bill("bill-1", status = "OPEN"))
        val open = dao.observeOpenBills().first()
        assertEquals(1, open.size)
        assertEquals("bill-1", open.first().id)
    }

    @Test
    fun updateBillToPaid_observeOpenBills_noLongerIncludesIt() = runTest {
        dao.upsert(bill("bill-1", status = "OPEN"))
        assertEquals(1, dao.observeOpenBills().first().size)

        dao.upsert(bill("bill-1", status = "PAID", paidAt = 1_000L))
        assertTrue(dao.observeOpenBills().first().isEmpty())
    }

    @Test
    fun getOpenBills_returnsEmptyThenTwoAfterInserts() = runTest {
        assertEquals(0, dao.getOpenBills().size)
        dao.upsert(bill("bill-1", status = "OPEN"))
        dao.upsert(bill("bill-2", status = "OPEN"))
        assertEquals(2, dao.getOpenBills().size)
    }

    @Test
    fun flow_emitsNewValueAfterEachInsert() = runTest {
        dao.observeOpenBills().test {
            assertEquals(0, awaitItem().size)
            dao.upsert(bill("bill-1", status = "OPEN"))
            assertEquals(1, awaitItem().size)
            dao.upsert(bill("bill-2", status = "OPEN"))
            assertEquals(2, awaitItem().size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun voidedBill_stillReturnedByGetById() = runTest {
        dao.upsert(bill("bill-1", status = "OPEN"))
        dao.upsert(bill("bill-1", status = "VOID"))
        val result = dao.getById("bill-1")
        assertNotNull(result)
        assertEquals("VOID", result!!.status)
    }

    @Test
    fun getById_missingBill_returnsNull() = runTest {
        assertNull(dao.getById("nonexistent"))
    }
}
