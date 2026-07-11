package com.wfx.warungpos.core.util

import org.junit.Assert.assertEquals
import org.junit.Test

class DecimalInputFilterTest {

    @Test
    fun `digits and a single dot pass through unchanged`() {
        assertEquals("12.5", filterDecimalInput("12.5"))
        assertEquals("12", filterDecimalInput("12"))
        assertEquals("", filterDecimalInput(""))
    }

    @Test
    fun `DEFECT-011 regression - a second dot is dropped, not merely left unparsed`() {
        assertEquals("2.51", filterDecimalInput("2.5.1"))
        assertEquals("2.5", filterDecimalInput("2..5"))
        assertEquals(".5", filterDecimalInput("..5"))
    }

    @Test
    fun `non-numeric characters are stripped`() {
        assertEquals("125", filterDecimalInput("1a2b5"))
        assertEquals("1.25", filterDecimalInput("Rp 1.2.5"))
    }

    @Test
    fun `result is always parseable by toDoubleOrNull when non-blank and not a lone dot`() {
        val filtered = filterDecimalInput("2.5.1.9.3")
        assertEquals("2.5193", filtered)
        assert(filtered.toDoubleOrNull() != null)
    }
}
