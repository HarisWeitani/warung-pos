package com.wfx.warungpos.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "bills",
    foreignKeys = [
        ForeignKey(
            entity = TableEntity::class,
            parentColumns = ["id"],
            childColumns = ["tableId"],
            onDelete = ForeignKey.SET_NULL,
        ),
        ForeignKey(
            entity = ShiftEntity::class,
            parentColumns = ["id"],
            childColumns = ["shiftId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("tableId"), Index("shiftId"), Index("status")],
)
data class BillEntity(
    @PrimaryKey val id: String,
    val tableId: String?,
    val type: String,
    val status: String,
    val sessionLabel: String,
    val createdAt: Long,
    val paidAt: Long?,
    val subtotal: Long,
    val discountTotal: Long,
    val grandTotal: Long,
    val note: String?,
    val shiftId: String?,
    val voidReason: String?,
    val voidedBy: String?,
    val updatedAt: Long,
    val syncStatus: String,
    val deviceId: String,
)
