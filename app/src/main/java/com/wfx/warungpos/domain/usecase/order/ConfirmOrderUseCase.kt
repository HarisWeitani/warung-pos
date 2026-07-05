package com.wfx.warungpos.domain.usecase.order

import com.wfx.warungpos.core.common.BillStatus
import com.wfx.warungpos.core.common.OrderItemStatus
import com.wfx.warungpos.core.common.SessionProvider
import com.wfx.warungpos.core.common.SyncStatus
import com.wfx.warungpos.core.util.DateUtil
import com.wfx.warungpos.core.util.UuidGenerator
import com.wfx.warungpos.domain.exception.EmptyCartException
import com.wfx.warungpos.domain.exception.MissingRequiredVariantException
import com.wfx.warungpos.domain.exception.ShiftNotOpenException
import com.wfx.warungpos.domain.model.Bill
import com.wfx.warungpos.domain.model.CartItem
import com.wfx.warungpos.domain.model.OrderItem
import com.wfx.warungpos.domain.model.VariantGroup
import com.wfx.warungpos.domain.repository.BillRepository
import com.wfx.warungpos.domain.repository.MenuRepository
import com.wfx.warungpos.domain.repository.OrderRepository
import com.wfx.warungpos.domain.repository.ShiftRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/** Every order is a single pay-first counter bill (AM-2) — there is no destination to choose. */
class ConfirmOrderUseCase @Inject constructor(
    private val billRepository: BillRepository,
    private val orderRepository: OrderRepository,
    private val menuRepository: MenuRepository,
    private val shiftRepository: ShiftRepository,
    private val sessionProvider: SessionProvider,
) {
    suspend operator fun invoke(cart: List<CartItem>): Result<String> {
        if (cart.isEmpty()) return Result.failure(EmptyCartException())

        val shift = shiftRepository.getOpenShift() ?: return Result.failure(ShiftNotOpenException())

        // Validate required variant groups for each cart item
        for (cartItem in cart) {
            val groups: List<VariantGroup> = menuRepository.observeVariantGroups(cartItem.menuItem.id).first()
            for (group in groups) {
                if (group.isRequired && cartItem.selectedVariants.none { it.groupId == group.id }) {
                    return Result.failure(MissingRequiredVariantException(cartItem.menuItem.name, group.name))
                }
            }
        }

        val now = DateUtil.nowEpochMs()
        val deviceId = sessionProvider.deviceId

        val bill = createBill(shift.id, now, deviceId)
        billRepository.saveBill(bill)
        saveOrderItems(cart, bill.id, now, deviceId)
        recalculate(bill)
        return Result.success(bill.id)
    }

    private fun createBill(shiftId: String, now: Long, deviceId: String) = Bill(
        id = UuidGenerator.generate(),
        status = BillStatus.OPEN,
        sessionLabel = "Order",
        createdAt = now,
        paidAt = null,
        subtotal = 0L,
        discountTotal = 0L,
        grandTotal = 0L,
        note = null,
        shiftId = shiftId,
        voidReason = null,
        voidedBy = null,
        updatedAt = now,
        syncStatus = SyncStatus.PENDING,
        deviceId = deviceId,
    )

    private suspend fun saveOrderItems(cart: List<CartItem>, billId: String, now: Long, deviceId: String) {
        for (cartItem in cart) {
            val pricePerUnit = cartItem.menuItem.basePrice + cartItem.selectedVariants.sumOf { it.priceDelta }
            orderRepository.saveItem(
                OrderItem(
                    id = UuidGenerator.generate(),
                    billId = billId,
                    menuItemId = cartItem.menuItem.id,
                    nameSnapshot = cartItem.menuItem.name,
                    priceSnapshot = pricePerUnit,
                    quantity = cartItem.quantity,
                    selectedVariants = cartItem.selectedVariants,
                    lineTotal = pricePerUnit * cartItem.quantity,
                    status = OrderItemStatus.ORDERED,
                    voidReason = null,
                    voidedBy = null,
                    createdAt = now,
                    updatedAt = now,
                    syncStatus = SyncStatus.PENDING,
                    deviceId = deviceId,
                )
            )
        }
    }

    private suspend fun recalculate(bill: Bill) {
        val active = orderRepository.getActiveItems(bill.id)
        val subtotal = active.sumOf { it.lineTotal }
        billRepository.saveBill(bill.copy(subtotal = subtotal, discountTotal = 0L, grandTotal = subtotal))
    }
}
