package com.wfx.warungpos.data.local.mapper

import com.wfx.warungpos.core.common.BillStatus
import com.wfx.warungpos.core.common.BillType
import com.wfx.warungpos.core.common.SyncStatus
import com.wfx.warungpos.core.common.VoidReason
import com.wfx.warungpos.data.local.entity.BillEntity
import com.wfx.warungpos.domain.model.Bill
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BillMapperTest {

    private val entity = BillEntity(
        id = "bill-1",
        tableId = "table-1",
        type = "OPEN_BILL",
        status = "OPEN",
        sessionLabel = "Meja 1",
        createdAt = 1_000L,
        paidAt = null,
        subtotal = 50_000L,
        discountTotal = 0L,
        grandTotal = 50_000L,
        note = null,
        shiftId = "shift-1",
        voidReason = null,
        voidedBy = null,
        updatedAt = 1_000L,
        syncStatus = "PENDING",
        deviceId = "dev-1",
    )

    @Test
    fun `entity toDomain maps all fields correctly`() {
        val domain = entity.toDomain()
        assertEquals("bill-1", domain.id)
        assertEquals(BillType.OPEN_BILL, domain.type)
        assertEquals(BillStatus.OPEN, domain.status)
        assertEquals(SyncStatus.PENDING, domain.syncStatus)
        assertNull(domain.voidReason)
    }

    @Test
    fun `domain toEntity round-trips correctly`() {
        val domain = entity.toDomain()
        val backToEntity = domain.toEntity()
        assertEquals(entity, backToEntity)
    }

    @Test
    fun `voidReason is mapped from enum to string and back`() {
        val withVoid = entity.copy(
            status = "VOID",
            voidReason = VoidReason.KITCHEN_ERROR.name,
        )
        val domain = withVoid.toDomain()
        assertEquals(VoidReason.KITCHEN_ERROR, domain.voidReason)
        assertEquals(VoidReason.KITCHEN_ERROR.name, domain.toEntity().voidReason)
    }

    @Test
    fun `domain Bill with enum types toEntity preserves string representations`() {
        val domain = Bill(
            id = "bill-2",
            tableId = null,
            type = BillType.UPFRONT,
            status = BillStatus.PAID,
            sessionLabel = "Counter",
            createdAt = 2_000L,
            paidAt = 3_000L,
            subtotal = 20_000L,
            discountTotal = 0L,
            grandTotal = 20_000L,
            note = "rush order",
            shiftId = null,
            voidReason = null,
            voidedBy = null,
            updatedAt = 3_000L,
            syncStatus = SyncStatus.SYNCED,
            deviceId = "dev-2",
        )
        val entity = domain.toEntity()
        assertEquals("UPFRONT", entity.type)
        assertEquals("PAID", entity.status)
        assertEquals("SYNCED", entity.syncStatus)
    }
}
