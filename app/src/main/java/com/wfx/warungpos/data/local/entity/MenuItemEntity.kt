package com.wfx.warungpos.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "menu_items",
    foreignKeys = [ForeignKey(
        entity = MenuCategoryEntity::class,
        parentColumns = ["id"],
        childColumns = ["categoryId"],
        onDelete = ForeignKey.SET_NULL,
    )],
    indices = [Index("categoryId")],
)
data class MenuItemEntity(
    @PrimaryKey val id: String,
    val categoryId: String?,
    val name: String,
    val basePrice: Long,
    val isAvailable: Boolean,
    val isSoldOut: Boolean,
    val updatedAt: Long,
    val syncStatus: String,
    val deviceId: String,
)
