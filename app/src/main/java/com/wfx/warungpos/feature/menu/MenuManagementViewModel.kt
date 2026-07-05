package com.wfx.warungpos.feature.menu

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wfx.warungpos.domain.model.MenuCategory
import com.wfx.warungpos.domain.model.MenuItem
import com.wfx.warungpos.domain.repository.BillRepository
import com.wfx.warungpos.domain.repository.MenuRepository
import com.wfx.warungpos.domain.repository.OrderRepository
import com.wfx.warungpos.domain.usecase.menu.HideMenuItemUseCase
import com.wfx.warungpos.domain.usecase.menu.ToggleSoldOutUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

const val UNCATEGORIZED_ID = "uncategorized"

data class MenuManagementUiState(
    val categories: List<MenuCategory> = emptyList(),
    val itemsByCategory: Map<String, List<MenuItem>> = emptyMap(),
    val itemPendingHide: MenuItem? = null,
    val itemInOpenBillWarning: Boolean = false,
)

@HiltViewModel
class MenuManagementViewModel @Inject constructor(
    private val menuRepository: MenuRepository,
    private val billRepository: BillRepository,
    private val orderRepository: OrderRepository,
    private val toggleSoldOutUseCase: ToggleSoldOutUseCase,
    private val hideMenuItemUseCase: HideMenuItemUseCase,
) : ViewModel() {

    private val _itemPendingHide = MutableStateFlow<MenuItem?>(null)
    private val _itemInOpenBillWarning = MutableStateFlow(false)

    val uiState: StateFlow<MenuManagementUiState> = combine(
        menuRepository.observeCategories(),
        menuRepository.observeAllItems(),
        _itemPendingHide,
        _itemInOpenBillWarning,
    ) { categories, items, pending, warning ->
        val knownCategoryIds = categories.map { it.id }.toSet()
        MenuManagementUiState(
            categories = categories,
            itemsByCategory = items.filter { it.isAvailable }.groupBy { item ->
                item.categoryId?.takeIf { it in knownCategoryIds } ?: UNCATEGORIZED_ID
            },
            itemPendingHide = pending,
            itemInOpenBillWarning = warning,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MenuManagementUiState())

    fun toggleSoldOut(item: MenuItem) {
        viewModelScope.launch {
            toggleSoldOutUseCase(item.id, !item.isSoldOut)
        }
    }

    fun requestHide(item: MenuItem) {
        viewModelScope.launch {
            val openBills = billRepository.getOpenBills()
            val inOpenBill = openBills.any { bill ->
                orderRepository.getActiveItems(bill.id).any { it.menuItemId == item.id }
            }
            _itemInOpenBillWarning.value = inOpenBill
            _itemPendingHide.value = item
        }
    }

    fun dismissHideRequest() {
        _itemPendingHide.value = null
        _itemInOpenBillWarning.value = false
    }

    fun confirmHide() {
        viewModelScope.launch {
            _itemPendingHide.value?.let { item -> hideMenuItemUseCase(item.id) }
            _itemPendingHide.value = null
            _itemInOpenBillWarning.value = false
        }
    }
}
