package com.wfx.warungpos.feature.more

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wfx.warungpos.core.common.SessionManager
import com.wfx.warungpos.core.common.UserRole
import com.wfx.warungpos.domain.repository.ShiftRepository
import com.wfx.warungpos.domain.repository.StockRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class MoreUiState(
    val username: String = "",
    val userRole: UserRole = UserRole.OWNER,
    val lowStockCount: Int = 0,
    val openShiftCount: Int = 0,
)

@HiltViewModel
class MoreViewModel @Inject constructor(
    private val sessionManager: SessionManager,
    private val stockRepository: StockRepository,
    private val shiftRepository: ShiftRepository,
) : ViewModel() {

    val uiState: StateFlow<MoreUiState> = combine(
        sessionManager.username,
        sessionManager.userRole,
        stockRepository.observeLowStock().map { it.size },
        // DEFECT-016: a count > 1 means another device (or a past un-closed session) left a
        // shift open that Close Day would otherwise never surface. Badges the menu item the
        // same way low-stock already does, rather than requiring the owner to notice on their
        // own that revenue might be going untracked.
        shiftRepository.observeAllOpenShifts().map { it.size },
    ) { username, role, lowStockCount, openShiftCount ->
        MoreUiState(username = username, userRole = role, lowStockCount = lowStockCount, openShiftCount = openShiftCount)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MoreUiState())

    /** Re-locks the app back to the PIN screen. */
    fun lock() {
        sessionManager.lock()
    }
}
