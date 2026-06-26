package com.wfx.warungpos.data.repository

import com.wfx.warungpos.core.common.SessionManager
import com.wfx.warungpos.core.common.SyncStatus
import com.wfx.warungpos.core.util.DateUtil
import com.wfx.warungpos.data.local.dao.StockDao
import com.wfx.warungpos.data.local.dao.StockOpnameDao
import com.wfx.warungpos.data.local.mapper.toDomain
import com.wfx.warungpos.data.local.mapper.toEntity
import com.wfx.warungpos.data.remote.sync.SyncCoordinator
import com.wfx.warungpos.domain.model.StockBatch
import com.wfx.warungpos.domain.model.StockItem
import com.wfx.warungpos.domain.model.StockOpname
import com.wfx.warungpos.domain.model.StockOpnameLine
import com.wfx.warungpos.domain.repository.StockRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StockRepositoryImpl @Inject constructor(
    private val stockDao: StockDao,
    private val opnameDao: StockOpnameDao,
    private val sessionManager: SessionManager,
    private val sync: SyncCoordinator,
) : StockRepository {

    override fun observeAllItems(): Flow<List<StockItem>> =
        stockDao.observeAll().map { it.map { e -> e.toDomain() } }

    override fun observeLowStock(): Flow<List<StockItem>> =
        stockDao.observeLowStock().map { it.map { e -> e.toDomain() } }

    override suspend fun getItem(id: String): StockItem? = stockDao.getItemById(id)?.toDomain()

    override suspend fun saveItem(item: StockItem) {
        stockDao.upsertItem(
            item.copy(
                syncStatus = SyncStatus.PENDING,
                updatedAt = DateUtil.nowEpochMs(),
                deviceId = sessionManager.deviceId,
            ).toEntity()
        )
        sync.notifyPendingSync()
    }

    override suspend fun saveBatch(batch: StockBatch) {
        stockDao.upsertBatch(
            batch.copy(
                syncStatus = SyncStatus.PENDING,
                updatedAt = DateUtil.nowEpochMs(),
                deviceId = sessionManager.deviceId,
            ).toEntity()
        )
        sync.notifyPendingSync()
    }

    override fun observeOpnames(): Flow<List<StockOpname>> =
        opnameDao.observeAll().map { it.map { e -> e.toDomain() } }

    override suspend fun saveOpname(opname: StockOpname) {
        opnameDao.upsertOpname(
            opname.copy(
                syncStatus = SyncStatus.PENDING,
                updatedAt = DateUtil.nowEpochMs(),
                deviceId = sessionManager.deviceId,
            ).toEntity()
        )
        sync.notifyPendingSync()
    }

    override suspend fun saveLine(line: StockOpnameLine) {
        opnameDao.upsertLine(
            line.copy(
                syncStatus = SyncStatus.PENDING,
                updatedAt = DateUtil.nowEpochMs(),
                deviceId = sessionManager.deviceId,
            ).toEntity()
        )
    }
}
