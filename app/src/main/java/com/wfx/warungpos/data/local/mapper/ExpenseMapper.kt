package com.wfx.warungpos.data.local.mapper

import com.wfx.warungpos.core.common.ExpenseCategory
import com.wfx.warungpos.core.common.SyncStatus
import com.wfx.warungpos.data.local.entity.ExpenseEntity
import com.wfx.warungpos.domain.model.Expense

fun ExpenseEntity.toDomain() = Expense(
    id = id,
    shiftId = shiftId,
    category = ExpenseCategory.valueOf(category),
    amount = amount,
    note = note,
    createdBy = createdBy,
    createdAt = createdAt,
    updatedAt = updatedAt,
    syncStatus = SyncStatus.valueOf(syncStatus),
    deviceId = deviceId,
)

fun Expense.toEntity() = ExpenseEntity(
    id = id,
    shiftId = shiftId,
    category = category.name,
    amount = amount,
    note = note,
    createdBy = createdBy,
    createdAt = createdAt,
    updatedAt = updatedAt,
    syncStatus = syncStatus.name,
    deviceId = deviceId,
)
