package com.wfx.warungpos.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "stock_opnames",
    indices = [Index("status")],
)
data class StockOpnameEntity(
    @PrimaryKey val id: String,
    val conductedBy: String,
    val status: String,
    val startedAt: Long,
    val completedAt: Long?,
    val updatedAt: Long,
    val syncStatus: String,
    val deviceId: String,
)
