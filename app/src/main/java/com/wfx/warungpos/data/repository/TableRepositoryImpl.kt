package com.wfx.warungpos.data.repository

import com.wfx.warungpos.core.common.SessionManager
import com.wfx.warungpos.core.common.SyncStatus
import com.wfx.warungpos.core.util.DateUtil
import com.wfx.warungpos.data.local.dao.TableDao
import com.wfx.warungpos.data.local.mapper.toDomain
import com.wfx.warungpos.data.local.mapper.toEntity
import com.wfx.warungpos.data.remote.sync.SyncCoordinator
import com.wfx.warungpos.domain.model.Table
import com.wfx.warungpos.domain.repository.TableRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TableRepositoryImpl @Inject constructor(
    private val tableDao: TableDao,
    private val sessionManager: SessionManager,
    private val sync: SyncCoordinator,
) : TableRepository {

    override fun observeActiveTables(): Flow<List<Table>> =
        tableDao.observeActive().map { it.map { e -> e.toDomain() } }

    override fun observeAllTables(): Flow<List<Table>> =
        tableDao.observeAll().map { it.map { e -> e.toDomain() } }

    override suspend fun getTable(id: String): Table? = tableDao.getById(id)?.toDomain()

    override suspend fun saveTable(table: Table) {
        tableDao.upsert(
            table.copy(
                syncStatus = SyncStatus.PENDING,
                updatedAt = DateUtil.nowEpochMs(),
                deviceId = sessionManager.deviceId,
            ).toEntity()
        )
        sync.notifyPendingSync()
    }

    override suspend fun deleteTable(id: String) = tableDao.deleteById(id)
}
