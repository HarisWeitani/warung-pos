package com.wfx.warungpos.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "stock_batches",
    foreignKeys = [ForeignKey(
        entity = StockItemEntity::class,
        parentColumns = ["id"],
        childColumns = ["stockItemId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("stockItemId")],
)
data class StockBatchEntity(
    @PrimaryKey val id: String,
    val stockItemId: String,
    val qty: Double,
    val costPerUnit: Long,
    val receivedAt: Long,
    val expiresAt: Long?,
    val updatedAt: Long,
    val syncStatus: String,
    val deviceId: String,
)
