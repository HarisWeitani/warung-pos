package com.wfx.warungpos.feature.shift

import com.wfx.warungpos.core.common.BillStatus
import com.wfx.warungpos.core.common.ShiftStatus
import com.wfx.warungpos.core.common.SyncStatus
import com.wfx.warungpos.core.common.UserRole
import com.wfx.warungpos.domain.model.Bill
import com.wfx.warungpos.domain.model.Shift
import com.wfx.warungpos.domain.usecase.shift.CloseShiftUseCase
import com.wfx.warungpos.domain.usecase.shift.GenerateZReportUseCase
import com.wfx.warungpos.fake.FakeBillRepository
import com.wfx.warungpos.fake.FakeExpenseRepository
import com.wfx.warungpos.fake.FakePaymentRepository
import com.wfx.warungpos.fake.FakeReportRepository
import com.wfx.warungpos.fake.FakeSessionProvider
import com.wfx.warungpos.fake.FakeShiftRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** DEFECT-016: the current shift is [currentShift] (most recently opened); any other OPEN shift
 * — as left behind by another device — must show up in [ShiftCloseViewModel.UiState.otherOpenShifts]
 * rather than being silently invisible. FakeShiftRepository's flows are one-shot snapshots (not
 * live-updating), so — consistent with this codebase's established pattern for that limitation —
 * repository state is seeded *before* constructing the ViewModel under test. */
@OptIn(ExperimentalCoroutinesApi::class)
class ShiftCloseViewModelTest {

    private lateinit var shiftRepository: FakeShiftRepository
    private lateinit var billRepository: FakeBillRepository
    private lateinit var expenseRepository: FakeExpenseRepository
    private lateinit var reportRepository: FakeReportRepository
    private lateinit var paymentRepository: FakePaymentRepository
    private lateinit var sessionProvider: FakeSessionProvider
    private lateinit var closeShiftUseCase: CloseShiftUseCase

    private val currentShift = Shift(
        id = "shift-current", openedBy = "user-1", closedBy = null, status = ShiftStatus.OPEN,
        openedAt = 300L, closedAt = null, openingFloat = 0L, closingFloat = null,
        updatedAt = 300L, syncStatus = SyncStatus.SYNCED, deviceId = "dev-1",
    )

    private val orphanShift = Shift(
        id = "shift-orphan", openedBy = "user-2", closedBy = null, status = ShiftStatus.OPEN,
        openedAt = 100L, closedAt = null, openingFloat = 0L, closingFloat = null,
        updatedAt = 100L, syncStatus = SyncStatus.SYNCED, deviceId = "dev-2",
    )

    @Before
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        shiftRepository = FakeShiftRepository()
        billRepository = FakeBillRepository()
        expenseRepository = FakeExpenseRepository()
        reportRepository = FakeReportRepository()
        paymentRepository = FakePaymentRepository()
        sessionProvider = FakeSessionProvider(currentUserRole = UserRole.OWNER)
        val generateZReportUseCase = GenerateZReportUseCase(shiftRepository, reportRepository, expenseRepository, paymentRepository)
        closeShiftUseCase = CloseShiftUseCase(shiftRepository, billRepository, paymentRepository, expenseRepository, sessionProvider, generateZReportUseCase)

        shiftRepository.shifts[currentShift.id] = currentShift
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun newViewModel() = ShiftCloseViewModel(
        shiftRepository, reportRepository, expenseRepository, billRepository, closeShiftUseCase,
    )

    @Test
    fun `only one open shift - otherOpenShifts is empty`() = runTest {
        val vm = newViewModel()
        assertEquals(currentShift.id, vm.uiState.value.shift?.id)
        assertTrue(vm.uiState.value.otherOpenShifts.isEmpty())
    }

    @Test
    fun `a second open shift appears in otherOpenShifts, not duplicated as current`() = runTest {
        shiftRepository.shifts[orphanShift.id] = orphanShift

        val vm = newViewModel()

        assertEquals("the most recently opened shift is still the one shown as current", currentShift.id, vm.uiState.value.shift?.id)
        assertEquals(1, vm.uiState.value.otherOpenShifts.size)
        assertEquals(orphanShift.id, vm.uiState.value.otherOpenShifts.first().shift.id)
    }

    @Test
    fun `an other shift with an open bill reports its open bill count`() = runTest {
        shiftRepository.shifts[orphanShift.id] = orphanShift
        billRepository.bills["bill-stranded"] = Bill(
            id = "bill-stranded", status = BillStatus.OPEN,
            sessionLabel = "Counter", createdAt = 0L, paidAt = null, subtotal = 55_000L, discountTotal = 0L,
            grandTotal = 55_000L, note = null, shiftId = orphanShift.id, voidReason = null, voidedBy = null,
            updatedAt = 0L, syncStatus = SyncStatus.SYNCED, deviceId = "dev-2",
        )

        val vm = newViewModel()

        assertEquals(1, vm.uiState.value.otherOpenShifts.first().openBillCount)
    }

    @Test
    fun `closing an other shift with no open bills succeeds and flips its status`() = runTest {
        shiftRepository.shifts[orphanShift.id] = orphanShift
        val vm = newViewModel()
        vm.onOtherShiftFloatChange(orphanShift.id, "20000")

        vm.closeOtherShift(orphanShift.id)

        assertEquals(ShiftStatus.CLOSED, shiftRepository.shifts[orphanShift.id]!!.status)
        // The current shift must be completely unaffected by closing a different one.
        assertEquals(ShiftStatus.OPEN, shiftRepository.shifts[currentShift.id]!!.status)
        assertEquals("no error after a successful close", null, vm.uiState.value.otherOpenShifts.first { it.shift.id == orphanShift.id }.error)
    }

    @Test
    fun `closing an other shift blocked by open bills surfaces a row-level error, not a crash`() = runTest {
        shiftRepository.shifts[orphanShift.id] = orphanShift
        billRepository.bills["bill-stranded"] = Bill(
            id = "bill-stranded", status = BillStatus.OPEN,
            sessionLabel = "Counter", createdAt = 0L, paidAt = null, subtotal = 55_000L, discountTotal = 0L,
            grandTotal = 55_000L, note = null, shiftId = orphanShift.id, voidReason = null, voidedBy = null,
            updatedAt = 0L, syncStatus = SyncStatus.SYNCED, deviceId = "dev-2",
        )
        val vm = newViewModel()

        vm.closeOtherShift(orphanShift.id)

        assertEquals(ShiftStatus.OPEN, shiftRepository.shifts[orphanShift.id]!!.status)
        val row = vm.uiState.value.otherOpenShifts.first { it.shift.id == orphanShift.id }
        assertEquals(false, row.isClosing)
        assertTrue("row error should mention the blocking open bill", row.error?.contains("open bill") == true)
    }

    @Test
    fun `per-row float text is tracked independently per shift`() = runTest {
        val secondOrphan = orphanShift.copy(id = "shift-orphan-2", openedAt = 50L)
        shiftRepository.shifts[orphanShift.id] = orphanShift
        shiftRepository.shifts[secondOrphan.id] = secondOrphan
        val vm = newViewModel()

        vm.onOtherShiftFloatChange(orphanShift.id, "10000")
        vm.onOtherShiftFloatChange(secondOrphan.id, "99000")

        assertEquals("10000", vm.uiState.value.otherOpenShifts.first { it.shift.id == orphanShift.id }.closingFloat)
        assertEquals("99000", vm.uiState.value.otherOpenShifts.first { it.shift.id == secondOrphan.id }.closingFloat)
    }

    @Test
    fun `closing the primary shift is unaffected by other open shifts existing`() = runTest {
        shiftRepository.shifts[orphanShift.id] = orphanShift
        val vm = newViewModel()
        vm.onFloatChange("50000")

        vm.closeShift()

        assertEquals(ShiftStatus.CLOSED, shiftRepository.shifts[currentShift.id]!!.status)
        assertEquals(currentShift.id, vm.uiState.value.closedShiftId)
    }
}
