package com.wfx.warungpos.domain.model

import com.wfx.warungpos.core.common.OpnameStatus
import com.wfx.warungpos.core.common.SyncStatus

data class StockOpname(
    val id: String,
    val conductedBy: String,
    val status: OpnameStatus,
    val startedAt: Long,
    val completedAt: Long?,
    val updatedAt: Long,
    val syncStatus: SyncStatus,
    val deviceId: String,
)
