package com.wfx.warungpos.data.local.mapper

import com.wfx.warungpos.domain.model.PaymentBreakdown
import com.wfx.warungpos.domain.model.ZReport
import com.wfx.warungpos.domain.model.ZReportSnapshot
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

private val json = Json { ignoreUnknownKeys = true }

@Serializable
private data class ZReportSnapshotDto(
    val revenue: Long,
    val expenses: Long,
    val transactions: Int,
    val voidCount: Int,
    val voidValue: Long,
    val openingFloat: Long,
    val countedCash: Long,
    val expectedCash: Long,
    val variance: Long,
    val paymentBreakdown: List<PaymentEntryDto>,
)

@Serializable
private data class PaymentEntryDto(val methodId: String, val total: Long)

/** DEFECT-010: parses the immutable snapshot GenerateZReportUseCase wrote at close time, instead
 * of the screen re-deriving figures live. Returns null if the snapshot is malformed/unreadable
 * (defensive — the JSON is hand-built, not written through this same serializer) so callers can
 * fall back rather than crash. */
fun ZReport.toSnapshot(): ZReportSnapshot? = runCatching {
    val dto = json.decodeFromString<ZReportSnapshotDto>(snapshotJson)
    ZReportSnapshot(
        revenue = dto.revenue,
        expenses = dto.expenses,
        transactions = dto.transactions,
        voidCount = dto.voidCount,
        voidValue = dto.voidValue,
        openingFloat = dto.openingFloat,
        countedCash = dto.countedCash,
        expectedCash = dto.expectedCash,
        variance = dto.variance,
        paymentBreakdown = dto.paymentBreakdown.map { PaymentBreakdown(paymentMethodId = it.methodId, total = it.total) },
    )
}.getOrNull()
