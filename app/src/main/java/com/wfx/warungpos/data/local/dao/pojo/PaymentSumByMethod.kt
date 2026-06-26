package com.wfx.warungpos.data.local.dao.pojo

data class PaymentSumByMethod(
    val paymentMethodId: String,
    val total: Long,
)
