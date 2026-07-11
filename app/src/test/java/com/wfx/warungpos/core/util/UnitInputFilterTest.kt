package com.wfx.warungpos.core.util

import org.junit.Assert.assertEquals
import org.junit.Test

class UnitInputFilterTest {

    @Test
    fun `letters and spaces pass through unchanged`() {
        assertEquals("kg", filterUnitInput("kg"))
        assertEquals("pcs", filterUnitInput("pcs"))
        assertEquals("sq m", filterUnitInput("sq m"))
        assertEquals("", filterUnitInput(""))
    }

    @Test
    fun `UX regression - digits are stripped so a mistyped quantity can't corrupt the unit`() {
        assertEquals(" kg", filterUnitInput("20 kg"))
        assertEquals("kg", filterUnitInput("kg5"))
        assertEquals("", filterUnitInput("123"))
    }
}
