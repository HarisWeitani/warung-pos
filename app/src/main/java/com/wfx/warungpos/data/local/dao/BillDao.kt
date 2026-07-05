package com.wfx.warungpos.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.wfx.warungpos.data.local.entity.BillEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BillDao {
    @Upsert
    suspend fun upsert(entity: BillEntity)

    @Query("SELECT * FROM bills WHERE status = 'OPEN' ORDER BY createdAt DESC")
    fun observeOpenBills(): Flow<List<BillEntity>>

    @Query("SELECT * FROM bills WHERE shiftId = :shiftId ORDER BY createdAt DESC")
    fun observeBillsForShift(shiftId: String): Flow<List<BillEntity>>

    @Query("SELECT * FROM bills WHERE id = :id")
    fun observeById(id: String): Flow<BillEntity?>

    @Query("SELECT * FROM bills WHERE id = :id")
    suspend fun getById(id: String): BillEntity?

    @Query("SELECT * FROM bills WHERE shiftId = :shiftId AND status = 'PAID'")
    suspend fun getPaidBillsForShift(shiftId: String): List<BillEntity>

    @Query("""
        SELECT * FROM bills
        WHERE status = 'PAID' AND paidAt BETWEEN :startEpoch AND :endEpoch
        ORDER BY paidAt DESC
    """)
    suspend fun getPaidBillsInRange(startEpoch: Long, endEpoch: Long): List<BillEntity>

    @Query("SELECT * FROM bills WHERE status = 'OPEN' ORDER BY createdAt DESC")
    suspend fun getOpenBills(): List<BillEntity>

    @Query("SELECT * FROM bills WHERE syncStatus = 'PENDING'")
    suspend fun getPendingSync(): List<BillEntity>
}
