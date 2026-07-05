package com.wfx.warungpos.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.wfx.warungpos.data.local.entity.StockOpnameEntity
import com.wfx.warungpos.data.local.entity.StockOpnameLineEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StockOpnameDao {
    @Upsert
    suspend fun upsertOpname(entity: StockOpnameEntity)

    @Upsert
    suspend fun upsertLine(entity: StockOpnameLineEntity)

    @Upsert
    suspend fun upsertLines(entities: List<StockOpnameLineEntity>)

    @Query("SELECT * FROM stock_opnames ORDER BY startedAt DESC")
    fun observeAll(): Flow<List<StockOpnameEntity>>

    @Query("SELECT * FROM stock_opnames WHERE status = 'IN_PROGRESS' LIMIT 1")
    fun observeInProgress(): Flow<StockOpnameEntity?>

    @Query("SELECT * FROM stock_opnames WHERE id = :id")
    suspend fun getById(id: String): StockOpnameEntity?

    @Query("SELECT * FROM stock_opname_lines WHERE opnameId = :opnameId")
    suspend fun getLinesForOpname(opnameId: String): List<StockOpnameLineEntity>

    @Query("SELECT * FROM stock_opname_lines WHERE opnameId = :opnameId")
    fun observeLinesForOpname(opnameId: String): Flow<List<StockOpnameLineEntity>>

    @Query("SELECT * FROM stock_opnames WHERE syncStatus = 'PENDING'")
    suspend fun getPendingOpnames(): List<StockOpnameEntity>

    @Query("SELECT * FROM stock_opname_lines WHERE syncStatus = 'PENDING'")
    suspend fun getPendingLines(): List<StockOpnameLineEntity>
}
