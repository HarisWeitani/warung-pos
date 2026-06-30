package com.wfx.warungpos.domain.usecase.report

import com.wfx.warungpos.domain.model.ReportData
import com.wfx.warungpos.domain.repository.ExpenseRepository
import com.wfx.warungpos.domain.repository.PaymentRepository
import com.wfx.warungpos.domain.repository.ReportRepository
import javax.inject.Inject

class GetReportDataUseCase @Inject constructor(
    private val reportRepository: ReportRepository,
    private val expenseRepository: ExpenseRepository,
    private val paymentRepository: PaymentRepository,
) {
    suspend operator fun invoke(startEpoch: Long, endEpoch: Long): Result<ReportData> {
        val revenue = reportRepository.getTotalRevenueInRange(startEpoch, endEpoch)
        val expenses = expenseRepository.getExpensesInRange(startEpoch, endEpoch)
        val totalExpenses = expenses.sumOf { it.amount }
        val expensesByCategory = expenses.groupBy { it.category }.mapValues { (_, v) -> v.sumOf { e -> e.amount } }
        val paymentBreakdown = paymentRepository.getPaymentBreakdownInRange(startEpoch, endEpoch)
        val voidStats = reportRepository.getVoidStatsInRange(startEpoch, endEpoch)
        val bestSellers = reportRepository.getBestSellers(startEpoch, endEpoch, limit = 20)
        return Result.success(
            ReportData(
                revenue = revenue,
                expenses = totalExpenses,
                grossProfit = revenue - totalExpenses,
                paymentBreakdown = paymentBreakdown,
                voidStats = voidStats,
                expensesByCategory = expensesByCategory,
                bestSellers = bestSellers,
            )
        )
    }
}
