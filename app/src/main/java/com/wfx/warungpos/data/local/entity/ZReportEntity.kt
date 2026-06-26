package com.wfx.warungpos.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "z_reports",
    foreignKeys = [ForeignKey(
        entity = ShiftEntity::class,
        parentColumns = ["id"],
        childColumns = ["shiftId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index(value = ["shiftId"], unique = true)],
)
data class ZReportEntity(
    @PrimaryKey val id: String,
    val shiftId: String,
    val snapshotJson: String,
    val createdAt: Long,
)
