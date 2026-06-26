package com.wfx.warungpos.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "stock_items")
data class StockItemEntity(
    @PrimaryKey val id: String,
    val name: String,
    val unit: String,
    val currentQty: Double,
    val reorderPoint: Double,
    val updatedAt: Long,
    val syncStatus: String,
    val deviceId: String,
)
