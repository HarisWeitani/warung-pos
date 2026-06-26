package com.wfx.warungpos.domain.model

import com.wfx.warungpos.core.common.OrderItemStatus
import com.wfx.warungpos.core.common.SyncStatus
import com.wfx.warungpos.core.common.VoidReason

data class OrderItem(
    val id: String,
    val billId: String,
    val menuItemId: String?,
    val nameSnapshot: String,
    val priceSnapshot: Long,
    val quantity: Int,
    val selectedVariants: List<VariantSelection>,
    val lineTotal: Long,
    val status: OrderItemStatus,
    val voidReason: VoidReason?,
    val voidedBy: String?,
    val createdAt: Long,
    val updatedAt: Long,
    val syncStatus: SyncStatus,
    val deviceId: String,
)
