package com.wfx.warungpos.data.local.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.wfx.warungpos.data.local.db.WarungDatabase
import com.wfx.warungpos.data.local.entity.BillEntity
import com.wfx.warungpos.data.local.entity.ShiftEntity
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
        shiftId: String? = null,
    ) = BillEntity(
        id = id, status = status, sessionLabel = "Counter",
        createdAt = createdAt, paidAt = paidAt, subtotal = grandTotal, discountTotal = 0L,
        grandTotal = grandTotal, note = null, shiftId = shiftId, voidReason = null, voidedBy = null,
        updatedAt = createdAt, syncStatus = "PENDING", deviceId = "dev-1",
    )

    private fun shiftEntity(id: String) = ShiftEntity(
        id = id, openedBy = "user-1", closedBy = null, status = "OPEN",
        openedAt = 0L, closedAt = null, openingFloat = 0L, closingFloat = null,
        updatedAt = 0L, syncStatus = "PENDING", deviceId = "dev-1",
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

    @Test
    fun getOpenBillsForShift_onlyReturnsBillsOnThatShift() = runTest {
        db.shiftDao().upsert(shiftEntity("shift-a"))
        db.shiftDao().upsert(shiftEntity("shift-b"))
        dao.upsert(bill("bill-1", status = "OPEN", shiftId = "shift-a"))
        dao.upsert(bill("bill-2", status = "OPEN", shiftId = "shift-b"))
        dao.upsert(bill("bill-3", status = "OPEN", shiftId = "shift-a"))

        val forA = dao.getOpenBillsForShift("shift-a")
        assertEquals(2, forA.size)
        assertTrue(forA.all { it.shiftId == "shift-a" })

        val forB = dao.getOpenBillsForShift("shift-b")
        assertEquals(1, forB.size)
    }

    @Test
    fun getOpenBillsForShift_excludesOtherShiftsEvenWhenGetOpenBillsIsNonEmpty() = runTest {
        // DEFECT-003/008 regression: a stray open bill on an unrelated shift must not surface
        // when asking for a specific shift's open bills.
        db.shiftDao().upsert(shiftEntity("shift-old"))
        dao.upsert(bill("bill-stray", status = "OPEN", shiftId = "shift-old"))

        assertEquals(1, dao.getOpenBills().size)
        assertTrue(dao.getOpenBillsForShift("shift-current").isEmpty())
    }
}
