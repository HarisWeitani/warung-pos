package com.wfx.warungpos.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "order_items",
    foreignKeys = [
        ForeignKey(
            entity = BillEntity::class,
            parentColumns = ["id"],
            childColumns = ["billId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = MenuItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["menuItemId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("billId"), Index("menuItemId"), Index("status")],
)
data class OrderItemEntity(
    @PrimaryKey val id: String,
    val billId: String,
    val menuItemId: String?,
    val nameSnapshot: String,
    val priceSnapshot: Long,
    val quantity: Int,
    val selectedVariantsJson: String,
    val lineTotal: Long,
    val status: String,
    val voidReason: String?,
    val voidedBy: String?,
    val createdAt: Long,
    val updatedAt: Long,
    val syncStatus: String,
    val deviceId: String,
)
