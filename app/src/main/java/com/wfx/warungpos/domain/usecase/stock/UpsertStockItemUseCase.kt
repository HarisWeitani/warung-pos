package com.wfx.warungpos.domain.usecase.stock

import com.wfx.warungpos.domain.model.StockItem
import com.wfx.warungpos.domain.repository.StockRepository
import javax.inject.Inject

class UpsertStockItemUseCase @Inject constructor(private val stockRepository: StockRepository) {
    suspend operator fun invoke(item: StockItem): Result<Unit> {
        if (item.name.isBlank()) return Result.failure(IllegalArgumentException("Name must not be blank"))
        if (item.unit.isBlank()) return Result.failure(IllegalArgumentException("Unit must not be blank"))
        if (item.reorderPoint < 0) return Result.failure(IllegalArgumentException("Reorder point cannot be negative"))
        stockRepository.saveItem(item)
        return Result.success(Unit)
    }
}
