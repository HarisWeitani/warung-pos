package com.wfx.warungpos.data.remote.sync

import com.google.firebase.database.DataSnapshot
import com.wfx.warungpos.core.common.SyncStatus
import com.wfx.warungpos.data.local.entity.BillEntity
import com.wfx.warungpos.data.local.entity.ExpenseEntity
import com.wfx.warungpos.data.local.entity.MenuCategoryEntity
import com.wfx.warungpos.data.local.entity.MenuItemEntity
import com.wfx.warungpos.data.local.entity.OrderItemEntity
import com.wfx.warungpos.data.local.entity.PaymentEntity
import com.wfx.warungpos.data.local.entity.PaymentMethodEntity
import com.wfx.warungpos.data.local.entity.ShiftEntity
import com.wfx.warungpos.data.local.entity.StockItemEntity
import com.wfx.warungpos.data.local.entity.VariantGroupEntity
import com.wfx.warungpos.data.local.entity.VariantOptionEntity

private val synced = SyncStatus.SYNCED.name

// ── DataSnapshot → Entity ────────────────────────────────────────────────────

internal fun DataSnapshot.toMenuCategoryEntity(): MenuCategoryEntity? {
    val id = key ?: return null
    return MenuCategoryEntity(
        id = id,
        name = child("name").getValue(String::class.java) ?: return null,
        sortOrder = (child("sortOrder").getValue(Long::class.java) ?: 0L).toInt(),
        updatedAt = child("updatedAt").getValue(Long::class.java) ?: 0L,
        syncStatus = synced,
        deviceId = child("deviceId").getValue(String::class.java) ?: "",
    )
}

internal fun DataSnapshot.toMenuItemEntity(): MenuItemEntity? {
    val id = key ?: return null
    return MenuItemEntity(
        id = id,
        categoryId = child("categoryId").getValue(String::class.java),
        name = child("name").getValue(String::class.java) ?: return null,
        basePrice = child("basePrice").getValue(Long::class.java) ?: 0L,
        isAvailable = child("isAvailable").getValue(Boolean::class.java) ?: true,
        isSoldOut = child("isSoldOut").getValue(Boolean::class.java) ?: false,
        updatedAt = child("updatedAt").getValue(Long::class.java) ?: 0L,
        syncStatus = synced,
        deviceId = child("deviceId").getValue(String::class.java) ?: "",
    )
}

internal fun DataSnapshot.toVariantGroupEntity(): VariantGroupEntity? {
    val id = key ?: return null
    return VariantGroupEntity(
        id = id,
        menuItemId = child("menuItemId").getValue(String::class.java) ?: return null,
        name = child("name").getValue(String::class.java) ?: return null,
        selectionType = child("selectionType").getValue(String::class.java) ?: "SINGLE",
        isRequired = child("isRequired").getValue(Boolean::class.java) ?: false,
        updatedAt = child("updatedAt").getValue(Long::class.java) ?: 0L,
        syncStatus = synced,
        deviceId = child("deviceId").getValue(String::class.java) ?: "",
    )
}

internal fun DataSnapshot.toVariantOptionEntity(): VariantOptionEntity? {
    val id = key ?: return null
    return VariantOptionEntity(
        id = id,
        variantGroupId = child("variantGroupId").getValue(String::class.java) ?: return null,
        name = child("name").getValue(String::class.java) ?: return null,
        priceDelta = child("priceDelta").getValue(Long::class.java) ?: 0L,
        updatedAt = child("updatedAt").getValue(Long::class.java) ?: 0L,
        syncStatus = synced,
        deviceId = child("deviceId").getValue(String::class.java) ?: "",
    )
}

internal fun DataSnapshot.toShiftEntity(): ShiftEntity? {
    val id = key ?: return null
    return ShiftEntity(
        id = id,
        openedBy = child("openedBy").getValue(String::class.java) ?: return null,
        closedBy = child("closedBy").getValue(String::class.java),
        status = child("status").getValue(String::class.java) ?: return null,
        openedAt = child("openedAt").getValue(Long::class.java) ?: return null,
        closedAt = child("closedAt").getValue(Long::class.java),
        openingFloat = child("openingFloat").getValue(Long::class.java) ?: 0L,
        closingFloat = child("closingFloat").getValue(Long::class.java),
        updatedAt = child("updatedAt").getValue(Long::class.java) ?: 0L,
        syncStatus = synced,
        deviceId = child("deviceId").getValue(String::class.java) ?: "",
    )
}

internal fun DataSnapshot.toBillEntity(): BillEntity? {
    val id = key ?: return null
    return BillEntity(
        id = id,
        status = child("status").getValue(String::class.java) ?: return null,
        sessionLabel = child("sessionLabel").getValue(String::class.java) ?: return null,
        createdAt = child("createdAt").getValue(Long::class.java) ?: return null,
        paidAt = child("paidAt").getValue(Long::class.java),
        subtotal = child("subtotal").getValue(Long::class.java) ?: 0L,
        discountTotal = child("discountTotal").getValue(Long::class.java) ?: 0L,
        grandTotal = child("grandTotal").getValue(Long::class.java) ?: 0L,
        note = child("note").getValue(String::class.java),
        shiftId = child("shiftId").getValue(String::class.java),
        voidReason = child("voidReason").getValue(String::class.java),
        voidedBy = child("voidedBy").getValue(String::class.java),
        updatedAt = child("updatedAt").getValue(Long::class.java) ?: 0L,
        syncStatus = synced,
        deviceId = child("deviceId").getValue(String::class.java) ?: "",
    )
}

internal fun DataSnapshot.toOrderItemEntity(): OrderItemEntity? {
    val id = key ?: return null
    return OrderItemEntity(
        id = id,
        billId = child("billId").getValue(String::class.java) ?: return null,
        menuItemId = child("menuItemId").getValue(String::class.java),
        nameSnapshot = child("nameSnapshot").getValue(String::class.java) ?: return null,
        priceSnapshot = child("priceSnapshot").getValue(Long::class.java) ?: 0L,
        quantity = (child("quantity").getValue(Long::class.java) ?: 1L).toInt(),
        selectedVariantsJson = child("selectedVariantsJson").getValue(String::class.java) ?: "[]",
        lineTotal = child("lineTotal").getValue(Long::class.java) ?: 0L,
        status = child("status").getValue(String::class.java) ?: return null,
        voidReason = child("voidReason").getValue(String::class.java),
        voidedBy = child("voidedBy").getValue(String::class.java),
        createdAt = child("createdAt").getValue(Long::class.java) ?: return null,
        updatedAt = child("updatedAt").getValue(Long::class.java) ?: 0L,
        syncStatus = synced,
        deviceId = child("deviceId").getValue(String::class.java) ?: "",
    )
}

internal fun DataSnapshot.toPaymentMethodEntity(): PaymentMethodEntity? {
    val id = key ?: return null
    return PaymentMethodEntity(
        id = id,
        name = child("name").getValue(String::class.java) ?: return null,
        isActive = child("isActive").getValue(Boolean::class.java) ?: true,
        isCash = child("isCash").getValue(Boolean::class.java) ?: false,
        sortOrder = (child("sortOrder").getValue(Long::class.java) ?: 0L).toInt(),
        updatedAt = child("updatedAt").getValue(Long::class.java) ?: 0L,
        syncStatus = synced,
        deviceId = child("deviceId").getValue(String::class.java) ?: "",
    )
}

internal fun DataSnapshot.toPaymentEntity(): PaymentEntity? {
    val id = key ?: return null
    return PaymentEntity(
        id = id,
        billId = child("billId").getValue(String::class.java) ?: return null,
        paymentMethodId = child("paymentMethodId").getValue(String::class.java) ?: return null,
        amount = child("amount").getValue(Long::class.java) ?: return null,
        change = child("change").getValue(Long::class.java) ?: 0L,
        paidAt = child("paidAt").getValue(Long::class.java) ?: return null,
        updatedAt = child("updatedAt").getValue(Long::class.java) ?: 0L,
        syncStatus = synced,
        deviceId = child("deviceId").getValue(String::class.java) ?: "",
    )
}

internal fun DataSnapshot.toExpenseEntity(): ExpenseEntity? {
    val id = key ?: return null
    return ExpenseEntity(
        id = id,
        shiftId = child("shiftId").getValue(String::class.java),
        category = child("category").getValue(String::class.java) ?: return null,
        amount = child("amount").getValue(Long::class.java) ?: return null,
        note = child("note").getValue(String::class.java),
        createdBy = child("createdBy").getValue(String::class.java) ?: return null,
        createdAt = child("createdAt").getValue(Long::class.java) ?: return null,
        updatedAt = child("updatedAt").getValue(Long::class.java) ?: 0L,
        syncStatus = synced,
        deviceId = child("deviceId").getValue(String::class.java) ?: "",
    )
}

internal fun DataSnapshot.toStockItemEntity(): StockItemEntity? {
    val id = key ?: return null
    return StockItemEntity(
        id = id,
        name = child("name").getValue(String::class.java) ?: return null,
        unit = child("unit").getValue(String::class.java) ?: return null,
        currentQty = child("currentQty").getValue(Double::class.java) ?: 0.0,
        reorderPoint = child("reorderPoint").getValue(Double::class.java) ?: 0.0,
        updatedAt = child("updatedAt").getValue(Long::class.java) ?: 0L,
        syncStatus = synced,
        deviceId = child("deviceId").getValue(String::class.java) ?: "",
    )
}

// ── Entity → RTDB Map ────────────────────────────────────────────────────────

internal fun MenuCategoryEntity.toRtdbMap(): Map<String, Any?> = mapOf(
    "name" to name, "sortOrder" to sortOrder, "updatedAt" to updatedAt, "deviceId" to deviceId,
)

internal fun MenuItemEntity.toRtdbMap(): Map<String, Any?> = mapOf(
    "categoryId" to categoryId, "name" to name, "basePrice" to basePrice,
    "isAvailable" to isAvailable, "isSoldOut" to isSoldOut, "updatedAt" to updatedAt,
    "deviceId" to deviceId,
)

internal fun VariantGroupEntity.toRtdbMap(): Map<String, Any?> = mapOf(
    "menuItemId" to menuItemId, "name" to name, "selectionType" to selectionType,
    "isRequired" to isRequired, "updatedAt" to updatedAt, "deviceId" to deviceId,
)

internal fun VariantOptionEntity.toRtdbMap(): Map<String, Any?> = mapOf(
    "variantGroupId" to variantGroupId, "name" to name, "priceDelta" to priceDelta,
    "updatedAt" to updatedAt, "deviceId" to deviceId,
)

internal fun ShiftEntity.toRtdbMap(): Map<String, Any?> = mapOf(
    "openedBy" to openedBy, "closedBy" to closedBy, "status" to status,
    "openedAt" to openedAt, "closedAt" to closedAt, "openingFloat" to openingFloat,
    "closingFloat" to closingFloat, "updatedAt" to updatedAt, "deviceId" to deviceId,
)

internal fun BillEntity.toRtdbMap(): Map<String, Any?> = mapOf(
    "status" to status, "sessionLabel" to sessionLabel,
    "createdAt" to createdAt, "paidAt" to paidAt, "subtotal" to subtotal,
    "discountTotal" to discountTotal, "grandTotal" to grandTotal, "note" to note,
    "shiftId" to shiftId, "voidReason" to voidReason, "voidedBy" to voidedBy,
    "updatedAt" to updatedAt, "deviceId" to deviceId,
)

internal fun OrderItemEntity.toRtdbMap(): Map<String, Any?> = mapOf(
    "billId" to billId, "menuItemId" to menuItemId, "nameSnapshot" to nameSnapshot,
    "priceSnapshot" to priceSnapshot, "quantity" to quantity,
    "selectedVariantsJson" to selectedVariantsJson, "lineTotal" to lineTotal,
    "status" to status, "voidReason" to voidReason, "voidedBy" to voidedBy,
    "createdAt" to createdAt, "updatedAt" to updatedAt, "deviceId" to deviceId,
)

internal fun PaymentMethodEntity.toRtdbMap(): Map<String, Any?> = mapOf(
    "name" to name, "isActive" to isActive, "isCash" to isCash, "sortOrder" to sortOrder,
    "updatedAt" to updatedAt, "deviceId" to deviceId,
)

internal fun PaymentEntity.toRtdbMap(): Map<String, Any?> = mapOf(
    "billId" to billId, "paymentMethodId" to paymentMethodId, "amount" to amount,
    "change" to change, "paidAt" to paidAt, "updatedAt" to updatedAt, "deviceId" to deviceId,
)

internal fun ExpenseEntity.toRtdbMap(): Map<String, Any?> = mapOf(
    "shiftId" to shiftId, "category" to category, "amount" to amount, "note" to note,
    "createdBy" to createdBy, "createdAt" to createdAt, "updatedAt" to updatedAt,
    "deviceId" to deviceId,
)

internal fun StockItemEntity.toRtdbMap(): Map<String, Any?> = mapOf(
    "name" to name, "unit" to unit, "currentQty" to currentQty, "reorderPoint" to reorderPoint,
    "updatedAt" to updatedAt, "deviceId" to deviceId,
)
