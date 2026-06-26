package com.wfx.warungpos.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class VariantSelection(
    val groupId: String,
    val groupName: String,
    val optionId: String,
    val optionName: String,
    val priceDelta: Long,
)
