package com.wfx.warungpos.domain.usecase.stock

import com.wfx.warungpos.domain.model.StockBatch
import com.wfx.warungpos.domain.repository.StockRepository
import javax.inject.Inject

/** Receiving a batch of stock (e.g. a new delivery) adds its quantity onto the item's current stock. */
class ReceiveStockBatchUseCase @Inject constructor(private val stockRepository: StockRepository) {
    suspend operator fun invoke(batch: StockBatch): Result<Unit> {
        if (batch.qty <= 0) return Result.failure(IllegalArgumentException("Quantity must be greater than 0"))
        if (batch.costPerUnit < 0) return Result.failure(IllegalArgumentException("Cost cannot be negative"))
        val item = stockRepository.getItem(batch.stockItemId)
            ?: return Result.failure(IllegalArgumentException("Stock item not found"))

        stockRepository.saveBatch(batch)
        stockRepository.setCurrentQty(item.id, item.currentQty + batch.qty)
        return Result.success(Unit)
    }
}
