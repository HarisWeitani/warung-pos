package com.wfx.warungpos.data.local.mapper

import com.wfx.warungpos.core.common.SyncStatus
import com.wfx.warungpos.data.local.entity.PaymentEntity
import com.wfx.warungpos.data.local.entity.PaymentMethodEntity
import com.wfx.warungpos.domain.model.Payment
import com.wfx.warungpos.domain.model.PaymentMethod

fun PaymentMethodEntity.toDomain() = PaymentMethod(
    id = id,
    name = name,
    isActive = isActive,
    isCash = isCash,
    sortOrder = sortOrder,
    updatedAt = updatedAt,
    syncStatus = SyncStatus.valueOf(syncStatus),
    deviceId = deviceId,
)

fun PaymentMethod.toEntity() = PaymentMethodEntity(
    id = id,
    name = name,
    isActive = isActive,
    isCash = isCash,
    sortOrder = sortOrder,
    updatedAt = updatedAt,
    syncStatus = syncStatus.name,
    deviceId = deviceId,
)

fun PaymentEntity.toDomain() = Payment(
    id = id,
    billId = billId,
    paymentMethodId = paymentMethodId,
    amount = amount,
    change = change,
    paidAt = paidAt,
    updatedAt = updatedAt,
    syncStatus = SyncStatus.valueOf(syncStatus),
    deviceId = deviceId,
)

fun Payment.toEntity() = PaymentEntity(
    id = id,
    billId = billId,
    paymentMethodId = paymentMethodId,
    amount = amount,
    change = change,
    paidAt = paidAt,
    updatedAt = updatedAt,
    syncStatus = syncStatus.name,
    deviceId = deviceId,
)
