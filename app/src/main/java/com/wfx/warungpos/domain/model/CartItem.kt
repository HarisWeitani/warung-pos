package com.wfx.warungpos.domain.model

data class CartItem(
    val menuItem: MenuItem,
    val quantity: Int,
    val selectedVariants: List<VariantSelection>,
)
