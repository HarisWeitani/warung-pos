package com.wfx.warungpos.domain.usecase.stock

import com.wfx.warungpos.domain.repository.OrderRepository
import com.wfx.warungpos.domain.repository.StockRepository
import javax.inject.Inject

/** FR-STOCK-4/6: on bill payment, deduct each active order item's recipe ingredients from stock.
 * Items with no recipe configured (no MenuItemIngredient rows) do not touch stock at all. */
class DeductStockForBillUseCase @Inject constructor(
    private val orderRepository: OrderRepository,
    private val stockRepository: StockRepository,
) {
    suspend operator fun invoke(billId: String) {
        val activeItems = orderRepository.getActiveItems(billId)
        for (orderItem in activeItems) {
            val menuItemId = orderItem.menuItemId ?: continue
            val ingredients = stockRepository.getIngredientsForMenuItem(menuItemId)
            for (ingredient in ingredients) {
                stockRepository.deductQty(ingredient.stockItemId, ingredient.qtyPerServing * orderItem.quantity)
            }
        }
    }
}
