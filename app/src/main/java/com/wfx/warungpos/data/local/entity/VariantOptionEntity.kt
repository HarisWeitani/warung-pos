package com.wfx.warungpos.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "variant_options",
    foreignKeys = [ForeignKey(
        entity = VariantGroupEntity::class,
        parentColumns = ["id"],
        childColumns = ["variantGroupId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("variantGroupId")],
)
data class VariantOptionEntity(
    @PrimaryKey val id: String,
    val variantGroupId: String,
    val name: String,
    val priceDelta: Long,
    val updatedAt: Long,
    val syncStatus: String,
    val deviceId: String,
)
