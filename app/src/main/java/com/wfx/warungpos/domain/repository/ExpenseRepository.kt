package com.wfx.warungpos.domain.repository

import com.wfx.warungpos.domain.model.Expense
import kotlinx.coroutines.flow.Flow

interface ExpenseRepository {
    fun observeAllExpenses(): Flow<List<Expense>>
    fun observeExpensesForShift(shiftId: String): Flow<List<Expense>>
    suspend fun saveExpense(expense: Expense)
    suspend fun totalForShift(shiftId: String): Long
    suspend fun getExpensesInRange(startEpoch: Long, endEpoch: Long): List<Expense>
}
