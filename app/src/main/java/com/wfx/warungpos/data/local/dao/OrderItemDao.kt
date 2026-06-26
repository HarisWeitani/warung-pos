package com.wfx.warungpos.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.wfx.warungpos.data.local.entity.OrderItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface OrderItemDao {
    @Upsert
    suspend fun upsert(entity: OrderItemEntity)

    @Upsert
    suspend fun upsertAll(entities: List<OrderItemEntity>)

    @Query("SELECT * FROM order_items WHERE billId = :billId ORDER BY createdAt ASC")
    fun observeForBill(billId: String): Flow<List<OrderItemEntity>>

    @Query("SELECT * FROM order_items WHERE billId = :billId AND status != 'VOID'")
    suspend fun getActiveForBill(billId: String): List<OrderItemEntity>

    @Query("SELECT * FROM order_items WHERE id = :id")
    suspend fun getById(id: String): OrderItemEntity?

    @Query("SELECT * FROM order_items WHERE syncStatus = 'PENDING'")
    suspend fun getPendingSync(): List<OrderItemEntity>

    @Query("""
        UPDATE order_items
        SET status = 'VOID', voidReason = :reason, voidedBy = :voidedBy,
            updatedAt = :updatedAt, syncStatus = 'PENDING'
        WHERE id = :id
    """)
    suspend fun voidItem(id: String, reason: String, voidedBy: String, updatedAt: Long)
}
