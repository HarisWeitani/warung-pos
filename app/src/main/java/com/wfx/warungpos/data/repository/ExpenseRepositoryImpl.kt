package com.wfx.warungpos.data.repository

import com.wfx.warungpos.core.common.SessionManager
import com.wfx.warungpos.core.common.SyncStatus
import com.wfx.warungpos.core.util.DateUtil
import com.wfx.warungpos.data.local.dao.ExpenseDao
import com.wfx.warungpos.data.local.mapper.toDomain
import com.wfx.warungpos.data.local.mapper.toEntity
import com.wfx.warungpos.data.remote.sync.SyncCoordinator
import com.wfx.warungpos.domain.model.Expense
import com.wfx.warungpos.domain.repository.ExpenseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExpenseRepositoryImpl @Inject constructor(
    private val expenseDao: ExpenseDao,
    private val sessionManager: SessionManager,
    private val sync: SyncCoordinator,
) : ExpenseRepository {

    override fun observeAllExpenses(): Flow<List<Expense>> =
        expenseDao.observeAll().map { it.map { e -> e.toDomain() } }

    override fun observeExpensesForShift(shiftId: String): Flow<List<Expense>> =
        expenseDao.observeForShift(shiftId).map { it.map { e -> e.toDomain() } }

    override suspend fun saveExpense(expense: Expense) {
        expenseDao.upsert(
            expense.copy(
                syncStatus = SyncStatus.PENDING,
                updatedAt = DateUtil.nowEpochMs(),
                deviceId = sessionManager.deviceId,
            ).toEntity()
        )
        sync.notifyPendingSync()
    }

    override suspend fun totalForShift(shiftId: String): Long =
        expenseDao.totalForShift(shiftId)

    override suspend fun getExpensesInRange(startEpoch: Long, endEpoch: Long): List<Expense> =
        expenseDao.getInRange(startEpoch, endEpoch).map { it.toDomain() }
}
