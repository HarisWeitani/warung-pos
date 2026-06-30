package com.wfx.warungpos.data.repository

import com.wfx.warungpos.core.common.SessionManager
import com.wfx.warungpos.core.common.SyncStatus
import com.wfx.warungpos.core.util.DateUtil
import com.wfx.warungpos.data.local.dao.MenuCategoryDao
import com.wfx.warungpos.data.local.dao.MenuItemDao
import com.wfx.warungpos.data.local.dao.VariantDao
import com.wfx.warungpos.data.local.mapper.toDomain
import com.wfx.warungpos.data.local.mapper.toEntity
import com.wfx.warungpos.data.remote.sync.SyncCoordinator
import com.wfx.warungpos.domain.model.MenuCategory
import com.wfx.warungpos.domain.model.MenuItem
import com.wfx.warungpos.domain.model.VariantGroup
import com.wfx.warungpos.domain.model.VariantOption
import com.wfx.warungpos.domain.repository.MenuRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MenuRepositoryImpl @Inject constructor(
    private val categoryDao: MenuCategoryDao,
    private val itemDao: MenuItemDao,
    private val variantDao: VariantDao,
    private val sessionManager: SessionManager,
    private val sync: SyncCoordinator,
) : MenuRepository {

    override fun observeCategories(): Flow<List<MenuCategory>> =
        categoryDao.observeAll().map { it.map { e -> e.toDomain() } }

    override fun observeAllItems(): Flow<List<MenuItem>> =
        itemDao.observeAll().map { it.map { e -> e.toDomain() } }

    override fun observeAvailableItems(): Flow<List<MenuItem>> =
        itemDao.observeAvailable().map { it.map { e -> e.toDomain() } }

    override fun observeItemsByCategory(categoryId: String): Flow<List<MenuItem>> =
        itemDao.observeByCategory(categoryId).map { it.map { e -> e.toDomain() } }

    override suspend fun getMenuItem(id: String): MenuItem? =
        itemDao.getById(id)?.toDomain()

    override suspend fun saveCategory(category: MenuCategory) {
        categoryDao.upsert(
            category.copy(
                syncStatus = SyncStatus.PENDING,
                updatedAt = DateUtil.nowEpochMs(),
                deviceId = sessionManager.deviceId,
            ).toEntity()
        )
        sync.notifyPendingSync()
    }

    override suspend fun saveMenuItem(item: MenuItem) {
        itemDao.upsert(
            item.copy(
                syncStatus = SyncStatus.PENDING,
                updatedAt = DateUtil.nowEpochMs(),
                deviceId = sessionManager.deviceId,
            ).toEntity()
        )
        sync.notifyPendingSync()
    }

    override suspend fun deleteCategory(id: String) = categoryDao.deleteById(id)

    override suspend fun updateSoldOut(id: String, soldOut: Boolean) {
        itemDao.updateSoldOut(id, soldOut, DateUtil.nowEpochMs())
        sync.notifyPendingSync()
    }

    override fun observeVariantGroups(menuItemId: String): Flow<List<VariantGroup>> =
        variantDao.observeGroupsForItem(menuItemId).map { it.map { e -> e.toDomain() } }

    override fun observeVariantOptions(groupId: String): Flow<List<VariantOption>> =
        variantDao.observeOptionsForGroup(groupId).map { it.map { e -> e.toDomain() } }

    override suspend fun saveVariantGroup(group: VariantGroup) {
        variantDao.upsertGroup(
            group.copy(
                syncStatus = SyncStatus.PENDING,
                updatedAt = DateUtil.nowEpochMs(),
                deviceId = sessionManager.deviceId,
            ).toEntity()
        )
        sync.notifyPendingSync()
    }

    override suspend fun saveVariantOption(option: VariantOption) {
        variantDao.upsertOption(
            option.copy(
                syncStatus = SyncStatus.PENDING,
                updatedAt = DateUtil.nowEpochMs(),
                deviceId = sessionManager.deviceId,
            ).toEntity()
        )
        sync.notifyPendingSync()
    }

    override suspend fun deleteVariantGroup(id: String) = variantDao.deleteGroup(id)

    override suspend fun deleteVariantOption(id: String) = variantDao.deleteOption(id)
}
