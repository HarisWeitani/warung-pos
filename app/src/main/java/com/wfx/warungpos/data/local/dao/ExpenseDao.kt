package com.wfx.warungpos.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.wfx.warungpos.data.local.entity.ExpenseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {
    @Upsert
    suspend fun upsert(entity: ExpenseEntity)

    @Query("SELECT * FROM expenses ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses WHERE shiftId = :shiftId ORDER BY createdAt DESC")
    fun observeForShift(shiftId: String): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses WHERE createdAt BETWEEN :startEpoch AND :endEpoch ORDER BY createdAt DESC")
    suspend fun getInRange(startEpoch: Long, endEpoch: Long): List<ExpenseEntity>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM expenses WHERE shiftId = :shiftId")
    suspend fun totalForShift(shiftId: String): Long

    @Query("SELECT * FROM expenses WHERE id = :id")
    suspend fun getById(id: String): ExpenseEntity?

    @Query("SELECT * FROM expenses WHERE syncStatus = 'PENDING'")
    suspend fun getPendingSync(): List<ExpenseEntity>
}
