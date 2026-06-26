package com.wfx.warungpos.domain.model

import com.wfx.warungpos.core.common.SyncStatus

data class StockBatch(
    val id: String,
    val stockItemId: String,
    val qty: Double,
    val costPerUnit: Long,
    val receivedAt: Long,
    val expiresAt: Long?,
    val updatedAt: Long,
    val syncStatus: SyncStatus,
    val deviceId: String,
)
