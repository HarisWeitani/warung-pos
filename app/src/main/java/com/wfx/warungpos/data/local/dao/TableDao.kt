package com.wfx.warungpos.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.wfx.warungpos.data.local.entity.TableEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TableDao {
    @Upsert
    suspend fun upsert(entity: TableEntity)

    @Upsert
    suspend fun upsertAll(entities: List<TableEntity>)

    @Query("SELECT * FROM tables WHERE isActive = 1 ORDER BY label ASC")
    fun observeActive(): Flow<List<TableEntity>>

    @Query("SELECT * FROM tables ORDER BY label ASC")
    fun observeAll(): Flow<List<TableEntity>>

    @Query("SELECT * FROM tables WHERE id = :id")
    suspend fun getById(id: String): TableEntity?

    @Query("SELECT * FROM tables WHERE syncStatus = 'PENDING'")
    suspend fun getPendingSync(): List<TableEntity>

    @Query("DELETE FROM tables WHERE id = :id")
    suspend fun deleteById(id: String)
}
