package com.wfx.warungpos.data.local.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.wfx.warungpos.data.local.db.WarungDatabase
import com.wfx.warungpos.data.local.entity.ShiftEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** DEFECT-003/008: multiple `shifts` rows could end up simultaneously OPEN because the old
 * check-then-act (read getOpenShift(), then separately insert) had no atomicity. These tests run
 * against a real Room-backed SQLite DB (not the in-memory fake) so the transactional guarantee of
 * [ShiftDao.openIfNoneOpen] is exercised for real, including under genuine thread concurrency. */
@RunWith(AndroidJUnit4::class)
class ShiftDaoTest {

    private lateinit var db: WarungDatabase
    private lateinit var dao: ShiftDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, WarungDatabase::class.java).build()
        dao = db.shiftDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun shift(id: String, openedAt: Long) = ShiftEntity(
        id = id, openedBy = "user-1", closedBy = null, status = "OPEN",
        openedAt = openedAt, closedAt = null, openingFloat = 0L, closingFloat = null,
        updatedAt = openedAt, syncStatus = "PENDING", deviceId = "dev-1",
    )

    @Test
    fun openIfNoneOpen_firstCallSucceeds() = runBlocking {
        val opened = dao.openIfNoneOpen(shift("shift-a", 100L))
        assertTrue(opened)
        assertNotNull(dao.getOpenShift())
        assertEquals("shift-a", dao.getOpenShift()!!.id)
    }

    @Test
    fun openIfNoneOpen_secondCallIsRejectedWhenOneAlreadyOpen() = runBlocking {
        assertTrue(dao.openIfNoneOpen(shift("shift-a", 100L)))
        val secondOpened = dao.openIfNoneOpen(shift("shift-b", 200L))

        assertEquals(false, secondOpened)
        val allShifts = dao.getRecent(10)
        assertEquals(1, allShifts.count { it.status == "OPEN" })
        assertEquals("shift-a", dao.getOpenShift()!!.id)
    }

    @Test
    fun openIfNoneOpen_underGenuineConcurrency_onlyOneWinnerEverPersists() = runBlocking {
        // Fire many real, concurrently-dispatched attempts to open a shift at once — this is the
        // actual race that used to produce duplicate OPEN rows (e.g. AppViewModel's session-start
        // call racing OrderViewModel's per-bill defensive call). The transactional guard must
        // ensure exactly one of them wins, regardless of interleaving.
        val attempts = 20
        withContext(Dispatchers.Default) {
            (0 until attempts).map { i ->
                async { dao.openIfNoneOpen(shift("shift-$i", i.toLong())) }
            }.awaitAll()
        }

        val openShifts = dao.getRecent(attempts + 1).filter { it.status == "OPEN" }
        assertEquals(
            "exactly one shift must end up OPEN no matter how many raced to open one",
            1,
            openShifts.size,
        )
    }

    /** DEFECT-016: [openIfNoneOpen] only guards against a *single device* racing itself. It does
     * nothing to stop inbound sync (a plain [ShiftDao.upsert], not [openIfNoneOpen]) from writing
     * a second, independently-opened OPEN shift that arrived from another device. These exercise
     * [getAllOpenShifts]/[observeAllOpenShifts] against exactly that scenario — multiple OPEN rows
     * that were never meant to coexist, but do. */
    @Test
    fun getAllOpenShifts_returnsEveryOpenRowNotJustTheNewest() = runBlocking {
        // Simulates inbound sync: each write lands via plain upsert, bypassing the
        // check-then-act guard entirely — exactly how a second device's shift would arrive.
        dao.upsert(shift("shift-old", openedAt = 100L))
        dao.upsert(shift("shift-newer", openedAt = 200L))
        dao.upsert(shift("shift-newest", openedAt = 300L))

        val allOpen = dao.getAllOpenShifts()
        assertEquals(3, allOpen.size)
        assertEquals(listOf("shift-newest", "shift-newer", "shift-old"), allOpen.map { it.id })
        // getOpenShift() must still resolve to a single row (the newest) — this fix adds
        // visibility into the others without changing which one is "current".
        assertEquals("shift-newest", dao.getOpenShift()!!.id)
    }

    @Test
    fun getAllOpenShifts_excludesClosedShifts() = runBlocking {
        dao.upsert(shift("shift-open", openedAt = 100L))
        val closed = shift("shift-closed", openedAt = 50L).copy(status = "CLOSED", closedAt = 60L)
        dao.upsert(closed)

        val allOpen = dao.getAllOpenShifts()
        assertEquals(1, allOpen.size)
        assertEquals("shift-open", allOpen.first().id)
    }

    @Test
    fun observeAllOpenShifts_emitsUpdatedListAsShiftsChange() = runBlocking {
        dao.upsert(shift("shift-a", openedAt = 100L))
        dao.upsert(shift("shift-b", openedAt = 200L))

        assertEquals(2, dao.observeAllOpenShifts().first().size)

        // Closing one should drop it from the live query without a manual re-subscribe.
        dao.upsert(shift("shift-a", openedAt = 100L).copy(status = "CLOSED", closedAt = 150L))
        val afterClose = dao.observeAllOpenShifts().first()
        assertEquals(1, afterClose.size)
        assertEquals("shift-b", afterClose.first().id)
    }
}
