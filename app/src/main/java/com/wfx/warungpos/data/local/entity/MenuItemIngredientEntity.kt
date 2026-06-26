package com.wfx.warungpos.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "menu_item_ingredients",
    primaryKeys = ["menuItemId", "stockItemId"],
    foreignKeys = [
        ForeignKey(
            entity = MenuItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["menuItemId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = StockItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["stockItemId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("menuItemId"), Index("stockItemId")],
)
data class MenuItemIngredientEntity(
    val menuItemId: String,
    val stockItemId: String,
    val qtyPerServing: Double,
    val updatedAt: Long,
    val syncStatus: String,
    val deviceId: String,
)
