package com.wfx.warungpos.domain.usecase.stock

import com.wfx.warungpos.core.common.OpnameStatus
import com.wfx.warungpos.core.common.SessionProvider
import com.wfx.warungpos.core.common.SyncStatus
import com.wfx.warungpos.core.util.DateUtil
import com.wfx.warungpos.core.util.UuidGenerator
import com.wfx.warungpos.domain.exception.OpnameAlreadyInProgressException
import com.wfx.warungpos.domain.model.StockOpname
import com.wfx.warungpos.domain.model.StockOpnameLine
import com.wfx.warungpos.domain.repository.StockRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/** FR-OPNAME-1/6: only one opname can be IN_PROGRESS at a time. Snapshots every StockItem's
 * current quantity as the line's systemQty baseline for later variance comparison. */
class StartStockOpnameUseCase @Inject constructor(
    private val stockRepository: StockRepository,
    private val sessionProvider: SessionProvider,
) {
    suspend operator fun invoke(): Result<String> {
        if (stockRepository.observeInProgressOpname().first() != null) {
            return Result.failure(OpnameAlreadyInProgressException())
        }

        val now = DateUtil.nowEpochMs()
        val deviceId = sessionProvider.deviceId
        val opname = StockOpname(
            id = UuidGenerator.generate(),
            conductedBy = sessionProvider.currentUserId ?: "",
            status = OpnameStatus.IN_PROGRESS,
            startedAt = now,
            completedAt = null,
            updatedAt = now,
            syncStatus = SyncStatus.PENDING,
            deviceId = deviceId,
        )
        stockRepository.saveOpname(opname)

        val lines = stockRepository.getAllItemsOnce().map { item ->
            StockOpnameLine(
                id = UuidGenerator.generate(),
                opnameId = opname.id,
                stockItemId = item.id,
                systemQty = item.currentQty,
                countedQty = item.currentQty,
                variance = 0.0,
                varianceReason = null,
                updatedAt = now,
                syncStatus = SyncStatus.PENDING,
                deviceId = deviceId,
            )
        }
        stockRepository.saveLines(lines)

        return Result.success(opname.id)
    }
}
