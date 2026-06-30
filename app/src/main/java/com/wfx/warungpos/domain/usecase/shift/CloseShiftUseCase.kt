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
    suspend operator fun invoke(countedCash: Long): Result<String> {
        if (sessionProvider.currentUserRole != UserRole.OWNER) {
            return Result.failure(InsufficientPermissionsException())
        }
        val shift = shiftRepository.getOpenShift() ?: return Result.failure(ShiftNotOpenException())

        val openBills = billRepository.getOpenBills()
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
