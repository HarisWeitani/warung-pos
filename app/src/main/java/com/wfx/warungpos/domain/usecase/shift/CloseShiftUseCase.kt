package com.wfx.warungpos.domain.usecase.shift

import com.wfx.warungpos.core.common.SessionProvider
import com.wfx.warungpos.core.common.ShiftStatus
import com.wfx.warungpos.core.common.UserRole
import com.wfx.warungpos.core.util.DateUtil
import com.wfx.warungpos.domain.exception.InsufficientPermissionsException
import com.wfx.warungpos.domain.exception.OpenBillsBlockShiftCloseException
import com.wfx.warungpos.domain.exception.ShiftNotOpenException
import com.wfx.warungpos.domain.repository.BillRepository
import com.wfx.warungpos.domain.repository.ExpenseRepository
import com.wfx.warungpos.domain.repository.PaymentRepository
import com.wfx.warungpos.domain.repository.ShiftRepository
import javax.inject.Inject

class CloseShiftUseCase @Inject constructor(
    private val shiftRepository: ShiftRepository,
    private val billRepository: BillRepository,
    private val paymentRepository: PaymentRepository,
    private val expenseRepository: ExpenseRepository,
    private val sessionProvider: SessionProvider,
    private val generateZReportUseCase: GenerateZReportUseCase,
) {
    /**
     * DEFECT-016: [shiftId] lets the caller close a *specific* OPEN shift rather than always
     * whichever one [ShiftRepository.getOpenShift] considers "current" (the most recently
     * opened). Without this, any older OPEN shift left behind by another device — and any bill
     * still attached to it — was permanently unreachable through Close Day. Defaults to null
     * (close the current shift) to preserve existing callers' behavior.
     */
    suspend operator fun invoke(countedCash: Long, shiftId: String? = null): Result<String> {
        if (sessionProvider.currentUserRole != UserRole.OWNER) {
            return Result.failure(InsufficientPermissionsException())
        }
        val shift = (if (shiftId != null) shiftRepository.getById(shiftId) else shiftRepository.getOpenShift())
            ?: return Result.failure(ShiftNotOpenException())

        // DEFECT-003/008: scoped to this shift — the old getOpenBills() counted every open bill
        // across every shift that has ever existed, so a stray open bill on an unrelated shift
        // could block closing a completely different shift.
        val openBills = billRepository.getOpenBillsForShift(shift.id)
        if (openBills.isNotEmpty()) {
            return Result.failure(OpenBillsBlockShiftCloseException(openBills))
        }

        val cashPayments = paymentRepository.getCashPaymentsTotalForShift(shift.id)
        val cashExpenses = expenseRepository.totalForShift(shift.id)
        val expectedCash = shift.openingFloat + cashPayments - cashExpenses
        val variance = countedCash - expectedCash

        val now = DateUtil.nowEpochMs()
        shiftRepository.saveShift(
            shift.copy(
                status = ShiftStatus.CLOSED,
                closedBy = sessionProvider.currentUserId,
                closedAt = now,
                closingFloat = countedCash,
                updatedAt = now,
            )
        )
        generateZReportUseCase(shift.id, countedCash, expectedCash, variance)
        return Result.success(shift.id)
    }
}
