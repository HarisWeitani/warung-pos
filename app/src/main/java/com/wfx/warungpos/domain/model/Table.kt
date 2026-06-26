package com.wfx.warungpos.domain.model

import com.wfx.warungpos.core.common.SyncStatus

data class Table(
    val id: String,
    val label: String?,
    val isActive: Boolean,
    val updatedAt: Long,
    val syncStatus: SyncStatus,
    val deviceId: String,
)
