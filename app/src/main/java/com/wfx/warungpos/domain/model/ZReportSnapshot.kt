package com.wfx.warungpos.domain.model

/** Parsed form of [ZReport.snapshotJson] — the immutable figures computed and persisted at the
 * moment a shift was closed (manually or via auto-close), as opposed to figures re-derived live
 * from the current state of the DB. See DEFECT-010: a closed shift's Z-report must show these
 * exact persisted numbers, not a live re-derivation that can drift from what was actually true
 * (and reported to the operator) at close time. */
data class ZReportSnapshot(
    val revenue: Long,
    val expenses: Long,
    val transactions: Int,
    val voidCount: Int,
    val voidValue: Long,
    val openingFloat: Long,
    val countedCash: Long,
    val expectedCash: Long,
    val variance: Long,
    val paymentBreakdown: List<PaymentBreakdown>,
)
