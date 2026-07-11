package com.wfx.warungpos.core.common

enum class VoidReason {
    CUSTOMER_CHANGE, KITCHEN_ERROR, ITEM_UNAVAILABLE, TEST, OTHER,

    /** DEFECT-009: attributed to order_items cascade-voided as a side effect of voiding their
     * whole bill (VoidBillUseCase), as opposed to an item voided individually by the operator. */
    BILL_VOID,
}
