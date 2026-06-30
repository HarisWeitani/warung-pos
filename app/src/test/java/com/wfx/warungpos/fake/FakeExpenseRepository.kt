package com.wfx.warungpos.fake

import com.wfx.warungpos.domain.model.Expense
import com.wfx.warungpos.domain.repository.ExpenseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class FakeExpenseRepository : ExpenseRepository {
    val expenses = mutableMapOf<String, Expense>()
    var totalForShiftValue: Long = 0L

    override fun observeAllExpenses(): Flow<List<Expense>> = flowOf(expenses.values.toList())

    override fun observeExpensesForShift(shiftId: String): Flow<List<Expense>> =
        flowOf(expenses.values.filter { it.shiftId == shiftId })

    override suspend fun saveExpense(expense: Expense) {
        expenses[expense.id] = expense
    }

    override suspend fun totalForShift(shiftId: String): Long = totalForShiftValue

    override suspend fun getExpensesInRange(startEpoch: Long, endEpoch: Long): List<Expense> =
        expenses.values.filter { it.createdAt in startEpoch..endEpoch }
}
