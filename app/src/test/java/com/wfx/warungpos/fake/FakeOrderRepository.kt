package com.wfx.warungpos.fake

import com.wfx.warungpos.core.common.OrderItemStatus
import com.wfx.warungpos.core.common.VoidReason
import com.wfx.warungpos.domain.model.OrderItem
import com.wfx.warungpos.domain.repository.OrderRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class FakeOrderRepository : OrderRepository {
    val items = mutableMapOf<String, OrderItem>()

    override fun observeOrderItems(billId: String): Flow<List<OrderItem>> =
        flowOf(items.values.filter { it.billId == billId })

    override suspend fun getActiveItems(billId: String): List<OrderItem> =
        items.values.filter { it.billId == billId && it.status != OrderItemStatus.VOID }

    override suspend fun getItemById(id: String): OrderItem? = items[id]

    override suspend fun saveItem(item: OrderItem) {
        items[item.id] = item
    }

    override suspend fun voidItem(id: String, reason: VoidReason, voidedBy: String) {
        items[id]?.let { items[id] = it.copy(status = OrderItemStatus.VOID, voidReason = reason, voidedBy = voidedBy) }
    }
}
