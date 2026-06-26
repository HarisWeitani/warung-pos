package com.wfx.warungpos.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.wfx.warungpos.data.local.entity.MenuCategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MenuCategoryDao {
    @Upsert
    suspend fun upsert(entity: MenuCategoryEntity)

    @Upsert
    suspend fun upsertAll(entities: List<MenuCategoryEntity>)

    @Query("SELECT * FROM menu_categories ORDER BY sortOrder ASC")
    fun observeAll(): Flow<List<MenuCategoryEntity>>

    @Query("SELECT * FROM menu_categories WHERE id = :id")
    suspend fun getById(id: String): MenuCategoryEntity?

    @Query("SELECT * FROM menu_categories WHERE syncStatus = 'PENDING'")
    suspend fun getPendingSync(): List<MenuCategoryEntity>

    @Query("DELETE FROM menu_categories WHERE id = :id")
    suspend fun deleteById(id: String)
}
