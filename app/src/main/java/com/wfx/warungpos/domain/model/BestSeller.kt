package com.wfx.warungpos.domain.model

data class BestSeller(
    val menuItemId: String,
    val name: String,
    val totalQty: Int,
    val totalRevenue: Long,
)
