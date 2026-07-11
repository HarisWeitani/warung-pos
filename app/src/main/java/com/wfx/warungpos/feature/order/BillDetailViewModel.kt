package com.wfx.warungpos.feature.order

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.wfx.warungpos.core.common.OrderItemStatus
import com.wfx.warungpos.core.common.SessionManager
import com.wfx.warungpos.core.common.SyncStatus
import com.wfx.warungpos.core.common.UserRole
import com.wfx.warungpos.core.common.VoidReason
import com.wfx.warungpos.core.navigation.BillDetailRoute
import com.wfx.warungpos.core.util.DateUtil
import com.wfx.warungpos.core.util.UuidGenerator
import com.wfx.warungpos.domain.model.Bill
import com.wfx.warungpos.domain.model.MenuCategory
import com.wfx.warungpos.domain.model.MenuItem
import com.wfx.warungpos.domain.model.OrderItem
import com.wfx.warungpos.domain.model.VariantGroup
import com.wfx.warungpos.domain.model.VariantOption
import com.wfx.warungpos.domain.model.VariantSelection
import com.wfx.warungpos.domain.repository.BillRepository
import com.wfx.warungpos.domain.repository.MenuRepository
import com.wfx.warungpos.domain.repository.OrderRepository
import com.wfx.warungpos.domain.usecase.bill.VoidBillUseCase
import com.wfx.warungpos.domain.usecase.bill.VoidOrderItemUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject

data class VariantSheetState(
    val menuItem: MenuItem,
    val groups: List<VariantGroup>,
    val optionsByGroup: Map<String, List<VariantOption>>,
)

data class BillDetailUiState(
    val bill: Bill? = null,
    val orderItems: List<OrderItem> = emptyList(),
    val categories: List<MenuCategory> = emptyList(),
    val menuItems: List<MenuItem> = emptyList(),
    val selectedCategoryId: String? = null,
    val isBillPaid: Boolean = false,
    val isOwner: Boolean = false,
    val billVoided: Boolean = false,
    val voidError: String? = null,
)

@HiltViewModel
class BillDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val billRepository: BillRepository,
    private val orderRepository: OrderRepository,
    private val menuRepository: MenuRepository,
    private val sessionManager: SessionManager,
    private val voidOrderItemUseCase: VoidOrderItemUseCase,
    private val voidBillUseCase: VoidBillUseCase,
) : ViewModel() {

    private val billId: String = savedStateHandle.toRoute<BillDetailRoute>().billId

    private val _selectedCategoryId = MutableStateFlow<String?>(null)
    private val _billVoided = MutableStateFlow(false)
    private val _voidError = MutableStateFlow<String?>(null)
    private val _variantSheetState = MutableStateFlow<VariantSheetState?>(null)
    val variantSheetState: StateFlow<VariantSheetState?> = _variantSheetState.asStateFlow()

    // DEFECT-004: addItem() does a read-then-write (read the existing line's quantity, write
    // quantity+1) against Room. Each tap launches its own coroutine via viewModelScope.launch, so
    // rapid taps on the same menu item could interleave: two taps both read quantity=N before
    // either write lands, and both write back N+1 — one of the increments is silently lost. This
    // mutex serializes the whole read-modify-write section so concurrent taps queue up instead of
    // racing.
    private val addItemMutex = Mutex()

    val uiState: StateFlow<BillDetailUiState> = combine(
        billRepository.observeBillById(billId),
        orderRepository.observeOrderItems(billId),
        menuRepository.observeCategories(),
        menuRepository.observeAvailableItems(),
        _selectedCategoryId,
        sessionManager.userRole,
        _billVoided,
        _voidError,
    ) { args ->
        @Suppress("UNCHECKED_CAST")
        val bill = args[0] as Bill?
        val items = args[1] as List<OrderItem>
        val categories = args[2] as List<MenuCategory>
        val allItems = args[3] as List<MenuItem>
        val selectedCat = args[4] as String?
        val role = args[5] as UserRole
        val billVoided = args[6] as Boolean
        val voidError = args[7] as String?
        val cat = selectedCat ?: categories.firstOrNull()?.id
        val filtered = if (cat != null) allItems.filter { it.categoryId == cat } else allItems
        BillDetailUiState(
            bill = bill,
            orderItems = items,
            categories = categories,
            menuItems = filtered,
            selectedCategoryId = cat,
            isBillPaid = bill?.status?.name == "PAID",
            isOwner = role == UserRole.OWNER,
            billVoided = billVoided,
            voidError = voidError,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BillDetailUiState())

    fun selectCategory(categoryId: String) {
        _selectedCategoryId.update { categoryId }
    }

    fun onMenuItemTapped(menuItem: MenuItem) {
        viewModelScope.launch {
            val groups = menuRepository.observeVariantGroups(menuItem.id).first()
            if (groups.isEmpty()) {
                addItem(menuItem, emptyList())
            } else {
                val optionsByGroup = groups.associate { g -> g.id to menuRepository.observeVariantOptions(g.id).first() }
                _variantSheetState.value = VariantSheetState(menuItem, groups, optionsByGroup)
            }
        }
    }

    fun dismissVariantSheet() {
        _variantSheetState.value = null
    }

    fun confirmVariantSelection(selections: List<VariantSelection>) {
        val sheetState = _variantSheetState.value ?: return
        viewModelScope.launch {
            addItem(sheetState.menuItem, selections)
            _variantSheetState.value = null
        }
    }

    private suspend fun addItem(menuItem: MenuItem, variants: List<VariantSelection>) = addItemMutex.withLock {
        val bill = billRepository.getBill(billId) ?: return@withLock
        val now = DateUtil.nowEpochMs()
        val pricePerUnit = menuItem.basePrice + variants.sumOf { it.priceDelta }
        val existing = if (variants.isEmpty()) {
            orderRepository.getActiveItems(billId)
                .firstOrNull { it.menuItemId == menuItem.id && it.selectedVariants.isEmpty() }
        } else null

        if (existing != null) {
            val newQty = existing.quantity + 1
            orderRepository.saveItem(
                existing.copy(
                    quantity = newQty,
                    lineTotal = existing.priceSnapshot * newQty,
                    updatedAt = now,
                )
            )
        } else {
            orderRepository.saveItem(
                OrderItem(
                    id = UuidGenerator.generate(),
                    billId = billId,
                    menuItemId = menuItem.id,
                    nameSnapshot = menuItem.name,
                    priceSnapshot = pricePerUnit,
                    quantity = 1,
                    selectedVariants = variants,
                    lineTotal = pricePerUnit,
                    status = OrderItemStatus.ORDERED,
                    voidReason = null,
                    voidNote = null,
                    voidedBy = null,
                    createdAt = now,
                    updatedAt = now,
                    syncStatus = SyncStatus.PENDING,
                    deviceId = sessionManager.deviceId,
                )
            )
        }
        recalculateBillTotals(bill)
    }

    fun voidItem(itemId: String, reason: VoidReason, note: String?) {
        viewModelScope.launch {
            voidOrderItemUseCase(itemId, reason, note)
                .onFailure { e -> _voidError.update { e.message } }
        }
    }

    fun voidBill() {
        viewModelScope.launch {
            voidBillUseCase(billId)
                .onSuccess { _billVoided.update { true } }
                .onFailure { e -> _voidError.update { e.message } }
        }
    }

    fun dismissVoidError() {
        _voidError.update { null }
    }

    private suspend fun recalculateBillTotals(bill: Bill) {
        val activeItems = orderRepository.getActiveItems(billId)
        val subtotal = activeItems.sumOf { it.lineTotal }
        billRepository.saveBill(
            bill.copy(subtotal = subtotal, discountTotal = 0L, grandTotal = subtotal)
        )
    }
}
