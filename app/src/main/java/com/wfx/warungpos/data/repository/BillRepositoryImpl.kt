package com.wfx.warungpos.data.repository

import com.wfx.warungpos.core.common.SessionManager
import com.wfx.warungpos.core.common.SyncStatus
import com.wfx.warungpos.core.util.DateUtil
import com.wfx.warungpos.data.local.dao.BillDao
import com.wfx.warungpos.data.local.mapper.toDomain
import com.wfx.warungpos.data.local.mapper.toEntity
import com.wfx.warungpos.data.remote.sync.SyncCoordinator
import com.wfx.warungpos.domain.model.Bill
import com.wfx.warungpos.domain.repository.BillRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BillRepositoryImpl @Inject constructor(
    private val billDao: BillDao,
    private val sessionManager: SessionManager,
    private val sync: SyncCoordinator,
) : BillRepository {

    override fun observeOpenBills(): Flow<List<Bill>> =
        billDao.observeOpenBills().map { it.map { e -> e.toDomain() } }

    override fun observeBillById(id: String): Flow<Bill?> =
        billDao.observeById(id).map { it?.toDomain() }

    override fun observeBillsForShift(shiftId: String): Flow<List<Bill>> =
        billDao.observeBillsForShift(shiftId).map { it.map { e -> e.toDomain() } }

    override suspend fun getBill(id: String): Bill? = billDao.getById(id)?.toDomain()

    override suspend fun saveBill(bill: Bill) {
        billDao.upsert(
            bill.copy(
                syncStatus = SyncStatus.PENDING,
                updatedAt = DateUtil.nowEpochMs(),
                deviceId = sessionManager.deviceId,
            ).toEntity()
        )
        sync.notifyPendingSync()
    }

    override suspend fun getOpenBills(): List<Bill> =
        billDao.getOpenBills().map { it.toDomain() }

    override suspend fun getPaidBillsForShift(shiftId: String): List<Bill> =
        billDao.getPaidBillsForShift(shiftId).map { it.toDomain() }

    override suspend fun getPaidBillsInRange(startEpoch: Long, endEpoch: Long): List<Bill> =
        billDao.getPaidBillsInRange(startEpoch, endEpoch).map { it.toDomain() }
}
