package com.wfx.warungpos.domain.model

import com.wfx.warungpos.core.common.SyncStatus
import com.wfx.warungpos.core.common.VarianceReason

data class StockOpnameLine(
    val id: String,
    val opnameId: String,
    val stockItemId: String,
    val systemQty: Double,
    val countedQty: Double,
    val variance: Double,
    val varianceReason: VarianceReason?,
    val updatedAt: Long,
    val syncStatus: SyncStatus,
    val deviceId: String,
)
