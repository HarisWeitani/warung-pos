package com.wfx.warungpos.feature.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wfx.warungpos.domain.model.BestSeller
import com.wfx.warungpos.domain.model.PaymentBreakdown
import com.wfx.warungpos.domain.usecase.report.GetDashboardDataUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DashboardUiState(
    val totalRevenue: Long = 0L,
    val transactionCount: Int = 0,
    val totalExpenses: Long = 0L,
    val paymentBreakdown: List<PaymentBreakdown> = emptyList(),
    val bestSellers: List<BestSeller> = emptyList(),
    val isLoading: Boolean = true,
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val getDashboardDataUseCase: GetDashboardDataUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            getDashboardDataUseCase().onSuccess { data ->
                _uiState.update {
                    it.copy(
                        totalRevenue = data.totalRevenue,
                        transactionCount = data.transactionCount,
                        totalExpenses = data.totalExpenses,
                        paymentBreakdown = data.paymentBreakdown,
                        bestSellers = data.bestSellers,
                        isLoading = false,
                    )
                }
            }
        }
    }
}
