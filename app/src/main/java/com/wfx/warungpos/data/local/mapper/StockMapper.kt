package com.wfx.warungpos.data.local.mapper

import com.wfx.warungpos.core.common.OpnameStatus
import com.wfx.warungpos.core.common.SyncStatus
import com.wfx.warungpos.core.common.VarianceReason
import com.wfx.warungpos.data.local.entity.StockBatchEntity
import com.wfx.warungpos.data.local.entity.StockItemEntity
import com.wfx.warungpos.data.local.entity.StockOpnameEntity
import com.wfx.warungpos.data.local.entity.StockOpnameLineEntity
import com.wfx.warungpos.domain.model.StockBatch
import com.wfx.warungpos.domain.model.StockItem
import com.wfx.warungpos.domain.model.StockOpname
import com.wfx.warungpos.domain.model.StockOpnameLine

fun StockItemEntity.toDomain() = StockItem(
    id = id,
    name = name,
    unit = unit,
    currentQty = currentQty,
    reorderPoint = reorderPoint,
    updatedAt = updatedAt,
    syncStatus = SyncStatus.valueOf(syncStatus),
    deviceId = deviceId,
)

fun StockItem.toEntity() = StockItemEntity(
    id = id,
    name = name,
    unit = unit,
    currentQty = currentQty,
    reorderPoint = reorderPoint,
    updatedAt = updatedAt,
    syncStatus = syncStatus.name,
    deviceId = deviceId,
)

fun StockBatchEntity.toDomain() = StockBatch(
    id = id,
    stockItemId = stockItemId,
    qty = qty,
    costPerUnit = costPerUnit,
    receivedAt = receivedAt,
    expiresAt = expiresAt,
    updatedAt = updatedAt,
    syncStatus = SyncStatus.valueOf(syncStatus),
    deviceId = deviceId,
)

fun StockBatch.toEntity() = StockBatchEntity(
    id = id,
    stockItemId = stockItemId,
    qty = qty,
    costPerUnit = costPerUnit,
    receivedAt = receivedAt,
    expiresAt = expiresAt,
    updatedAt = updatedAt,
    syncStatus = syncStatus.name,
    deviceId = deviceId,
)

fun StockOpnameEntity.toDomain() = StockOpname(
    id = id,
    conductedBy = conductedBy,
    status = OpnameStatus.valueOf(status),
    startedAt = startedAt,
    completedAt = completedAt,
    updatedAt = updatedAt,
    syncStatus = SyncStatus.valueOf(syncStatus),
    deviceId = deviceId,
)

fun StockOpname.toEntity() = StockOpnameEntity(
    id = id,
    conductedBy = conductedBy,
    status = status.name,
    startedAt = startedAt,
    completedAt = completedAt,
    updatedAt = updatedAt,
    syncStatus = syncStatus.name,
    deviceId = deviceId,
)

fun StockOpnameLineEntity.toDomain() = StockOpnameLine(
    id = id,
    opnameId = opnameId,
    stockItemId = stockItemId,
    systemQty = systemQty,
    countedQty = countedQty,
    variance = variance,
    varianceReason = varianceReason?.let { VarianceReason.valueOf(it) },
    updatedAt = updatedAt,
    syncStatus = SyncStatus.valueOf(syncStatus),
    deviceId = deviceId,
)

fun StockOpnameLine.toEntity() = StockOpnameLineEntity(
    id = id,
    opnameId = opnameId,
    stockItemId = stockItemId,
    systemQty = systemQty,
    countedQty = countedQty,
    variance = variance,
    varianceReason = varianceReason?.name,
    updatedAt = updatedAt,
    syncStatus = syncStatus.name,
    deviceId = deviceId,
)
