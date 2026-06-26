package com.wfx.warungpos.domain.model

import com.wfx.warungpos.core.common.SyncStatus

data class VariantOption(
    val id: String,
    val variantGroupId: String,
    val name: String,
    val priceDelta: Long,
    val updatedAt: Long,
    val syncStatus: SyncStatus,
    val deviceId: String,
)
