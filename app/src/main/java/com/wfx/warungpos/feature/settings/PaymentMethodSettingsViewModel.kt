package com.wfx.warungpos.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wfx.warungpos.core.util.DateUtil
import com.wfx.warungpos.domain.model.PaymentMethod
import com.wfx.warungpos.domain.repository.PaymentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PaymentMethodSettingsViewModel @Inject constructor(
    private val paymentRepository: PaymentRepository,
) : ViewModel() {

    val methods: StateFlow<List<PaymentMethod>> = paymentRepository.observeAllPaymentMethods()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun toggleActive(method: PaymentMethod) {
        viewModelScope.launch {
            paymentRepository.savePaymentMethod(
                method.copy(isActive = !method.isActive, updatedAt = DateUtil.nowEpochMs())
            )
        }
    }
}
