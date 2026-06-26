package com.wfx.warungpos.domain.model

import com.wfx.warungpos.core.common.ShiftStatus
import com.wfx.warungpos.core.common.SyncStatus

data class Shift(
    val id: String,
    val openedBy: String,
    val closedBy: String?,
    val status: ShiftStatus,
    val openedAt: Long,
    val closedAt: Long?,
    val openingFloat: Long,
    val closingFloat: Long?,
    val updatedAt: Long,
    val syncStatus: SyncStatus,
    val deviceId: String,
)
