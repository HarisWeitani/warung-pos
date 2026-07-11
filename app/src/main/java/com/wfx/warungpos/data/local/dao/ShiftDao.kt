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
     * DEFECT-016: [getOpenShift]/[observeOpenShift] only ever surface the single
     * most-recently-opened shift. When a second device (or a historical un-closed session) has
     * left another shift OPEN, that older shift — and any bill still attached to it — becomes
     * permanently unreachable through the normal Close Day flow, since it's never the "most
     * recent" one. These return every OPEN shift so the UI can surface and let the owner close
     * each of them, not just the newest.
     */
    @Query("SELECT * FROM shifts WHERE status = 'OPEN' ORDER BY openedAt DESC")
    fun observeAllOpenShifts(): Flow<List<ShiftEntity>>

    @Query("SELECT * FROM shifts WHERE status = 'OPEN' ORDER BY openedAt DESC")
    suspend fun getAllOpenShifts(): List<ShiftEntity>

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
