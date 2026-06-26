package com.wfx.warungpos.domain.model

import com.wfx.warungpos.core.common.SyncStatus

data class Payment(
    val id: String,
    val billId: String,
    val paymentMethodId: String,
    val amount: Long,
    val change: Long,
    val paidAt: Long,
    val updatedAt: Long,
    val syncStatus: SyncStatus,
    val deviceId: String,
)
