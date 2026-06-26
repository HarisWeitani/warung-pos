package com.wfx.warungpos.core.util

import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UuidGeneratorTest {

    @Test
    fun `generate returns valid UUID format`() {
        val uuid = UuidGenerator.generate()
        assertTrue(
            uuid.matches(Regex("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"))
        )
    }

    @Test
    fun `generate returns unique values`() {
        val uuid1 = UuidGenerator.generate()
        val uuid2 = UuidGenerator.generate()
        assertNotEquals(uuid1, uuid2)
    }
}
