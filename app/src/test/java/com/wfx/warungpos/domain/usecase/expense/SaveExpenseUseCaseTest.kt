package com.wfx.warungpos.domain.usecase.expense

import com.wfx.warungpos.core.common.ExpenseCategory
import com.wfx.warungpos.core.common.ShiftStatus
import com.wfx.warungpos.core.common.SyncStatus
import com.wfx.warungpos.domain.exception.ShiftNotOpenException
import com.wfx.warungpos.domain.model.Shift
import com.wfx.warungpos.fake.FakeExpenseRepository
import com.wfx.warungpos.fake.FakeSessionProvider
import com.wfx.warungpos.fake.FakeShiftRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SaveExpenseUseCaseTest {

    private lateinit var expenseRepository: FakeExpenseRepository
    private lateinit var shiftRepository: FakeShiftRepository
    private lateinit var sessionProvider: FakeSessionProvider
    private lateinit var useCase: SaveExpenseUseCase

    @Before
    fun setup() {
        expenseRepository = FakeExpenseRepository()
        shiftRepository = FakeShiftRepository()
        sessionProvider = FakeSessionProvider()
        useCase = SaveExpenseUseCase(expenseRepository, shiftRepository, sessionProvider)
        shiftRepository.shifts["shift-1"] = Shift(
            id = "shift-1", openedBy = "user-1", closedBy = null, status = ShiftStatus.OPEN,
            openedAt = 0L, closedAt = null, openingFloat = 100_000L, closingFloat = null,
            updatedAt = 0L, syncStatus = SyncStatus.SYNCED, deviceId = "dev",
        )
    }

    @Test
    fun zeroAmount_failsWithIllegalArgumentException() = runTest {
        val result = useCase(ExpenseCategory.SUPPLIES, 0L, null)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
    }

    @Test
    fun negativeAmount_failsWithIllegalArgumentException() = runTest {
        val result = useCase(ExpenseCategory.SUPPLIES, -5_000L, null)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
    }

    @Test
    fun noOpenShift_failsWithShiftNotOpenException() = runTest {
        shiftRepository.shifts.clear()
        val result = useCase(ExpenseCategory.SUPPLIES, 10_000L, null)
        assertTrue(result.exceptionOrNull() is ShiftNotOpenException)
    }

    @Test
    fun validExpense_savesAgainstOpenShift() = runTest {
        val result = useCase(ExpenseCategory.TRANSPORT, 30_000L, "Gas refill")
        assertTrue(result.isSuccess)
        val saved = expenseRepository.expenses.values.first()
        assertEquals("shift-1", saved.shiftId)
        assertEquals(30_000L, saved.amount)
        assertEquals("Gas refill", saved.note)
    }

    @Test
    fun blankNote_savedAsNull() = runTest {
        useCase(ExpenseCategory.OTHER, 5_000L, "   ")
        assertEquals(null, expenseRepository.expenses.values.first().note)
    }
}
