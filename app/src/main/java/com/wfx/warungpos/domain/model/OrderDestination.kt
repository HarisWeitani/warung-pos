package com.wfx.warungpos.domain.model

sealed class OrderDestination {
    object GrabAndGo : OrderDestination()
    data class NewTable(val tableId: String) : OrderDestination()
    data class ExistingBill(val billId: String) : OrderDestination()
}
