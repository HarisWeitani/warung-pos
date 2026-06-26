package com.wfx.warungpos.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tables")
data class TableEntity(
    @PrimaryKey val id: String,
    val label: String?,
    val isActive: Boolean,
    val updatedAt: Long,
    val syncStatus: String,
    val deviceId: String,
)
