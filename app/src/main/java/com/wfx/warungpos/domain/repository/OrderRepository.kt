package com.wfx.warungpos.domain.repository

import com.wfx.warungpos.core.common.VoidReason
import com.wfx.warungpos.domain.model.OrderItem
import kotlinx.coroutines.flow.Flow

interface OrderRepository {
    fun observeOrderItems(billId: String): Flow<List<OrderItem>>
    suspend fun getActiveItems(billId: String): List<OrderItem>
    suspend fun getItemById(id: String): OrderItem?
    suspend fun saveItem(item: OrderItem)
    suspend fun voidItem(id: String, reason: VoidReason, voidedBy: String)
}
