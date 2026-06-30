package com.wfx.warungpos.feature.shift

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wfx.warungpos.core.common.ShiftStatus
import com.wfx.warungpos.domain.model.Shift
import com.wfx.warungpos.domain.repository.ReportRepository
import com.wfx.warungpos.domain.repository.ShiftRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ShiftHistoryItem(val shift: Shift, val grossSales: Long)

data class ShiftHistoryUiState(
    val items: List<ShiftHistoryItem> = emptyList(),
    val isLoading: Boolean = true,
)

@HiltViewModel
class ShiftHistoryViewModel @Inject constructor(
    private val shiftRepository: ShiftRepository,
    private val reportRepository: ReportRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ShiftHistoryUiState())
    val uiState: StateFlow<ShiftHistoryUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch { load() }
    }

    private suspend fun load() {
        val shifts = shiftRepository.getRecentShifts(50)
            .filter { it.status == ShiftStatus.CLOSED }
            .sortedByDescending { it.closedAt ?: it.openedAt }
        val items = shifts.map { shift ->
            ShiftHistoryItem(shift, reportRepository.getTotalRevenueForShift(shift.id))
        }
        _uiState.update { it.copy(items = items, isLoading = false) }
    }
}
