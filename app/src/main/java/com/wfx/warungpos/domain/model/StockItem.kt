package com.wfx.warungpos.domain.model

import com.wfx.warungpos.core.common.SyncStatus

data class StockItem(
    val id: String,
    val name: String,
    val unit: String,
    val currentQty: Double,
    val reorderPoint: Double,
    val updatedAt: Long,
    val syncStatus: SyncStatus,
    val deviceId: String,
)
