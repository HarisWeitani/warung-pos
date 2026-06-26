package com.wfx.warungpos.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "expenses",
    foreignKeys = [ForeignKey(
        entity = ShiftEntity::class,
        parentColumns = ["id"],
        childColumns = ["shiftId"],
        onDelete = ForeignKey.SET_NULL,
    )],
    indices = [Index("shiftId")],
)
data class ExpenseEntity(
    @PrimaryKey val id: String,
    val shiftId: String?,
    val category: String,
    val amount: Long,
    val note: String?,
    val createdBy: String,
    val createdAt: Long,
    val updatedAt: Long,
    val syncStatus: String,
    val deviceId: String,
)
