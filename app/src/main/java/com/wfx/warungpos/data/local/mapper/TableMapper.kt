package com.wfx.warungpos.data.local.mapper

import com.wfx.warungpos.core.common.SyncStatus
import com.wfx.warungpos.data.local.entity.TableEntity
import com.wfx.warungpos.domain.model.Table

fun TableEntity.toDomain() = Table(
    id = id,
    label = label,
    isActive = isActive,
    updatedAt = updatedAt,
    syncStatus = SyncStatus.valueOf(syncStatus),
    deviceId = deviceId,
)

fun Table.toEntity() = TableEntity(
    id = id,
    label = label,
    isActive = isActive,
    updatedAt = updatedAt,
    syncStatus = syncStatus.name,
    deviceId = deviceId,
)
