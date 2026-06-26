package com.wfx.warungpos.domain.model

import com.wfx.warungpos.core.common.SyncStatus

data class MenuCategory(
    val id: String,
    val name: String,
    val sortOrder: Int,
    val updatedAt: Long,
    val syncStatus: SyncStatus,
    val deviceId: String,
)
