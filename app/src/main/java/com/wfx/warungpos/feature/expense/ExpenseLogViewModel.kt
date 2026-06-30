package com.wfx.warungpos.feature.expense

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wfx.warungpos.core.common.ExpenseCategory
import com.wfx.warungpos.core.common.SessionManager
import com.wfx.warungpos.core.common.SyncStatus
import com.wfx.warungpos.core.util.DateUtil
import com.wfx.warungpos.core.util.UuidGenerator
import com.wfx.warungpos.domain.model.Expense
import com.wfx.warungpos.domain.model.Shift
import com.wfx.warungpos.domain.repository.ExpenseRepository
import com.wfx.warungpos.domain.repository.ShiftRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ExpenseLogUiState(
    val expenses: List<Expense> = emptyList(),
    val openShift: Shift? = null,
    val isAddSheetOpen: Boolean = false,
    val newCategory: ExpenseCategory = ExpenseCategory.SUPPLIES,
    val newAmount: String = "",
    val newNote: String = "",
    val isSaving: Boolean = false,
)

@HiltViewModel
class ExpenseLogViewModel @Inject constructor(
    private val expenseRepository: ExpenseRepository,
    private val shiftRepository: ShiftRepository,
    private val sessionManager: SessionManager,
) : ViewModel() {

    private val _shift: StateFlow<Shift?> = shiftRepository.observeOpenShift()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _expenses: StateFlow<List<Expense>> = _shift
        .flatMapLatest { shift ->
            if (shift != null) expenseRepository.observeExpensesForShift(shift.id)
            else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _formState = MutableStateFlow(
        Triple(false, ExpenseCategory.SUPPLIES, "")
    )

    val uiState: StateFlow<ExpenseLogUiState> = combine(
        _shift, _expenses, _formState,
    ) { shift, expenses, (isOpen, cat, amount) ->
        ExpenseLogUiState(
            expenses = expenses,
            openShift = shift,
            isAddSheetOpen = isOpen,
            newCategory = cat,
            newAmount = amount,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ExpenseLogUiState())

    private val _noteState = MutableStateFlow("")
    val note: StateFlow<String> = _noteState.asStateFlow()

    fun showAddSheet() { _formState.update { (_, cat, amt) -> Triple(true, cat, amt) } }
    fun dismissSheet() {
        _formState.update { Triple(false, ExpenseCategory.SUPPLIES, "") }
        _noteState.value = ""
    }

    fun onCategoryChange(cat: ExpenseCategory) {
        _formState.update { (isOpen, _, amt) -> Triple(isOpen, cat, amt) }
    }

    fun onAmountChange(value: String) {
        _formState.update { (isOpen, cat, _) -> Triple(isOpen, cat, value.filter { it.isDigit() }) }
    }

    fun onNoteChange(value: String) { _noteState.value = value }

    fun saveExpense() {
        viewModelScope.launch {
            val (_, cat, amount) = _formState.value
            val amountLong = amount.toLongOrNull() ?: return@launch
            if (amountLong <= 0L) return@launch
            val now = DateUtil.nowEpochMs()
            expenseRepository.saveExpense(
                Expense(
                    id = UuidGenerator.generate(),
                    shiftId = _shift.value?.id,
                    category = cat,
                    amount = amountLong,
                    note = _noteState.value.ifBlank { null },
                    createdBy = sessionManager.currentUser.value?.uid ?: "",
                    createdAt = now,
                    updatedAt = now,
                    syncStatus = SyncStatus.PENDING,
                    deviceId = sessionManager.deviceId,
                )
            )
            dismissSheet()
        }
    }

}
