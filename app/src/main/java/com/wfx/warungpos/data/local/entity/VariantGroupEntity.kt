package com.wfx.warungpos.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "variant_groups",
    foreignKeys = [ForeignKey(
        entity = MenuItemEntity::class,
        parentColumns = ["id"],
        childColumns = ["menuItemId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("menuItemId")],
)
data class VariantGroupEntity(
    @PrimaryKey val id: String,
    val menuItemId: String,
    val name: String,
    val selectionType: String,
    val isRequired: Boolean,
    val updatedAt: Long,
    val syncStatus: String,
    val deviceId: String,
)
