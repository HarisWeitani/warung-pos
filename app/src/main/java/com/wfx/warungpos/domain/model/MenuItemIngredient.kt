package com.wfx.warungpos.domain.model

import com.wfx.warungpos.core.common.SyncStatus

data class MenuItemIngredient(
    val menuItemId: String,
    val stockItemId: String,
    val qtyPerServing: Double,
    val updatedAt: Long,
    val syncStatus: SyncStatus,
    val deviceId: String,
)
