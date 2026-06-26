package com.wfx.warungpos.domain.model

data class ZReport(
    val id: String,
    val shiftId: String,
    val snapshotJson: String,
    val createdAt: Long,
)
