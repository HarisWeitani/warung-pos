package com.wfx.warungpos.domain.usecase.stock

import com.wfx.warungpos.domain.repository.OrderRepository
import com.wfx.warungpos.domain.repository.StockRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/** FR-STOCK-4/6: on bill payment, deduct each active order item's recipe ingredients from stock.
 * Items with no recipe configured (no MenuItemIngredient rows) do not touch stock at all.
 *
 * FR-OPNAME-7: while an opname is IN_PROGRESS, deductions are queued instead of applied
 * immediately — otherwise the opname's absolute setCurrentQty on submit would silently drop them. */
class DeductStockForBillUseCase @Inject constructor(
    private val orderRepository: OrderRepository,
    private val stockRepository: StockRepository,
) {
    suspend operator fun invoke(billId: String) {
        val activeItems = orderRepository.getActiveItems(billId)
        val inProgressOpname = stockRepository.observeInProgressOpname().first()
        for (orderItem in activeItems) {
            val menuItemId = orderItem.menuItemId ?: continue
            val ingredients = stockRepository.getIngredientsForMenuItem(menuItemId)
            for (ingredient in ingredients) {
                val amount = ingredient.qtyPerServing * orderItem.quantity
                if (inProgressOpname != null) {
                    stockRepository.queueDeduction(inProgressOpname.id, ingredient.stockItemId, amount)
                } else {
                    stockRepository.deductQty(ingredient.stockItemId, amount)
                }
            }
        }
    }
}
