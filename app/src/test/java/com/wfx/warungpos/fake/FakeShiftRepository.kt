package com.wfx.warungpos.fake

import com.wfx.warungpos.core.common.ShiftStatus
import com.wfx.warungpos.domain.model.Shift
import com.wfx.warungpos.domain.model.ZReport
import com.wfx.warungpos.domain.repository.ShiftRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class FakeShiftRepository : ShiftRepository {
    val shifts = mutableMapOf<String, Shift>()
    val zReports = mutableMapOf<String, ZReport>()

    override fun observeOpenShift(): Flow<Shift?> =
        flowOf(shifts.values.firstOrNull { it.status == ShiftStatus.OPEN })

    override suspend fun getOpenShift(): Shift? = shifts.values.firstOrNull { it.status == ShiftStatus.OPEN }

    override suspend fun saveShift(shift: Shift) {
        shifts[shift.id] = shift
    }

    override suspend fun getRecentShifts(limit: Int): List<Shift> =
        shifts.values.sortedByDescending { it.openedAt }.take(limit)

    override suspend fun saveZReport(report: ZReport) {
        zReports[report.shiftId] = report
    }

    override suspend fun getZReport(shiftId: String): ZReport? = zReports[shiftId]
}
