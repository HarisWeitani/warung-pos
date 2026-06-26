package com.wfx.warungpos.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.wfx.warungpos.data.local.entity.ZReportEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ZReportDao {
    @Upsert
    suspend fun upsert(entity: ZReportEntity)

    @Query("SELECT * FROM z_reports WHERE shiftId = :shiftId LIMIT 1")
    suspend fun getForShift(shiftId: String): ZReportEntity?

    @Query("SELECT * FROM z_reports ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<ZReportEntity>>

    @Query("SELECT * FROM z_reports WHERE id = :id")
    suspend fun getById(id: String): ZReportEntity?
}
