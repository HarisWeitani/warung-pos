package com.wfx.warungpos.data.local.mapper

import com.wfx.warungpos.core.common.ShiftStatus
import com.wfx.warungpos.core.common.SyncStatus
import com.wfx.warungpos.data.local.entity.ShiftEntity
import com.wfx.warungpos.data.local.entity.ZReportEntity
import com.wfx.warungpos.domain.model.Shift
import com.wfx.warungpos.domain.model.ZReport

fun ShiftEntity.toDomain() = Shift(
    id = id,
    openedBy = openedBy,
    closedBy = closedBy,
    status = ShiftStatus.valueOf(status),
    openedAt = openedAt,
    closedAt = closedAt,
    openingFloat = openingFloat,
    closingFloat = closingFloat,
    updatedAt = updatedAt,
    syncStatus = SyncStatus.valueOf(syncStatus),
    deviceId = deviceId,
)

fun Shift.toEntity() = ShiftEntity(
    id = id,
    openedBy = openedBy,
    closedBy = closedBy,
    status = status.name,
    openedAt = openedAt,
    closedAt = closedAt,
    openingFloat = openingFloat,
    closingFloat = closingFloat,
    updatedAt = updatedAt,
    syncStatus = syncStatus.name,
    deviceId = deviceId,
)

fun ZReportEntity.toDomain() = ZReport(
    id = id,
    shiftId = shiftId,
    snapshotJson = snapshotJson,
    createdAt = createdAt,
)

fun ZReport.toEntity() = ZReportEntity(
    id = id,
    shiftId = shiftId,
    snapshotJson = snapshotJson,
    createdAt = createdAt,
)
