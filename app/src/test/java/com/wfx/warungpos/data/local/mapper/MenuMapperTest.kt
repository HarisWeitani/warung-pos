package com.wfx.warungpos.data.local.mapper

import com.wfx.warungpos.core.common.SyncStatus
import com.wfx.warungpos.core.common.VariantSelectionType
import com.wfx.warungpos.data.local.entity.MenuCategoryEntity
import com.wfx.warungpos.data.local.entity.MenuItemEntity
import com.wfx.warungpos.data.local.entity.VariantGroupEntity
import com.wfx.warungpos.data.local.entity.VariantOptionEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class MenuMapperTest {

    @Test
    fun `MenuCategoryEntity round-trips through domain`() {
        val entity = MenuCategoryEntity(
            id = "cat-1", name = "Makanan", sortOrder = 1,
            updatedAt = 1_000L, syncStatus = "PENDING", deviceId = "dev-1",
        )
        assertEquals(entity, entity.toDomain().toEntity())
    }

    @Test
    fun `MenuItemEntity round-trips through domain`() {
        val entity = MenuItemEntity(
            id = "item-1", categoryId = "cat-1", name = "Nasi Goreng", basePrice = 15_000L,
            isAvailable = true, isSoldOut = false,
            updatedAt = 1_000L, syncStatus = "SYNCED", deviceId = "dev-1",
        )
        assertEquals(entity, entity.toDomain().toEntity())
    }

    @Test
    fun `MenuItemEntity with null categoryId round-trips`() {
        val entity = MenuItemEntity(
            id = "item-2", categoryId = null, name = "Es Teh", basePrice = 5_000L,
            isAvailable = true, isSoldOut = true,
            updatedAt = 2_000L, syncStatus = "PENDING", deviceId = "dev-1",
        )
        val domain = entity.toDomain()
        assertEquals(null, domain.categoryId)
        assertEquals(entity, domain.toEntity())
    }

    @Test
    fun `VariantGroupEntity round-trips and selectionType maps correctly`() {
        val entity = VariantGroupEntity(
            id = "group-1", menuItemId = "item-1", name = "Size",
            selectionType = "SINGLE", isRequired = true,
            updatedAt = 1_000L, syncStatus = "SYNCED", deviceId = "dev-1",
        )
        val domain = entity.toDomain()
        assertEquals(VariantSelectionType.SINGLE, domain.selectionType)
        assertEquals(entity, domain.toEntity())
    }

    @Test
    fun `VariantOptionEntity round-trips including negative priceDelta`() {
        val entity = VariantOptionEntity(
            id = "opt-1", variantGroupId = "group-1", name = "No Sugar", priceDelta = -2_000L,
            updatedAt = 1_000L, syncStatus = "SYNCED", deviceId = "dev-1",
        )
        assertEquals(entity, entity.toDomain().toEntity())
    }
}
