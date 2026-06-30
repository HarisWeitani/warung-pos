package com.wfx.warungpos.domain.usecase.payment

data class PaymentRow(
    val methodId: String,
    val amount: Long,
    val tenderedAmount: Long = amount,
)
