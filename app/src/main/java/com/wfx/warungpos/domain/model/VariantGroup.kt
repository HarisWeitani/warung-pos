package com.wfx.warungpos.domain.model

import com.wfx.warungpos.core.common.SyncStatus
import com.wfx.warungpos.core.common.VariantSelectionType

data class VariantGroup(
    val id: String,
    val menuItemId: String,
    val name: String,
    val selectionType: VariantSelectionType,
    val isRequired: Boolean,
    val updatedAt: Long,
    val syncStatus: SyncStatus,
    val deviceId: String,
)
