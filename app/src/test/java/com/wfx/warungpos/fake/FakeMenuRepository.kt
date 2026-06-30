package com.wfx.warungpos.fake

import com.wfx.warungpos.domain.model.MenuCategory
import com.wfx.warungpos.domain.model.MenuItem
import com.wfx.warungpos.domain.model.VariantGroup
import com.wfx.warungpos.domain.model.VariantOption
import com.wfx.warungpos.domain.repository.MenuRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class FakeMenuRepository : MenuRepository {
    val categories = mutableMapOf<String, MenuCategory>()
    val items = mutableMapOf<String, MenuItem>()
    val variantGroups = mutableMapOf<String, VariantGroup>()
    val variantOptions = mutableMapOf<String, VariantOption>()

    override fun observeCategories(): Flow<List<MenuCategory>> = flowOf(categories.values.toList())

    override fun observeAllItems(): Flow<List<MenuItem>> = flowOf(items.values.toList())

    override fun observeAvailableItems(): Flow<List<MenuItem>> = flowOf(items.values.filter { it.isAvailable })

    override fun observeItemsByCategory(categoryId: String): Flow<List<MenuItem>> =
        flowOf(items.values.filter { it.categoryId == categoryId })

    override suspend fun getMenuItem(id: String): MenuItem? = items[id]

    override suspend fun saveCategory(category: MenuCategory) {
        categories[category.id] = category
    }

    override suspend fun saveMenuItem(item: MenuItem) {
        items[item.id] = item
    }

    override suspend fun deleteCategory(id: String) {
        categories.remove(id)
    }

    override suspend fun updateSoldOut(id: String, soldOut: Boolean) {
        items[id]?.let { items[id] = it.copy(isSoldOut = soldOut) }
    }

    override fun observeVariantGroups(menuItemId: String): Flow<List<VariantGroup>> =
        flowOf(variantGroups.values.filter { it.menuItemId == menuItemId })

    override fun observeVariantOptions(groupId: String): Flow<List<VariantOption>> =
        flowOf(variantOptions.values.filter { it.variantGroupId == groupId })

    override suspend fun saveVariantGroup(group: VariantGroup) {
        variantGroups[group.id] = group
    }

    override suspend fun saveVariantOption(option: VariantOption) {
        variantOptions[option.id] = option
    }

    override suspend fun deleteVariantGroup(id: String) {
        variantGroups.remove(id)
        variantOptions.values.filter { it.variantGroupId == id }.map { it.id }.forEach { variantOptions.remove(it) }
    }

    override suspend fun deleteVariantOption(id: String) {
        variantOptions.remove(id)
    }
}
