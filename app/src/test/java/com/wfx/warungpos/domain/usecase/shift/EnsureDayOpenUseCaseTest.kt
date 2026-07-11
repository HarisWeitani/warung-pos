package com.wfx.warungpos.domain.usecase.shift

import com.wfx.warungpos.core.common.ShiftStatus
import com.wfx.warungpos.core.common.SyncStatus
import com.wfx.warungpos.core.util.DateUtil
import com.wfx.warungpos.domain.model.Bill
import com.wfx.warungpos.core.common.BillStatus
import com.wfx.warungpos.domain.model.MenuItem
import com.wfx.warungpos.domain.model.Shift
import com.wfx.warungpos.fake.FakeBillRepository
import com.wfx.warungpos.fake.FakeExpenseRepository
import com.wfx.warungpos.fake.FakeMenuRepository
import com.wfx.warungpos.fake.FakePaymentRepository
import com.wfx.warungpos.fake.FakeReportRepository
import com.wfx.warungpos.fake.FakeSessionProvider
import com.wfx.warungpos.fake.FakeShiftRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

private const val TWO_DAYS_MS = 2 * 24 * 60 * 60 * 1000L

class EnsureDayOpenUseCaseTest {

    private lateinit var shiftRepository: FakeShiftRepository
    private lateinit var billRepository: FakeBillRepository
    private lateinit var paymentRepository: FakePaymentRepository
    private lateinit var expenseRepository: FakeExpenseRepository
    private lateinit var reportRepository: FakeReportRepository
    private lateinit var sessionProvider: FakeSessionProvider
    private lateinit var useCase: EnsureDayOpenUseCase

    @Before
    fun setup() {
        shiftRepository = FakeShiftRepository()
        billRepository = FakeBillRepository()
        paymentRepository = FakePaymentRepository()
        expenseRepository = FakeExpenseRepository()
        reportRepository = FakeReportRepository()
        sessionProvider = FakeSessionProvider()
        val generateZReportUseCase = GenerateZReportUseCase(shiftRepository, reportRepository, expenseRepository, paymentRepository)
        useCase = EnsureDayOpenUseCase(shiftRepository, billRepository, paymentRepository, expenseRepository, generateZReportUseCase, sessionProvider)
    }

    @Test
    fun `no existing shift opens a new day with zero float`() = runTest {
        useCase()
        val opened = shiftRepository.getOpenShift()
        assertNotNull(opened)
        assertEquals(0L, opened!!.openingFloat)
        assertEquals(ShiftStatus.OPEN, opened.status)
    }

    @Test
    fun `open shift from the same calendar day is left untouched`() = runTest {
        val shift = Shift(
            id = "shift-1", openedBy = "user-1", closedBy = null, status = ShiftStatus.OPEN,
            openedAt = DateUtil.nowEpochMs(), closedAt = null, openingFloat = 0L, closingFloat = null,
            updatedAt = 0L, syncStatus = SyncStatus.SYNCED, deviceId = "dev",
        )
        shiftRepository.shifts[shift.id] = shift

        useCase()

        assertEquals(1, shiftRepository.shifts.size)
        assertEquals(ShiftStatus.OPEN, shiftRepository.shifts[shift.id]!!.status)
    }

    @Test
    fun `rolled-over day with zero open bills auto-closes and opens the next day`() = runTest {
        val shift = Shift(
            id = "shift-1", openedBy = "user-1", closedBy = null, status = ShiftStatus.OPEN,
            openedAt = DateUtil.nowEpochMs() - TWO_DAYS_MS, closedAt = null, openingFloat = 100_000L, closingFloat = null,
            updatedAt = 0L, syncStatus = SyncStatus.SYNCED, deviceId = "dev",
        )
        shiftRepository.shifts[shift.id] = shift
        paymentRepository.cashTotalForShift = 50_000L
        expenseRepository.totalForShiftValue = 10_000L

        useCase()

        val closed = shiftRepository.shifts[shift.id]!!
        assertEquals(ShiftStatus.CLOSED, closed.status)
        assertNull(closed.closedBy)
        assertEquals(140_000L, closed.closingFloat)
        assertNotNull(shiftRepository.zReports[shift.id])

        val newDay = shiftRepository.getOpenShift()
        assertNotNull(newDay)
        assertEquals(0L, newDay!!.openingFloat)
    }

    @Test
    fun `rolled-over day with open bills stays open`() = runTest {
        val shift = Shift(
            id = "shift-1", openedBy = "user-1", closedBy = null, status = ShiftStatus.OPEN,
            openedAt = DateUtil.nowEpochMs() - TWO_DAYS_MS, closedAt = null, openingFloat = 0L, closingFloat = null,
            updatedAt = 0L, syncStatus = SyncStatus.SYNCED, deviceId = "dev",
        )
        shiftRepository.shifts[shift.id] = shift
        billRepository.bills["bill-1"] = Bill(
            id = "bill-1", status = BillStatus.OPEN, sessionLabel = "Counter",
            createdAt = 0L, paidAt = null, subtotal = 0L, discountTotal = 0L, grandTotal = 0L,
            note = null, shiftId = shift.id, voidReason = null, voidedBy = null,
            updatedAt = 0L, syncStatus = SyncStatus.SYNCED, deviceId = "dev",
        )

        useCase()

        assertEquals(1, shiftRepository.shifts.size)
        assertEquals(ShiftStatus.OPEN, shiftRepository.shifts[shift.id]!!.status)
    }

    @Test
    fun `DEFECT-003-008 regression - openShiftIfNoneOpen rejects a second open while one is already open`() = runTest {
        val shiftA = Shift(
            id = "shift-a", openedBy = "user-1", closedBy = null, status = ShiftStatus.OPEN,
            openedAt = 100L, closedAt = null, openingFloat = 0L, closingFloat = null,
            updatedAt = 0L, syncStatus = SyncStatus.PENDING, deviceId = "dev",
        )
        val shiftB = shiftA.copy(id = "shift-b", openedAt = 200L)

        val firstOpened = shiftRepository.openShiftIfNoneOpen(shiftA)
        val secondOpened = shiftRepository.openShiftIfNoneOpen(shiftB)

        assertTrue(firstOpened)
        assertFalse(secondOpened)
        assertEquals(1, shiftRepository.shifts.values.count { it.status == ShiftStatus.OPEN })
        assertEquals("shift-a", shiftRepository.getOpenShift()!!.id)
    }
}

class CheckSoldOutItemsUseCaseTest {

    private fun menuItem(id: String, soldOut: Boolean) = MenuItem(
        id = id, categoryId = "cat-1", name = "Item", basePrice = 10_000L,
        isAvailable = true, isSoldOut = soldOut, updatedAt = 0L,
        syncStatus = SyncStatus.SYNCED, deviceId = "dev",
    )

    @Test
    fun `returns true when one item has isSoldOut true`() = runTest {
        val menuRepository = FakeMenuRepository()
        menuRepository.items["item-1"] = menuItem("item-1", soldOut = true)
        val useCase = CheckSoldOutItemsUseCase(menuRepository)
        assertTrue(useCase())
    }

    @Test
    fun `returns false when zero sold-out items`() = runTest {
        val menuRepository = FakeMenuRepository()
        menuRepository.items["item-1"] = menuItem("item-1", soldOut = false)
        val useCase = CheckSoldOutItemsUseCase(menuRepository)
        assertFalse(useCase())
    }
}

class ResetSoldOutItemsUseCaseTest {

    private fun menuItem(id: String, soldOut: Boolean) = MenuItem(
        id = id, categoryId = "cat-1", name = "Item", basePrice = 10_000L,
        isAvailable = true, isSoldOut = soldOut, updatedAt = 0L,
        syncStatus = SyncStatus.SYNCED, deviceId = "dev",
    )

    @Test
    fun `resets all sold-out items back to available`() = runTest {
        val menuRepository = FakeMenuRepository()
        menuRepository.items["item-1"] = menuItem("item-1", soldOut = true)
        menuRepository.items["item-2"] = menuItem("item-2", soldOut = true)
        menuRepository.items["item-3"] = menuItem("item-3", soldOut = false)

        val useCase = ResetSoldOutItemsUseCase(menuRepository)
        useCase()

        assertTrue(menuRepository.items.values.none { it.isSoldOut })
    }
}
