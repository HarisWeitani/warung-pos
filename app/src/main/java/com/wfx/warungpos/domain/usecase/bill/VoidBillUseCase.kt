package com.wfx.warungpos.domain.usecase.bill

import com.wfx.warungpos.core.common.BillStatus
import com.wfx.warungpos.core.common.SessionProvider
import com.wfx.warungpos.core.common.UserRole
import com.wfx.warungpos.domain.exception.BillNotVoidableException
import com.wfx.warungpos.domain.exception.InsufficientPermissionsException
import com.wfx.warungpos.domain.repository.BillRepository
import javax.inject.Inject

class VoidBillUseCase @Inject constructor(
    private val billRepository: BillRepository,
    private val sessionProvider: SessionProvider,
) {
    suspend operator fun invoke(billId: String): Result<Unit> {
        if (sessionProvider.currentUserRole != UserRole.OWNER) {
            return Result.failure(InsufficientPermissionsException())
        }
        val bill = billRepository.getBill(billId)
            ?: return Result.failure(IllegalArgumentException("Bill not found"))
        if (bill.status != BillStatus.OPEN) {
            return Result.failure(BillNotVoidableException())
        }
        billRepository.saveBill(
            bill.copy(
                status = BillStatus.VOID,
                voidedBy = sessionProvider.currentUserId,
            )
        )
        return Result.success(Unit)
    }
}
