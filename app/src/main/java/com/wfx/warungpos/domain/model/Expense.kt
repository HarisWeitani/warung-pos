package com.wfx.warungpos.domain.model

import com.wfx.warungpos.core.common.ExpenseCategory
import com.wfx.warungpos.core.common.SyncStatus

data class Expense(
    val id: String,
    val shiftId: String?,
    val category: ExpenseCategory,
    val amount: Long,
    val note: String?,
    val createdBy: String,
    val createdAt: Long,
    val updatedAt: Long,
    val syncStatus: SyncStatus,
    val deviceId: String,
)
