package com.wfx.warungpos.core.util

import org.junit.Assert.assertEquals
import org.junit.Test

class CurrencyFormatterTest {

    @Test
    fun `format 15000 returns Rp 15000 with dot thousands separator`() {
        assertEquals("Rp 15.000", CurrencyFormatter.format(15_000L))
    }

    @Test
    fun `format 0 returns Rp 0`() {
        assertEquals("Rp 0", CurrencyFormatter.format(0L))
    }

    @Test
    fun `format 500 returns Rp 500 without separator`() {
        assertEquals("Rp 500", CurrencyFormatter.format(500L))
    }

    @Test
    fun `format 1500000 returns Rp 1500000 with multiple separators`() {
        assertEquals("Rp 1.500.000", CurrencyFormatter.format(1_500_000L))
    }

    @Test
    fun `format 1000 returns Rp 1000`() {
        assertEquals("Rp 1.000", CurrencyFormatter.format(1_000L))
    }
}
