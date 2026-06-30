package com.wfx.warungpos.data.local.mapper

import com.wfx.warungpos.core.common.ExpenseCategory
import com.wfx.warungpos.data.local.entity.ExpenseEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ExpenseMapperTest {

    @Test
    fun `ExpenseEntity round-trips through domain`() {
        val entity = ExpenseEntity(
            id = "exp-1", shiftId = "shift-1", category = "SUPPLIES", amount = 30_000L,
            note = "Gas refill", createdBy = "user-1",
            createdAt = 1_000L, updatedAt = 1_000L, syncStatus = "PENDING", deviceId = "dev-1",
        )
        val domain = entity.toDomain()
        assertEquals(ExpenseCategory.SUPPLIES, domain.category)
        assertEquals(30_000L, domain.amount)
        assertEquals(entity, domain.toEntity())
    }

    @Test
    fun `ExpenseEntity with null shiftId and note round-trips`() {
        val entity = ExpenseEntity(
            id = "exp-2", shiftId = null, category = "OTHER", amount = 5_000L,
            note = null, createdBy = "user-1",
            createdAt = 2_000L, updatedAt = 2_000L, syncStatus = "SYNCED", deviceId = "dev-1",
        )
        val domain = entity.toDomain()
        assertNull(domain.shiftId)
        assertNull(domain.note)
        assertEquals(entity, domain.toEntity())
    }

    @Test
    fun `monetary amount round-trips as Long unchanged`() {
        val entity = ExpenseEntity(
            id = "exp-3", shiftId = "shift-1", category = "RENT", amount = 15_000L,
            note = null, createdBy = "user-1",
            createdAt = 1_000L, updatedAt = 1_000L, syncStatus = "PENDING", deviceId = "dev-1",
        )
        assertEquals(15_000L, entity.toDomain().toEntity().amount)
    }
}
