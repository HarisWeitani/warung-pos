package com.wfx.warungpos.domain.repository

import com.wfx.warungpos.domain.model.Shift
import com.wfx.warungpos.domain.model.ZReport
import kotlinx.coroutines.flow.Flow

interface ShiftRepository {
    fun observeOpenShift(): Flow<Shift?>
    suspend fun getOpenShift(): Shift?
    suspend fun saveShift(shift: Shift)
    suspend fun getRecentShifts(limit: Int): List<Shift>
    suspend fun saveZReport(report: ZReport)
    suspend fun getZReport(shiftId: String): ZReport?
}
