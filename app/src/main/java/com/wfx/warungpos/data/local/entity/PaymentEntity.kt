package com.wfx.warungpos.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "payments",
    foreignKeys = [
        ForeignKey(
            entity = BillEntity::class,
            parentColumns = ["id"],
            childColumns = ["billId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = PaymentMethodEntity::class,
            parentColumns = ["id"],
            childColumns = ["paymentMethodId"],
            onDelete = ForeignKey.NO_ACTION,
        ),
    ],
    indices = [Index("billId"), Index("paymentMethodId")],
)
data class PaymentEntity(
    @PrimaryKey val id: String,
    val billId: String,
    val paymentMethodId: String,
    val amount: Long,
    val change: Long,
    val paidAt: Long,
    val updatedAt: Long,
    val syncStatus: String,
    val deviceId: String,
)
