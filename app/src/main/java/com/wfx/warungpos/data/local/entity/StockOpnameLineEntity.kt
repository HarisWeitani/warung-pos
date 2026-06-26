package com.wfx.warungpos.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "stock_opname_lines",
    foreignKeys = [
        ForeignKey(
            entity = StockOpnameEntity::class,
            parentColumns = ["id"],
            childColumns = ["opnameId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = StockItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["stockItemId"],
            onDelete = ForeignKey.NO_ACTION,
        ),
    ],
    indices = [Index("opnameId"), Index("stockItemId")],
)
data class StockOpnameLineEntity(
    @PrimaryKey val id: String,
    val opnameId: String,
    val stockItemId: String,
    val systemQty: Double,
    val countedQty: Double,
    val variance: Double,
    val varianceReason: String?,
    val updatedAt: Long,
    val syncStatus: String,
    val deviceId: String,
)
