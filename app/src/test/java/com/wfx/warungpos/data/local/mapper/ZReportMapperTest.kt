package com.wfx.warungpos.data.local.mapper

import com.wfx.warungpos.data.local.entity.ZReportEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class ZReportMapperTest {

    @Test
    fun `ZReportEntity round-trips through domain`() {
        val snapshot = """{"revenue":150000,"expenses":30000,"transactions":7,"voidCount":1,"voidValue":5000,"openingFloat":100000,"countedCash":150000,"expectedCash":155000,"variance":-5000,"paymentBreakdown":[{"methodId":"pm_tunai","total":100000},{"methodId":"pm_qris","total":50000}]}"""
        val entity = ZReportEntity(
            id = "zreport-1", shiftId = "shift-1", snapshotJson = snapshot, createdAt = 1_000L,
        )
        val domain = entity.toDomain()
        assertEquals(snapshot, domain.snapshotJson)
        assertEquals(entity, domain.toEntity())
    }

    @Test
    fun `complex snapshotJson is preserved byte-for-byte through round-trip`() {
        val snapshot = """{"a":1,"b":[1,2,3],"c":{"nested":true},"d":null}"""
        val entity = ZReportEntity(id = "zreport-2", shiftId = "shift-2", snapshotJson = snapshot, createdAt = 2_000L)
        val roundTripped = entity.toDomain().toEntity()
        assertEquals(entity, roundTripped)
        assertEquals(snapshot, roundTripped.snapshotJson)
    }
}
