package com.wfx.warungpos.domain.repository

import com.wfx.warungpos.domain.model.Shift
import com.wfx.warungpos.domain.model.ZReport
import kotlinx.coroutines.flow.Flow

interface ShiftRepository {
    fun observeOpenShift(): Flow<Shift?>
    suspend fun getOpenShift(): Shift?
    suspend fun saveShift(shift: Shift)

    /** Atomically opens [shift] only if no shift is currently OPEN (checked and inserted in the
     * same DB transaction). Returns false (and leaves [shift] un-persisted) if another shift was
     * already open — the caller should re-fetch via [getOpenShift] instead of treating this as
     * a failure. */
    suspend fun openShiftIfNoneOpen(shift: Shift): Boolean
    suspend fun getRecentShifts(limit: Int): List<Shift>
    suspend fun saveZReport(report: ZReport)
    suspend fun getZReport(shiftId: String): ZReport?
}
