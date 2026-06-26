package com.wfx.warungpos.domain.model

data class PaymentBreakdown(
    val paymentMethodId: String,
    val total: Long,
)
