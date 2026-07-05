package com.wfx.warungpos.data.repository

import androidx.room.withTransaction
import com.wfx.warungpos.core.common.SessionManager
import com.wfx.warungpos.core.common.SyncStatus
import com.wfx.warungpos.core.util.DateUtil
import com.wfx.warungpos.data.local.dao.StockDao
import com.wfx.warungpos.data.local.dao.StockOpnameDao
import com.wfx.warungpos.data.local.db.WarungDatabase
import com.wfx.warungpos.data.local.mapper.toDomain
import com.wfx.warungpos.data.local.mapper.toEntity
import com.wfx.warungpos.data.remote.sync.SyncCoordinator
import com.wfx.warungpos.domain.model.MenuItemIngredient
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
    private val database: WarungDatabase,
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

    override suspend fun getAllItemsOnce(): List<StockItem> = stockDao.getAllOnce().map { it.toDomain() }

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

    override fun observeAllBatches(): Flow<List<StockBatch>> =
        stockDao.observeAllBatches().map { it.map { e -> e.toDomain() } }

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

    override fun observeIngredientsForMenuItem(menuItemId: String): Flow<List<MenuItemIngredient>> =
        stockDao.observeIngredientsForMenuItem(menuItemId).map { it.map { e -> e.toDomain() } }

    override suspend fun getIngredientsForMenuItem(menuItemId: String): List<MenuItemIngredient> =
        stockDao.getIngredientsForMenuItem(menuItemId).map { it.toDomain() }

    override suspend fun saveIngredient(ingredient: MenuItemIngredient) {
        stockDao.upsertIngredient(
            ingredient.copy(
                syncStatus = SyncStatus.PENDING,
                updatedAt = DateUtil.nowEpochMs(),
                deviceId = sessionManager.deviceId,
            ).toEntity()
        )
        sync.notifyPendingSync()
    }

    override suspend fun deleteIngredient(menuItemId: String, stockItemId: String) {
        stockDao.deleteIngredient(menuItemId, stockItemId)
    }

    override fun observeOpnames(): Flow<List<StockOpname>> =
        opnameDao.observeAll().map { it.map { e -> e.toDomain() } }

    override fun observeInProgressOpname(): Flow<StockOpname?> =
        opnameDao.observeInProgress().map { it?.toDomain() }

    override suspend fun getLinesForOpname(opnameId: String): List<StockOpnameLine> =
        opnameDao.getLinesForOpname(opnameId).map { it.toDomain() }

    override fun observeLinesForOpname(opnameId: String): Flow<List<StockOpnameLine>> =
        opnameDao.observeLinesForOpname(opnameId).map { it.map { e -> e.toDomain() } }

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
        sync.notifyPendingSync()
    }

    override suspend fun saveLines(lines: List<StockOpnameLine>) {
        val now = DateUtil.nowEpochMs()
        val deviceId = sessionManager.deviceId
        opnameDao.upsertLines(
            lines.map { it.copy(syncStatus = SyncStatus.PENDING, updatedAt = now, deviceId = deviceId).toEntity() }
        )
        sync.notifyPendingSync()
    }

    override suspend fun startOpname(opname: StockOpname, lines: List<StockOpnameLine>) {
        val now = DateUtil.nowEpochMs()
        val deviceId = sessionManager.deviceId
        database.withTransaction {
            opnameDao.upsertOpname(
                opname.copy(syncStatus = SyncStatus.PENDING, updatedAt = now, deviceId = deviceId).toEntity()
            )
            opnameDao.upsertLines(
                lines.map { it.copy(syncStatus = SyncStatus.PENDING, updatedAt = now, deviceId = deviceId).toEntity() }
            )
        }
        sync.notifyPendingSync()
    }

    override suspend fun setCurrentQty(stockItemId: String, qty: Double) {
        stockDao.updateQty(stockItemId, qty, DateUtil.nowEpochMs())
        sync.notifyPendingSync()
    }

    override suspend fun deductQty(stockItemId: String, amount: Double) {
        stockDao.deductQty(stockItemId, amount, DateUtil.nowEpochMs())
        sync.notifyPendingSync()
    }
}
