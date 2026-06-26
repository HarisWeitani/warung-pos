package com.wfx.warungpos.data.local.mapper

import com.wfx.warungpos.core.common.SyncStatus
import com.wfx.warungpos.core.common.VariantSelectionType
import com.wfx.warungpos.data.local.entity.MenuCategoryEntity
import com.wfx.warungpos.data.local.entity.MenuItemEntity
import com.wfx.warungpos.data.local.entity.VariantGroupEntity
import com.wfx.warungpos.data.local.entity.VariantOptionEntity
import com.wfx.warungpos.domain.model.MenuCategory
import com.wfx.warungpos.domain.model.MenuItem
import com.wfx.warungpos.domain.model.VariantGroup
import com.wfx.warungpos.domain.model.VariantOption

fun MenuCategoryEntity.toDomain() = MenuCategory(
    id = id,
    name = name,
    sortOrder = sortOrder,
    updatedAt = updatedAt,
    syncStatus = SyncStatus.valueOf(syncStatus),
    deviceId = deviceId,
)

fun MenuCategory.toEntity() = MenuCategoryEntity(
    id = id,
    name = name,
    sortOrder = sortOrder,
    updatedAt = updatedAt,
    syncStatus = syncStatus.name,
    deviceId = deviceId,
)

fun MenuItemEntity.toDomain() = MenuItem(
    id = id,
    categoryId = categoryId,
    name = name,
    basePrice = basePrice,
    isAvailable = isAvailable,
    isSoldOut = isSoldOut,
    updatedAt = updatedAt,
    syncStatus = SyncStatus.valueOf(syncStatus),
    deviceId = deviceId,
)

fun MenuItem.toEntity() = MenuItemEntity(
    id = id,
    categoryId = categoryId,
    name = name,
    basePrice = basePrice,
    isAvailable = isAvailable,
    isSoldOut = isSoldOut,
    updatedAt = updatedAt,
    syncStatus = syncStatus.name,
    deviceId = deviceId,
)

fun VariantGroupEntity.toDomain() = VariantGroup(
    id = id,
    menuItemId = menuItemId,
    name = name,
    selectionType = VariantSelectionType.valueOf(selectionType),
    isRequired = isRequired,
    updatedAt = updatedAt,
    syncStatus = SyncStatus.valueOf(syncStatus),
    deviceId = deviceId,
)

fun VariantGroup.toEntity() = VariantGroupEntity(
    id = id,
    menuItemId = menuItemId,
    name = name,
    selectionType = selectionType.name,
    isRequired = isRequired,
    updatedAt = updatedAt,
    syncStatus = syncStatus.name,
    deviceId = deviceId,
)

fun VariantOptionEntity.toDomain() = VariantOption(
    id = id,
    variantGroupId = variantGroupId,
    name = name,
    priceDelta = priceDelta,
    updatedAt = updatedAt,
    syncStatus = SyncStatus.valueOf(syncStatus),
    deviceId = deviceId,
)

fun VariantOption.toEntity() = VariantOptionEntity(
    id = id,
    variantGroupId = variantGroupId,
    name = name,
    priceDelta = priceDelta,
    updatedAt = updatedAt,
    syncStatus = syncStatus.name,
    deviceId = deviceId,
)
