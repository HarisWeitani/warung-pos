package com.wfx.warungpos.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.wfx.warungpos.data.local.entity.ShiftEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ShiftDao {
    @Upsert
    suspend fun upsert(entity: ShiftEntity)

    @Query("SELECT * FROM shifts WHERE status = 'OPEN' ORDER BY openedAt DESC LIMIT 1")
    fun observeOpenShift(): Flow<ShiftEntity?>

    @Query("SELECT * FROM shifts WHERE status = 'OPEN' ORDER BY openedAt DESC LIMIT 1")
    suspend fun getOpenShift(): ShiftEntity?

    /**
     * DEFECT-003/008: opens [entity] only if no shift is currently OPEN, checked and inserted in
     * the same DB transaction. Room serializes concurrent writer transactions on one connection,
     * so this closes the check-then-act race that let two call sites (app-start and
     * create-bill) both observe "no open shift" and each insert their own OPEN row. Returns
     * whether [entity] was actually opened (false means another shift was already open — the
     * caller should re-fetch via [getOpenShift] rather than treat this as a failure).
     */
    @Transaction
    suspend fun openIfNoneOpen(entity: ShiftEntity): Boolean {
        if (getOpenShift() != null) return false
        upsert(entity)
        return true
    }

    @Query("SELECT * FROM shifts WHERE id = :id")
    suspend fun getById(id: String): ShiftEntity?

    @Query("SELECT * FROM shifts ORDER BY openedAt DESC LIMIT :limit")
    suspend fun getRecent(limit: Int): List<ShiftEntity>

    @Query("SELECT * FROM shifts WHERE syncStatus = 'PENDING'")
    suspend fun getPendingSync(): List<ShiftEntity>
}
