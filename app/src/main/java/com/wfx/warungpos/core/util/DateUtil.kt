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

    // LocalDate.ofInstant(Instant, ZoneId) only exists from API 34; minSdk here is 26, so we
    // derive the LocalDate via LocalDateTime.ofInstant instead, which has been available since 26.
    private fun localDateOf(epochMs: Long): LocalDate =
        LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMs), WIB).toLocalDate()

    fun startOfDay(epochMs: Long): Long =
        localDateOf(epochMs)
            .atStartOfDay(WIB)
            .toInstant()
            .toEpochMilli()

    fun endOfDay(epochMs: Long): Long =
        localDateOf(epochMs)
            .plusDays(1)
            .atStartOfDay(WIB)
            .toInstant()
            .toEpochMilli() - 1L

    fun toDisplayString(epochMs: Long): String =
        LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMs), WIB).format(displayFormatter)

    fun toDisplayDate(epochMs: Long): String =
        localDateOf(epochMs).format(dateFormatter)

    fun toDisplayTime(epochMs: Long): String =
        LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMs), WIB).format(timeFormatter)

    fun nowEpochMs(): Long = System.currentTimeMillis()

    fun todayRangeWib(): Pair<Long, Long> {
        val now = nowEpochMs()
        return Pair(startOfDay(now), endOfDay(now))
    }
}
