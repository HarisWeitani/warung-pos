package com.wfx.warungpos.core.util

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class DateUtilTest {

    private val wib = ZoneId.of("Asia/Jakarta")

    @Test
    fun `startOfDay returns midnight WIB for mid-day timestamp`() {
        val input = ZonedDateTime.of(2024, 1, 15, 10, 30, 0, 0, wib).toInstant().toEpochMilli()
        val expected = ZonedDateTime.of(2024, 1, 15, 0, 0, 0, 0, wib).toInstant().toEpochMilli()
        assertEquals(expected, DateUtil.startOfDay(input))
    }

    @Test
    fun `startOfDay at midnight returns same midnight`() {
        val midnight = ZonedDateTime.of(2024, 1, 15, 0, 0, 0, 0, wib).toInstant().toEpochMilli()
        assertEquals(midnight, DateUtil.startOfDay(midnight))
    }

    @Test
    fun `endOfDay returns last millisecond of day in WIB`() {
        val input = ZonedDateTime.of(2024, 1, 15, 10, 30, 0, 0, wib).toInstant().toEpochMilli()
        val expected = ZonedDateTime.of(2024, 1, 16, 0, 0, 0, 0, wib).toInstant().toEpochMilli() - 1L
        assertEquals(expected, DateUtil.endOfDay(input))
    }

    @Test
    fun `startOfDay and endOfDay span exactly 24 hours minus 1ms`() {
        val input = ZonedDateTime.of(2024, 6, 15, 12, 0, 0, 0, wib).toInstant().toEpochMilli()
        val start = DateUtil.startOfDay(input)
        val end = DateUtil.endOfDay(input)
        // Indonesia has no DST so this is always exactly 86400000 - 1
        assertEquals(86_400_000L - 1L, end - start)
    }

    @Test
    fun `startOfDay is consistent with endOfDay across midnight boundary`() {
        val beforeMidnight = ZonedDateTime.of(2024, 3, 10, 23, 59, 59, 0, wib).toInstant().toEpochMilli()
        val afterMidnight = ZonedDateTime.of(2024, 3, 11, 0, 0, 1, 0, wib).toInstant().toEpochMilli()
        val endOfPrev = DateUtil.endOfDay(beforeMidnight)
        val startOfNext = DateUtil.startOfDay(afterMidnight)
        assertEquals(1L, startOfNext - endOfPrev)
    }
}
