package com.wfx.warungpos.domain.model

import com.wfx.warungpos.core.common.BillStatus
import com.wfx.warungpos.core.common.SyncStatus
import com.wfx.warungpos.core.common.VoidReason

data class Bill(
    val id: String,
    val status: BillStatus,
    val sessionLabel: String,
    val createdAt: Long,
    val paidAt: Long?,
    val subtotal: Long,
    val discountTotal: Long,
    val grandTotal: Long,
    val note: String?,
    val shiftId: String?,
    val voidReason: VoidReason?,
    val voidedBy: String?,
    val updatedAt: Long,
    val syncStatus: SyncStatus,
    val deviceId: String,
)
