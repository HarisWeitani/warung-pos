package com.wfx.warungpos.data.local.mapper

import com.wfx.warungpos.core.common.BillStatus
import com.wfx.warungpos.core.common.BillType
import com.wfx.warungpos.core.common.SyncStatus
import com.wfx.warungpos.core.common.VoidReason
import com.wfx.warungpos.data.local.entity.BillEntity
import com.wfx.warungpos.domain.model.Bill

fun BillEntity.toDomain() = Bill(
    id = id,
    tableId = tableId,
    type = BillType.valueOf(type),
    status = BillStatus.valueOf(status),
    sessionLabel = sessionLabel,
    createdAt = createdAt,
    paidAt = paidAt,
    subtotal = subtotal,
    discountTotal = discountTotal,
    grandTotal = grandTotal,
    note = note,
    shiftId = shiftId,
    voidReason = voidReason?.let { VoidReason.valueOf(it) },
    voidedBy = voidedBy,
    updatedAt = updatedAt,
    syncStatus = SyncStatus.valueOf(syncStatus),
    deviceId = deviceId,
)

fun Bill.toEntity() = BillEntity(
    id = id,
    tableId = tableId,
    type = type.name,
    status = status.name,
    sessionLabel = sessionLabel,
    createdAt = createdAt,
    paidAt = paidAt,
    subtotal = subtotal,
    discountTotal = discountTotal,
    grandTotal = grandTotal,
    note = note,
    shiftId = shiftId,
    voidReason = voidReason?.name,
    voidedBy = voidedBy,
    updatedAt = updatedAt,
    syncStatus = syncStatus.name,
    deviceId = deviceId,
)
