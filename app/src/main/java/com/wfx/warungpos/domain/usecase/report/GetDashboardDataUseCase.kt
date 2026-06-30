package com.wfx.warungpos.domain.usecase.report

import com.wfx.warungpos.core.util.DateUtil
import com.wfx.warungpos.domain.model.DashboardData
import com.wfx.warungpos.domain.repository.BillRepository
import com.wfx.warungpos.domain.repository.ExpenseRepository
import com.wfx.warungpos.domain.repository.PaymentRepository
import com.wfx.warungpos.domain.repository.ReportRepository
import javax.inject.Inject

class GetDashboardDataUseCase @Inject constructor(
    private val billRepository: BillRepository,
    private val reportRepository: ReportRepository,
    private val expenseRepository: ExpenseRepository,
    private val paymentRepository: PaymentRepository,
) {
    suspend operator fun invoke(): Result<DashboardData> {
        val (start, end) = DateUtil.todayRangeWib()
        val revenue = reportRepository.getTotalRevenueInRange(start, end)
        val txCount = billRepository.getPaidBillsInRange(start, end).size
        val expenses = expenseRepository.getExpensesInRange(start, end).sumOf { it.amount }
        val paymentBreakdown = paymentRepository.getPaymentBreakdownInRange(start, end)
        val bestSellers = reportRepository.getBestSellers(start, end, 5)
        return Result.success(
            DashboardData(
                totalRevenue = revenue,
                transactionCount = txCount,
                totalExpenses = expenses,
                paymentBreakdown = paymentBreakdown,
                bestSellers = bestSellers,
            )
        )
    }
}
