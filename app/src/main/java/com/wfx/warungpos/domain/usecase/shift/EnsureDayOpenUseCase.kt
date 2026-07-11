package com.wfx.warungpos.domain.usecase.shift

import com.wfx.warungpos.core.common.SessionProvider
import com.wfx.warungpos.core.common.ShiftStatus
import com.wfx.warungpos.core.common.SyncStatus
import com.wfx.warungpos.core.util.DateUtil
import com.wfx.warungpos.core.util.UuidGenerator
import com.wfx.warungpos.domain.model.Shift
import com.wfx.warungpos.domain.repository.BillRepository
import com.wfx.warungpos.domain.repository.ExpenseRepository
import com.wfx.warungpos.domain.repository.PaymentRepository
import com.wfx.warungpos.domain.repository.ShiftRepository
import javax.inject.Inject

/**
 * There is no manual "open day" action: a Day auto-opens with a zero float, and either
 * auto-closes when the calendar date rolls over (skipping the counted-cash step entirely,
 * since no one manually counted) or stays open until the owner closes it manually from
 * Settings. Call this once per app session (AppViewModel init) and again defensively before
 * creating a new bill, in case the app was left running across a midnight rollover.
 */
class EnsureDayOpenUseCase @Inject constructor(
    private val shiftRepository: ShiftRepository,
    private val billRepository: BillRepository,
    private val paymentRepository: PaymentRepository,
    private val expenseRepository: ExpenseRepository,
    private val generateZReportUseCase: GenerateZReportUseCase,
    private val sessionProvider: SessionProvider,
) {
    suspend operator fun invoke() {
        val current = shiftRepository.getOpenShift()
        if (current == null) {
            openNewDay()
            return
        }

        val sameDay = DateUtil.startOfDay(current.openedAt) == DateUtil.startOfDay(DateUtil.nowEpochMs())
        if (sameDay) return

        // Date has rolled over. Auto-close only if there are no open bills *on this shift*;
        // otherwise the previous Day stays open (even though its date has passed) until an
        // owner manually resolves it via the Close Day screen in Settings. Scoped to this
        // shift's id (DEFECT-003/008) — the old unscoped getOpenBills() counted every open bill
        // across every shift ever, so one stray open bill anywhere could block every future
        // day's auto-close.
        if (billRepository.getOpenBillsForShift(current.id).isNotEmpty()) return

        autoCloseAndOpenNext(current)
    }

    private suspend fun autoCloseAndOpenNext(shift: Shift) {
        val cashPayments = paymentRepository.getCashPaymentsTotalForShift(shift.id)
        val cashExpenses = expenseRepository.totalForShift(shift.id)
        val expectedCash = shift.openingFloat + cashPayments - cashExpenses
        val now = DateUtil.nowEpochMs()

        shiftRepository.saveShift(
            shift.copy(
                status = ShiftStatus.CLOSED,
                // null closedBy marks this as an automatic (system) close, as opposed to an
                // owner's manual close which always stamps their user id.
                closedBy = null,
                closedAt = now,
                closingFloat = expectedCash,
                updatedAt = now,
            )
        )
        generateZReportUseCase(shift.id, countedCash = expectedCash, expectedCash = expectedCash, variance = 0L)
        openNewDay()
    }

    private suspend fun openNewDay() {
        val now = DateUtil.nowEpochMs()
        // DEFECT-003/008: this used to be a plain saveShift() reached via a check-then-act
        // (invoke() reads getOpenShift(), decides null, *then* calls this) with no atomicity
        // between the two — two racing callers (AppViewModel's session-start call and
        // OrderViewModel's defensive per-bill call) could both observe "no open shift" and each
        // insert their own OPEN row. openShiftIfNoneOpen() re-checks inside the same DB
        // transaction as the insert, so at most one of the racers actually opens a shift; the
        // other's write is simply skipped (the winner's shift is picked up by the next
        // getOpenShift() call downstream).
        shiftRepository.openShiftIfNoneOpen(
            Shift(
                id = UuidGenerator.generate(),
                openedBy = sessionProvider.currentUserId ?: "",
                closedBy = null,
                status = ShiftStatus.OPEN,
                openedAt = now,
                closedAt = null,
                openingFloat = 0L,
                closingFloat = null,
                updatedAt = now,
                syncStatus = SyncStatus.PENDING,
                deviceId = sessionProvider.deviceId,
            )
        )
    }
}
