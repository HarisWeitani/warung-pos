package com.wfx.warungpos.domain.usecase.shift

import com.wfx.warungpos.core.util.DateUtil
import com.wfx.warungpos.core.util.UuidGenerator
import com.wfx.warungpos.domain.model.ZReport
import com.wfx.warungpos.domain.repository.ExpenseRepository
import com.wfx.warungpos.domain.repository.PaymentRepository
import com.wfx.warungpos.domain.repository.ReportRepository
import com.wfx.warungpos.domain.repository.ShiftRepository
import javax.inject.Inject

class GenerateZReportUseCase @Inject constructor(
    private val shiftRepository: ShiftRepository,
    private val reportRepository: ReportRepository,
    private val expenseRepository: ExpenseRepository,
    private val paymentRepository: PaymentRepository,
) {
    suspend operator fun invoke(
        shiftId: String,
        countedCash: Long = 0L,
        expectedCash: Long = 0L,
        variance: Long = 0L,
    ): Result<ZReport> {
        val revenue = reportRepository.getTotalRevenueForShift(shiftId)
        val expenses = expenseRepository.totalForShift(shiftId)
        val txCount = reportRepository.getTransactionCountForShift(shiftId)
        val paymentBreakdown = paymentRepository.getPaymentBreakdownForShift(shiftId)
        val voidStats = reportRepository.getVoidStatsForShift(shiftId)

        val paymentJson = paymentBreakdown.joinToString(",", "[", "]") {
            """{"methodId":"${it.paymentMethodId}","total":${it.total}}"""
        }
        val snapshotJson = """{"revenue":$revenue,"expenses":$expenses,"transactions":$txCount,"voidCount":${voidStats.count},"voidValue":${voidStats.totalValue},"openingFloat":0,"countedCash":$countedCash,"expectedCash":$expectedCash,"variance":$variance,"paymentBreakdown":$paymentJson}"""

        val report = ZReport(
            id = UuidGenerator.generate(),
            shiftId = shiftId,
            snapshotJson = snapshotJson,
            createdAt = DateUtil.nowEpochMs(),
        )
        shiftRepository.saveZReport(report)
        return Result.success(report)
    }
}
