package com.wfx.warungpos.domain.usecase.stock

import com.wfx.warungpos.core.common.OpnameStatus
import com.wfx.warungpos.core.util.DateUtil
import com.wfx.warungpos.domain.exception.MissingVarianceReasonException
import com.wfx.warungpos.domain.exception.OpnameNotInProgressException
import com.wfx.warungpos.domain.model.StockOpnameLine
import com.wfx.warungpos.domain.repository.StockRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/** FR-OPNAME-3/5: non-zero variance lines require a reason; on submit, currentQty is set to the
 * counted quantity for every line and the opname session becomes immutable. */
class SubmitStockOpnameUseCase @Inject constructor(private val stockRepository: StockRepository) {
    suspend operator fun invoke(lines: List<StockOpnameLine>): Result<Unit> {
        val opname = stockRepository.observeInProgressOpname().first()
            ?: return Result.failure(OpnameNotInProgressException())

        val resolved = lines.map { it.copy(variance = it.countedQty - it.systemQty) }
        for (line in resolved) {
            if (line.variance != 0.0 && line.varianceReason == null) {
                val name = stockRepository.getItem(line.stockItemId)?.name ?: line.stockItemId
                return Result.failure(MissingVarianceReasonException(name))
            }
        }

        stockRepository.saveLines(resolved)
        resolved.forEach { line -> stockRepository.setCurrentQty(line.stockItemId, line.countedQty) }

        val now = DateUtil.nowEpochMs()
        stockRepository.saveOpname(opname.copy(status = OpnameStatus.COMPLETED, completedAt = now))
        return Result.success(Unit)
    }
}
