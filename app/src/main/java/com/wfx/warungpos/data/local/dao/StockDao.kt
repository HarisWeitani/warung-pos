package com.wfx.warungpos.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.wfx.warungpos.data.local.entity.MenuItemIngredientEntity
import com.wfx.warungpos.data.local.entity.StockBatchEntity
import com.wfx.warungpos.data.local.entity.StockItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StockDao {
    @Upsert
    suspend fun upsertItem(entity: StockItemEntity)

    @Upsert
    suspend fun upsertBatch(entity: StockBatchEntity)

    @Upsert
    suspend fun upsertIngredient(entity: MenuItemIngredientEntity)

    @Query("SELECT * FROM stock_items ORDER BY name ASC")
    fun observeAll(): Flow<List<StockItemEntity>>

    @Query("SELECT * FROM stock_items ORDER BY name ASC")
    suspend fun getAllOnce(): List<StockItemEntity>

    @Query("SELECT * FROM stock_items WHERE currentQty <= reorderPoint")
    fun observeLowStock(): Flow<List<StockItemEntity>>

    @Query("SELECT * FROM stock_items WHERE id = :id")
    suspend fun getItemById(id: String): StockItemEntity?

    @Query("SELECT * FROM stock_batches WHERE stockItemId = :stockItemId ORDER BY receivedAt ASC")
    suspend fun getBatchesForItem(stockItemId: String): List<StockBatchEntity>

    @Query("SELECT * FROM stock_batches ORDER BY receivedAt DESC")
    fun observeAllBatches(): Flow<List<StockBatchEntity>>

    @Query("SELECT * FROM menu_item_ingredients WHERE menuItemId = :menuItemId")
    suspend fun getIngredientsForMenuItem(menuItemId: String): List<MenuItemIngredientEntity>

    @Query("SELECT * FROM menu_item_ingredients WHERE menuItemId = :menuItemId")
    fun observeIngredientsForMenuItem(menuItemId: String): Flow<List<MenuItemIngredientEntity>>

    @Query("DELETE FROM menu_item_ingredients WHERE menuItemId = :menuItemId AND stockItemId = :stockItemId")
    suspend fun deleteIngredient(menuItemId: String, stockItemId: String)

    @Query("SELECT * FROM stock_items WHERE syncStatus = 'PENDING'")
    suspend fun getPendingItems(): List<StockItemEntity>

    @Query("SELECT * FROM stock_batches WHERE syncStatus = 'PENDING'")
    suspend fun getPendingBatches(): List<StockBatchEntity>

    @Query("SELECT * FROM menu_item_ingredients WHERE syncStatus = 'PENDING'")
    suspend fun getPendingIngredients(): List<MenuItemIngredientEntity>

    @Query("UPDATE stock_items SET currentQty = :qty, updatedAt = :updatedAt, syncStatus = 'PENDING' WHERE id = :id")
    suspend fun updateQty(id: String, qty: Double, updatedAt: Long)

    @Query("UPDATE stock_items SET currentQty = currentQty - :amount, updatedAt = :updatedAt, syncStatus = 'PENDING' WHERE id = :id")
    suspend fun deductQty(id: String, amount: Double, updatedAt: Long)
}
