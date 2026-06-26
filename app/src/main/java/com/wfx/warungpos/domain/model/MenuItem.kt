package com.wfx.warungpos.domain.model

import com.wfx.warungpos.core.common.SyncStatus

data class MenuItem(
    val id: String,
    val categoryId: String?,
    val name: String,
    val basePrice: Long,
    val isAvailable: Boolean,
    val isSoldOut: Boolean,
    val updatedAt: Long,
    val syncStatus: SyncStatus,
    val deviceId: String,
)
