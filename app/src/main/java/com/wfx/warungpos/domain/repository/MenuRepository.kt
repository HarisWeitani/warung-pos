package com.wfx.warungpos.domain.repository

import com.wfx.warungpos.domain.model.MenuCategory
import com.wfx.warungpos.domain.model.MenuItem
import com.wfx.warungpos.domain.model.VariantGroup
import com.wfx.warungpos.domain.model.VariantOption
import kotlinx.coroutines.flow.Flow

interface MenuRepository {
    fun observeCategories(): Flow<List<MenuCategory>>
    fun observeAllItems(): Flow<List<MenuItem>>
    fun observeAvailableItems(): Flow<List<MenuItem>>
    fun observeItemsByCategory(categoryId: String): Flow<List<MenuItem>>
    suspend fun getMenuItem(id: String): MenuItem?
    suspend fun saveCategory(category: MenuCategory)
    suspend fun saveMenuItem(item: MenuItem)
    suspend fun deleteCategory(id: String)
    suspend fun updateSoldOut(id: String, soldOut: Boolean)
    fun observeVariantGroups(menuItemId: String): Flow<List<VariantGroup>>
    fun observeVariantOptions(groupId: String): Flow<List<VariantOption>>
    suspend fun saveVariantGroup(group: VariantGroup)
    suspend fun saveVariantOption(option: VariantOption)
    suspend fun deleteVariantGroup(id: String)
    suspend fun deleteVariantOption(id: String)
}
