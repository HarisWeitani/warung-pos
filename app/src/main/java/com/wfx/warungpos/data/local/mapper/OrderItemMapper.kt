package com.wfx.warungpos.data.local.mapper

import com.wfx.warungpos.core.common.OrderItemStatus
import com.wfx.warungpos.core.common.SyncStatus
import com.wfx.warungpos.core.common.VoidReason
import com.wfx.warungpos.data.local.entity.OrderItemEntity
import com.wfx.warungpos.domain.model.OrderItem
import com.wfx.warungpos.domain.model.VariantSelection
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val json = Json { ignoreUnknownKeys = true }

fun OrderItemEntity.toDomain() = OrderItem(
    id = id,
    billId = billId,
    menuItemId = menuItemId,
    nameSnapshot = nameSnapshot,
    priceSnapshot = priceSnapshot,
    quantity = quantity,
    selectedVariants = runCatching {
        json.decodeFromString<List<VariantSelection>>(selectedVariantsJson)
    }.getOrDefault(emptyList()),
    lineTotal = lineTotal,
    status = OrderItemStatus.valueOf(status),
    voidReason = voidReason?.let { VoidReason.valueOf(it) },
    voidNote = voidNote,
    voidedBy = voidedBy,
    createdAt = createdAt,
    updatedAt = updatedAt,
    syncStatus = SyncStatus.valueOf(syncStatus),
    deviceId = deviceId,
)

fun OrderItem.toEntity() = OrderItemEntity(
    id = id,
    billId = billId,
    menuItemId = menuItemId,
    nameSnapshot = nameSnapshot,
    priceSnapshot = priceSnapshot,
    quantity = quantity,
    selectedVariantsJson = json.encodeToString(selectedVariants),
    lineTotal = lineTotal,
    status = status.name,
    voidReason = voidReason?.name,
    voidNote = voidNote,
    voidedBy = voidedBy,
    createdAt = createdAt,
    updatedAt = updatedAt,
    syncStatus = syncStatus.name,
    deviceId = deviceId,
)
