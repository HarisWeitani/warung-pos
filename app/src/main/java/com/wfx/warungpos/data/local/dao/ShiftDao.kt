package com.wfx.warungpos.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.wfx.warungpos.data.local.entity.ShiftEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ShiftDao {
    @Upsert
    suspend fun upsert(entity: ShiftEntity)

    @Query("SELECT * FROM shifts WHERE status = 'OPEN' LIMIT 1")
    fun observeOpenShift(): Flow<ShiftEntity?>

    @Query("SELECT * FROM shifts WHERE status = 'OPEN' LIMIT 1")
    suspend fun getOpenShift(): ShiftEntity?

    @Query("SELECT * FROM shifts WHERE id = :id")
    suspend fun getById(id: String): ShiftEntity?

    @Query("SELECT * FROM shifts ORDER BY openedAt DESC LIMIT :limit")
    suspend fun getRecent(limit: Int): List<ShiftEntity>

    @Query("SELECT * FROM shifts WHERE syncStatus = 'PENDING'")
    suspend fun getPendingSync(): List<ShiftEntity>
}
