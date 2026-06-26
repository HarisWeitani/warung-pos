package com.wfx.warungpos.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "payment_methods")
data class PaymentMethodEntity(
    @PrimaryKey val id: String,
    val name: String,
    val isActive: Boolean,
    val sortOrder: Int,
    val updatedAt: Long,
    val syncStatus: String,
    val deviceId: String,
)
