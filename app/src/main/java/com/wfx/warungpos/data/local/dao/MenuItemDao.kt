package com.wfx.warungpos.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.wfx.warungpos.data.local.entity.MenuItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MenuItemDao {
    @Upsert
    suspend fun upsert(entity: MenuItemEntity)

    @Upsert
    suspend fun upsertAll(entities: List<MenuItemEntity>)

    @Query("SELECT * FROM menu_items WHERE isAvailable = 1 ORDER BY name ASC")
    fun observeAvailable(): Flow<List<MenuItemEntity>>

    @Query("SELECT * FROM menu_items WHERE categoryId = :categoryId ORDER BY name ASC")
    fun observeByCategory(categoryId: String): Flow<List<MenuItemEntity>>

    @Query("SELECT * FROM menu_items ORDER BY name ASC")
    fun observeAll(): Flow<List<MenuItemEntity>>

    @Query("SELECT * FROM menu_items WHERE id = :id")
    suspend fun getById(id: String): MenuItemEntity?

    @Query("SELECT * FROM menu_items WHERE syncStatus = 'PENDING'")
    suspend fun getPendingSync(): List<MenuItemEntity>

    @Query("UPDATE menu_items SET isSoldOut = :soldOut, updatedAt = :updatedAt, syncStatus = 'PENDING' WHERE id = :id")
    suspend fun updateSoldOut(id: String, soldOut: Boolean, updatedAt: Long)

    @Query("DELETE FROM menu_items WHERE id = :id")
    suspend fun deleteById(id: String)
}
