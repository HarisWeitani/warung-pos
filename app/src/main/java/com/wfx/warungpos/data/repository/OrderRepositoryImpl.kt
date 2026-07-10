package com.wfx.warungpos.data.repository

import com.wfx.warungpos.core.common.SessionManager
import com.wfx.warungpos.core.common.SyncStatus
import com.wfx.warungpos.core.common.VoidReason
import com.wfx.warungpos.core.util.DateUtil
import com.wfx.warungpos.data.local.dao.OrderItemDao
import com.wfx.warungpos.data.local.mapper.toDomain
import com.wfx.warungpos.data.local.mapper.toEntity
import com.wfx.warungpos.data.remote.sync.SyncCoordinator
import com.wfx.warungpos.domain.model.OrderItem
import com.wfx.warungpos.domain.repository.OrderRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OrderRepositoryImpl @Inject constructor(
    private val orderItemDao: OrderItemDao,
    private val sessionManager: SessionManager,
    private val sync: SyncCoordinator,
) : OrderRepository {

    override fun observeOrderItems(billId: String): Flow<List<OrderItem>> =
        orderItemDao.observeForBill(billId).map { it.map { e -> e.toDomain() } }

    override suspend fun getActiveItems(billId: String): List<OrderItem> =
        orderItemDao.getActiveForBill(billId).map { it.toDomain() }

    override suspend fun getItemById(id: String): OrderItem? =
        orderItemDao.getById(id)?.toDomain()

    override suspend fun saveItem(item: OrderItem) {
        orderItemDao.upsert(
            item.copy(
                syncStatus = SyncStatus.PENDING,
                updatedAt = DateUtil.nowEpochMs(),
                deviceId = sessionManager.deviceId,
            ).toEntity()
        )
        sync.notifyPendingSync()
    }

    override suspend fun voidItem(id: String, reason: VoidReason, voidedBy: String) {
        orderItemDao.voidItem(
            id = id,
            reason = reason.name,
            voidedBy = voidedBy,
            updatedAt = DateUtil.nowEpochMs(),
        )
        sync.notifyPendingSync()
    }

    override fun observeQueue(): Flow<List<OrderItem>> =
        orderItemDao.observeQueue().map { it.map { e -> e.toDomain() } }

    override suspend fun markItemDone(id: String) {
        orderItemDao.markDone(id, DateUtil.nowEpochMs())
        sync.notifyPendingSync()
    }
}
