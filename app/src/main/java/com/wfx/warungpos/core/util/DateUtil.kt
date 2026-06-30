package com.wfx.warungpos.core.util

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object DateUtil {
    val WIB: ZoneId = ZoneId.of("Asia/Jakarta")

    private val displayFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm", Locale("id", "ID"))
    private val dateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale("id", "ID"))
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    fun startOfDay(epochMs: Long): Long =
        LocalDate.ofInstant(Instant.ofEpochMilli(epochMs), WIB)
            .atStartOfDay(WIB)
            .toInstant()
            .toEpochMilli()

    fun endOfDay(epochMs: Long): Long =
        LocalDate.ofInstant(Instant.ofEpochMilli(epochMs), WIB)
            .plusDays(1)
            .atStartOfDay(WIB)
            .toInstant()
            .toEpochMilli() - 1L

    fun toDisplayString(epochMs: Long): String =
        LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMs), WIB).format(displayFormatter)

    fun toDisplayDate(epochMs: Long): String =
        LocalDate.ofInstant(Instant.ofEpochMilli(epochMs), WIB).format(dateFormatter)

    fun toDisplayTime(epochMs: Long): String =
        LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMs), WIB).format(timeFormatter)

    fun nowEpochMs(): Long = System.currentTimeMillis()

    fun todayRangeWib(): Pair<Long, Long> {
        val now = nowEpochMs()
        return Pair(startOfDay(now), endOfDay(now))
    }
}
