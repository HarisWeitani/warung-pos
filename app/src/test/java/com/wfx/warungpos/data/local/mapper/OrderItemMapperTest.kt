package com.wfx.warungpos.data.local.mapper

import com.wfx.warungpos.core.common.OrderItemStatus
import com.wfx.warungpos.core.common.VoidReason
import com.wfx.warungpos.data.local.entity.OrderItemEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OrderItemMapperTest {

    @Test
    fun `OrderItemEntity with no variants round-trips`() {
        val entity = OrderItemEntity(
            id = "item-1", billId = "bill-1", menuItemId = "menu-1", nameSnapshot = "Nasi Goreng",
            priceSnapshot = 15_000L, quantity = 2, selectedVariantsJson = "[]", lineTotal = 30_000L,
            status = "ORDERED", voidReason = null, voidNote = null, voidedBy = null,
            createdAt = 1_000L, updatedAt = 1_000L, syncStatus = "PENDING", deviceId = "dev-1",
        )
        val domain = entity.toDomain()
        assertEquals(OrderItemStatus.ORDERED, domain.status)
        assertEquals(0, domain.selectedVariants.size)
        assertEquals(entity, domain.toEntity())
    }

    @Test
    fun `selectedVariantsJson with two variant groups decodes all fields correctly`() {
        val json = """[
            {"groupId":"g1","groupName":"Size","optionId":"o1","optionName":"Large","priceDelta":3000},
            {"groupId":"g2","groupName":"Spice","optionId":"o2","optionName":"Extra Hot","priceDelta":2000}
        ]"""
        val entity = OrderItemEntity(
            id = "item-2", billId = "bill-1", menuItemId = "menu-1", nameSnapshot = "Nasi Goreng",
            priceSnapshot = 20_000L, quantity = 1, selectedVariantsJson = json, lineTotal = 20_000L,
            status = "ORDERED", voidReason = null, voidNote = null, voidedBy = null,
            createdAt = 1_000L, updatedAt = 1_000L, syncStatus = "SYNCED", deviceId = "dev-1",
        )
        val domain = entity.toDomain()
        assertEquals(2, domain.selectedVariants.size)
        assertEquals("Size", domain.selectedVariants[0].groupName)
        assertEquals(3_000L, domain.selectedVariants[0].priceDelta)
        assertEquals("Extra Hot", domain.selectedVariants[1].optionName)
        assertEquals(2_000L, domain.selectedVariants[1].priceDelta)

        val backToEntity = domain.toEntity()
        val redecoded = backToEntity.toDomain()
        assertEquals(domain.selectedVariants, redecoded.selectedVariants)
    }

    @Test
    fun `voided item round-trips voidReason and voidedBy`() {
        val entity = OrderItemEntity(
            id = "item-3", billId = "bill-1", menuItemId = "menu-1", nameSnapshot = "Es Teh",
            priceSnapshot = 5_000L, quantity = 1, selectedVariantsJson = "[]", lineTotal = 5_000L,
            status = "VOID", voidReason = VoidReason.KITCHEN_ERROR.name, voidNote = null, voidedBy = "user-1",
            createdAt = 1_000L, updatedAt = 2_000L, syncStatus = "PENDING", deviceId = "dev-1",
        )
        val domain = entity.toDomain()
        assertEquals(VoidReason.KITCHEN_ERROR, domain.voidReason)
        assertEquals("user-1", domain.voidedBy)
        assertEquals(entity, domain.toEntity())
    }

    @Test
    fun `DEFECT-006 regression - voidNote round-trips through the mapper`() {
        val entity = OrderItemEntity(
            id = "item-5", billId = "bill-1", menuItemId = "menu-1", nameSnapshot = "Es Teh",
            priceSnapshot = 5_000L, quantity = 1, selectedVariantsJson = "[]", lineTotal = 5_000L,
            status = "VOID", voidReason = VoidReason.OTHER.name, voidNote = "Customer allergic to sugar",
            voidedBy = "user-1", createdAt = 1_000L, updatedAt = 2_000L, syncStatus = "PENDING", deviceId = "dev-1",
        )
        val domain = entity.toDomain()
        assertEquals("Customer allergic to sugar", domain.voidNote)
        assertEquals(entity, domain.toEntity())
    }

    @Test
    fun `null menuItemId round-trips`() {
        val entity = OrderItemEntity(
            id = "item-4", billId = "bill-1", menuItemId = null, nameSnapshot = "Removed Item",
            priceSnapshot = 5_000L, quantity = 1, selectedVariantsJson = "[]", lineTotal = 5_000L,
            status = "ORDERED", voidReason = null, voidNote = null, voidedBy = null,
            createdAt = 1_000L, updatedAt = 1_000L, syncStatus = "PENDING", deviceId = "dev-1",
        )
        val domain = entity.toDomain()
        assertNull(domain.menuItemId)
        assertEquals(entity, domain.toEntity())
    }
}
