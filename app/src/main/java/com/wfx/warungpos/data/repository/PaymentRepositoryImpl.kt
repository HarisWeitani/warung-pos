package com.wfx.warungpos.data.repository

import com.wfx.warungpos.core.common.SessionManager
import com.wfx.warungpos.core.common.SyncStatus
import com.wfx.warungpos.core.util.DateUtil
import com.wfx.warungpos.data.local.dao.PaymentDao
import com.wfx.warungpos.data.local.dao.PaymentMethodDao
import com.wfx.warungpos.data.local.mapper.toDomain
import com.wfx.warungpos.data.local.mapper.toEntity
import com.wfx.warungpos.data.remote.sync.SyncCoordinator
import com.wfx.warungpos.domain.model.Payment
import com.wfx.warungpos.domain.model.PaymentBreakdown
import com.wfx.warungpos.domain.model.PaymentMethod
import com.wfx.warungpos.domain.repository.PaymentRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PaymentRepositoryImpl @Inject constructor(
    private val paymentDao: PaymentDao,
    private val paymentMethodDao: PaymentMethodDao,
    private val sessionManager: SessionManager,
    private val sync: SyncCoordinator,
) : PaymentRepository {

    override fun observePaymentsForBill(billId: String): Flow<List<Payment>> =
        paymentDao.observeForBill(billId).map { it.map { e -> e.toDomain() } }

    override fun observeActivePaymentMethods(): Flow<List<PaymentMethod>> =
        paymentMethodDao.observeActive().map { it.map { e -> e.toDomain() } }

    override fun observeAllPaymentMethods(): Flow<List<PaymentMethod>> =
        paymentMethodDao.observeAll().map { it.map { e -> e.toDomain() } }

    override suspend fun recordPayment(payment: Payment) {
        paymentDao.upsert(
            payment.copy(
                syncStatus = SyncStatus.PENDING,
                updatedAt = DateUtil.nowEpochMs(),
                deviceId = sessionManager.deviceId,
            ).toEntity()
        )
        sync.notifyPendingSync()
    }

    override suspend fun savePaymentMethod(method: PaymentMethod) {
        paymentMethodDao.upsert(
            method.copy(
                syncStatus = SyncStatus.PENDING,
                updatedAt = DateUtil.nowEpochMs(),
                deviceId = sessionManager.deviceId,
            ).toEntity()
        )
        sync.notifyPendingSync()
    }

    override suspend fun getPaymentBreakdownForShift(shiftId: String): List<PaymentBreakdown> =
        paymentDao.sumByMethodForShift(shiftId)
            .map { PaymentBreakdown(paymentMethodId = it.paymentMethodId, total = it.total) }

    override suspend fun getPaymentBreakdownInRange(startEpoch: Long, endEpoch: Long): List<PaymentBreakdown> =
        paymentDao.sumByMethodInRange(startEpoch, endEpoch)
            .map { PaymentBreakdown(paymentMethodId = it.paymentMethodId, total = it.total) }
}
