package com.wfx.warungpos.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "menu_categories")
data class MenuCategoryEntity(
    @PrimaryKey val id: String,
    val name: String,
    val sortOrder: Int,
    val updatedAt: Long,
    val syncStatus: String,
    val deviceId: String,
)
