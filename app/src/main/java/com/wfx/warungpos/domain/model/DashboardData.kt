package com.wfx.warungpos.domain.model

data class DashboardData(
    val totalRevenue: Long,
    val transactionCount: Int,
    val totalExpenses: Long,
    val paymentBreakdown: List<PaymentBreakdown>,
    val bestSellers: List<BestSeller>,
)
