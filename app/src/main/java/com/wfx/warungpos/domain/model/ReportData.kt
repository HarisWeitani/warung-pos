package com.wfx.warungpos.domain.model

import com.wfx.warungpos.core.common.ExpenseCategory

data class ReportData(
    val revenue: Long,
    val expenses: Long,
    val grossProfit: Long,
    val paymentBreakdown: List<PaymentBreakdown>,
    val voidStats: VoidStats,
    val expensesByCategory: Map<ExpenseCategory, Long>,
    val bestSellers: List<BestSeller>,
)
