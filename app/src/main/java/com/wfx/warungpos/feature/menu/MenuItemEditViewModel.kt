package com.wfx.warungpos.feature.menu

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.wfx.warungpos.core.common.SessionManager
import com.wfx.warungpos.core.common.SyncStatus
import com.wfx.warungpos.core.common.VariantSelectionType
import com.wfx.warungpos.core.navigation.MenuItemEditRoute
import com.wfx.warungpos.core.util.DateUtil
import com.wfx.warungpos.core.util.UuidGenerator
import com.wfx.warungpos.domain.model.MenuCategory
import com.wfx.warungpos.domain.model.MenuItem
import com.wfx.warungpos.domain.model.MenuItemIngredient
import com.wfx.warungpos.domain.model.StockItem
import com.wfx.warungpos.domain.model.VariantGroup
import com.wfx.warungpos.domain.model.VariantOption
import com.wfx.warungpos.domain.repository.MenuRepository
import com.wfx.warungpos.domain.repository.StockRepository
import com.wfx.warungpos.domain.usecase.menu.UpsertMenuItemUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class VariantGroupUi(
    val group: VariantGroup,
    val options: List<VariantOption>,
)

data class MenuItemEditUiState(
    val itemId: String? = null,
    val name: String = "",
    val categoryId: String? = null,
    val price: String = "",
    val isAvailable: Boolean = true,
    val isSoldOut: Boolean = false,
    val categories: List<MenuCategory> = emptyList(),
    val variantGroups: List<VariantGroupUi> = emptyList(),
    val stockItems: List<StockItem> = emptyList(),
    val ingredients: List<MenuItemIngredient> = emptyList(),
    val isSaving: Boolean = false,
    val error: String? = null,
    val isSaved: Boolean = false,
)

@HiltViewModel
class MenuItemEditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val menuRepository: MenuRepository,
    private val stockRepository: StockRepository,
    private val upsertMenuItemUseCase: UpsertMenuItemUseCase,
    private val sessionManager: SessionManager,
) : ViewModel() {

    private val initialItemId: String? = savedStateHandle.toRoute<MenuItemEditRoute>().itemId

    private val _uiState = MutableStateFlow(MenuItemEditUiState(itemId = initialItemId))
    val uiState: StateFlow<MenuItemEditUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val categories = menuRepository.observeCategories().first()
            val stockItems = stockRepository.observeAllItems().first()
            _uiState.update { it.copy(categories = categories, stockItems = stockItems) }
            if (initialItemId != null) {
                menuRepository.getMenuItem(initialItemId)?.let { item ->
                    _uiState.update {
                        it.copy(
                            name = item.name,
                            categoryId = item.categoryId,
                            price = item.basePrice.toString(),
                            isAvailable = item.isAvailable,
                            isSoldOut = item.isSoldOut,
                        )
                    }
                }
                loadVariantGroups()
                loadIngredients()
            }
        }
    }

    private suspend fun loadVariantGroups() {
        val id = _uiState.value.itemId ?: return
        val groups = menuRepository.observeVariantGroups(id).first()
        val groupUis = groups.map { g -> VariantGroupUi(g, menuRepository.observeVariantOptions(g.id).first()) }
        _uiState.update { it.copy(variantGroups = groupUis) }
    }

    private suspend fun loadIngredients() {
        val id = _uiState.value.itemId ?: return
        val ingredients = stockRepository.getIngredientsForMenuItem(id)
        _uiState.update { it.copy(ingredients = ingredients) }
    }

    fun addIngredient() {
        viewModelScope.launch {
            val state = _uiState.value
            val itemId = state.itemId ?: return@launch
            val stockItem = state.stockItems.firstOrNull { candidate ->
                state.ingredients.none { it.stockItemId == candidate.id }
            } ?: return@launch
            stockRepository.saveIngredient(
                MenuItemIngredient(
                    menuItemId = itemId,
                    stockItemId = stockItem.id,
                    qtyPerServing = 0.0,
                    updatedAt = DateUtil.nowEpochMs(),
                    syncStatus = SyncStatus.PENDING,
                    deviceId = sessionManager.deviceId,
                )
            )
            loadIngredients()
        }
    }

    fun updateIngredient(oldStockItemId: String, updated: MenuItemIngredient) {
        viewModelScope.launch {
            val itemId = _uiState.value.itemId ?: return@launch
            if (updated.stockItemId != oldStockItemId) {
                stockRepository.deleteIngredient(itemId, oldStockItemId)
            }
            stockRepository.saveIngredient(updated.copy(updatedAt = DateUtil.nowEpochMs()))
            loadIngredients()
        }
    }

    fun deleteIngredient(stockItemId: String) {
        viewModelScope.launch {
            val itemId = _uiState.value.itemId ?: return@launch
            stockRepository.deleteIngredient(itemId, stockItemId)
            loadIngredients()
        }
    }

    fun onNameChange(value: String) = _uiState.update { it.copy(name = value, error = null) }
    fun onCategoryChange(id: String?) = _uiState.update { it.copy(categoryId = id, error = null) }
    fun onPriceChange(value: String) = _uiState.update { it.copy(price = value.filter { c -> c.isDigit() }, error = null) }

    fun addVariantGroup() {
        viewModelScope.launch {
            val itemId = _uiState.value.itemId ?: return@launch
            val now = DateUtil.nowEpochMs()
            menuRepository.saveVariantGroup(
                VariantGroup(
                    id = UuidGenerator.generate(),
                    menuItemId = itemId,
                    name = "New Group",
                    selectionType = VariantSelectionType.SINGLE,
                    isRequired = false,
                    updatedAt = now,
                    syncStatus = SyncStatus.PENDING,
                    deviceId = sessionManager.deviceId,
                )
            )
            loadVariantGroups()
        }
    }

    fun updateGroup(group: VariantGroup) {
        viewModelScope.launch {
            menuRepository.saveVariantGroup(group.copy(updatedAt = DateUtil.nowEpochMs()))
            loadVariantGroups()
        }
    }

    fun deleteGroup(groupId: String) {
        viewModelScope.launch {
            menuRepository.deleteVariantGroup(groupId)
            loadVariantGroups()
        }
    }

    fun addOption(groupId: String) {
        viewModelScope.launch {
            val now = DateUtil.nowEpochMs()
            menuRepository.saveVariantOption(
                VariantOption(
                    id = UuidGenerator.generate(),
                    variantGroupId = groupId,
                    name = "New Option",
                    priceDelta = 0L,
                    updatedAt = now,
                    syncStatus = SyncStatus.PENDING,
                    deviceId = sessionManager.deviceId,
                )
            )
            loadVariantGroups()
        }
    }

    fun updateOption(option: VariantOption) {
        viewModelScope.launch {
            menuRepository.saveVariantOption(option.copy(updatedAt = DateUtil.nowEpochMs()))
            loadVariantGroups()
        }
    }

    fun deleteOption(optionId: String) {
        viewModelScope.launch {
            menuRepository.deleteVariantOption(optionId)
            loadVariantGroups()
        }
    }

    fun save() {
        viewModelScope.launch {
            val state = _uiState.value
            _uiState.update { it.copy(isSaving = true, error = null) }
            val now = DateUtil.nowEpochMs()
            val item = MenuItem(
                id = state.itemId ?: UuidGenerator.generate(),
                categoryId = state.categoryId,
                name = state.name.trim(),
                basePrice = state.price.toLongOrNull() ?: 0L,
                isAvailable = state.isAvailable,
                isSoldOut = state.isSoldOut,
                updatedAt = now,
                syncStatus = SyncStatus.PENDING,
                deviceId = sessionManager.deviceId,
            )
            upsertMenuItemUseCase(item)
                .onSuccess { _uiState.update { it.copy(isSaving = false, isSaved = true, itemId = item.id) } }
                .onFailure { e -> _uiState.update { it.copy(isSaving = false, error = e.message) } }
        }
    }
}
