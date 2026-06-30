package com.wfx.warungpos.domain.usecase.expense

import com.wfx.warungpos.core.common.ExpenseCategory
import com.wfx.warungpos.core.common.SessionProvider
import com.wfx.warungpos.core.common.SyncStatus
import com.wfx.warungpos.core.util.DateUtil
import com.wfx.warungpos.core.util.UuidGenerator
import com.wfx.warungpos.domain.exception.ShiftNotOpenException
import com.wfx.warungpos.domain.model.Expense
import com.wfx.warungpos.domain.repository.ExpenseRepository
import com.wfx.warungpos.domain.repository.ShiftRepository
import javax.inject.Inject

class SaveExpenseUseCase @Inject constructor(
    private val expenseRepository: ExpenseRepository,
    private val shiftRepository: ShiftRepository,
    private val sessionProvider: SessionProvider,
) {
    suspend operator fun invoke(category: ExpenseCategory, amount: Long, note: String?): Result<Unit> {
        if (amount <= 0) return Result.failure(IllegalArgumentException("Amount must be greater than 0"))
        val shift = shiftRepository.getOpenShift() ?: return Result.failure(ShiftNotOpenException())
        val now = DateUtil.nowEpochMs()
        expenseRepository.saveExpense(
            Expense(
                id = UuidGenerator.generate(),
                shiftId = shift.id,
                category = category,
                amount = amount,
                note = note?.ifBlank { null },
                createdBy = sessionProvider.currentUserId ?: "",
                createdAt = now,
                updatedAt = now,
                syncStatus = SyncStatus.PENDING,
                deviceId = sessionProvider.deviceId,
            )
        )
        return Result.success(Unit)
    }
}
