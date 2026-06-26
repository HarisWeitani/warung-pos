package com.wfx.warungpos.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "shifts",
    indices = [Index("status")],
)
data class ShiftEntity(
    @PrimaryKey val id: String,
    val openedBy: String,
    val closedBy: String?,
    val status: String,
    val openedAt: Long,
    val closedAt: Long?,
    val openingFloat: Long,
    val closingFloat: Long?,
    val updatedAt: Long,
    val syncStatus: String,
    val deviceId: String,
)
