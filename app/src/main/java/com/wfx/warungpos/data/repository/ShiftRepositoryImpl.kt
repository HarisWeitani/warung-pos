package com.wfx.warungpos.data.repository

import com.wfx.warungpos.core.common.SessionManager
import com.wfx.warungpos.core.common.SyncStatus
import com.wfx.warungpos.core.util.DateUtil
import com.wfx.warungpos.data.local.dao.ShiftDao
import com.wfx.warungpos.data.local.dao.ZReportDao
import com.wfx.warungpos.data.local.mapper.toDomain
import com.wfx.warungpos.data.local.mapper.toEntity
import com.wfx.warungpos.data.remote.sync.SyncCoordinator
import com.wfx.warungpos.domain.model.Shift
import com.wfx.warungpos.domain.model.ZReport
import com.wfx.warungpos.domain.repository.ShiftRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ShiftRepositoryImpl @Inject constructor(
    private val shiftDao: ShiftDao,
    private val zReportDao: ZReportDao,
    private val sessionManager: SessionManager,
    private val sync: SyncCoordinator,
) : ShiftRepository {

    override fun observeOpenShift(): Flow<Shift?> =
        shiftDao.observeOpenShift().map { it?.toDomain() }

    override suspend fun getOpenShift(): Shift? = shiftDao.getOpenShift()?.toDomain()

    override suspend fun saveShift(shift: Shift) {
        shiftDao.upsert(
            shift.copy(
                syncStatus = SyncStatus.PENDING,
                updatedAt = DateUtil.nowEpochMs(),
                deviceId = sessionManager.deviceId,
            ).toEntity()
        )
        sync.notifyPendingSync()
    }

    override suspend fun openShiftIfNoneOpen(shift: Shift): Boolean {
        val opened = shiftDao.openIfNoneOpen(
            shift.copy(
                syncStatus = SyncStatus.PENDING,
                updatedAt = DateUtil.nowEpochMs(),
                deviceId = sessionManager.deviceId,
            ).toEntity()
        )
        if (opened) sync.notifyPendingSync()
        return opened
    }

    override suspend fun getRecentShifts(limit: Int): List<Shift> =
        shiftDao.getRecent(limit).map { it.toDomain() }

    override suspend fun saveZReport(report: ZReport) =
        zReportDao.upsert(report.toEntity())

    override suspend fun getZReport(shiftId: String): ZReport? =
        zReportDao.getForShift(shiftId)?.toDomain()
}
