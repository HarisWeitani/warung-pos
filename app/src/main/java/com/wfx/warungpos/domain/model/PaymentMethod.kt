package com.wfx.warungpos.domain.model

import com.wfx.warungpos.core.common.SyncStatus

data class PaymentMethod(
    val id: String,
    val name: String,
    val isActive: Boolean,
    val sortOrder: Int,
    val updatedAt: Long,
    val syncStatus: SyncStatus,
    val deviceId: String,
)
