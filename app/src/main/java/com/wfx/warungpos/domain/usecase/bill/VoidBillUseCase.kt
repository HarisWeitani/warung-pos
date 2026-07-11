package com.wfx.warungpos.domain.usecase.bill

import com.wfx.warungpos.core.common.BillStatus
import com.wfx.warungpos.core.common.SessionProvider
import com.wfx.warungpos.core.common.UserRole
import com.wfx.warungpos.core.common.VoidReason
import com.wfx.warungpos.domain.exception.BillNotVoidableException
import com.wfx.warungpos.domain.exception.InsufficientPermissionsException
import com.wfx.warungpos.domain.repository.BillRepository
import com.wfx.warungpos.domain.repository.OrderRepository
import javax.inject.Inject

class VoidBillUseCase @Inject constructor(
    private val billRepository: BillRepository,
    private val orderRepository: OrderRepository,
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
        val voidedBy = sessionProvider.currentUserId
        billRepository.saveBill(
            bill.copy(
                status = BillStatus.VOID,
                voidedBy = voidedBy,
            )
        )
        // DEFECT-009: a whole-bill void must cascade VOID status to its order_items too — the
        // Z-report's void audit (ReportQueryDao.totalVoidsForShift/InRange) only counts
        // order_items with status='VOID', so without this a whole-bill void contributed nothing
        // to voidCount/voidValue even though the entire bill's items were, in effect, voided.
        orderRepository.getActiveItems(billId).forEach { item ->
            orderRepository.voidItem(item.id, VoidReason.BILL_VOID, voidNote = null, voidedBy ?: "")
        }
        return Result.success(Unit)
    }
}
