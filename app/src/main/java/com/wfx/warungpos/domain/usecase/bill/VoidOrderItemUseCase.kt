package com.wfx.warungpos.domain.usecase.bill

import com.wfx.warungpos.core.common.SessionManager
import com.wfx.warungpos.core.common.VoidReason
import com.wfx.warungpos.domain.repository.BillRepository
import com.wfx.warungpos.domain.repository.OrderRepository
import javax.inject.Inject

class VoidOrderItemUseCase @Inject constructor(
    private val orderRepository: OrderRepository,
    private val billRepository: BillRepository,
    private val sessionManager: SessionManager,
) {
    suspend operator fun invoke(itemId: String, reason: VoidReason, note: String?): Result<Unit> {
        if (reason == VoidReason.OTHER && note.isNullOrBlank()) {
            return Result.failure(IllegalArgumentException("Note is required when reason is OTHER"))
        }
        val item = orderRepository.getItemById(itemId)
            ?: return Result.failure(IllegalArgumentException("Order item not found"))
        val uid = sessionManager.currentUser.value?.uid ?: ""
        orderRepository.voidItem(itemId, reason, uid)
        // Recalculate bill totals after voiding
        val bill = billRepository.getBill(item.billId) ?: return Result.success(Unit)
        val active = orderRepository.getActiveItems(bill.id)
        val subtotal = active.sumOf { it.lineTotal }
        billRepository.saveBill(bill.copy(subtotal = subtotal, discountTotal = 0L, grandTotal = subtotal))
        return Result.success(Unit)
    }
}
