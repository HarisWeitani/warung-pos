package com.wfx.warungpos.fake

import com.wfx.warungpos.core.common.OpnameStatus
import com.wfx.warungpos.domain.model.MenuItemIngredient
import com.wfx.warungpos.domain.model.StockBatch
import com.wfx.warungpos.domain.model.StockItem
import com.wfx.warungpos.domain.model.StockOpname
import com.wfx.warungpos.domain.model.StockOpnameLine
import com.wfx.warungpos.domain.repository.StockRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class FakeStockRepository : StockRepository {
    val items = mutableMapOf<String, StockItem>()
    val batches = mutableMapOf<String, StockBatch>()
    val ingredients = mutableListOf<MenuItemIngredient>()
    val opnames = mutableMapOf<String, StockOpname>()
    val lines = mutableMapOf<String, StockOpnameLine>()

    override fun observeAllItems(): Flow<List<StockItem>> = flowOf(items.values.toList())

    override fun observeLowStock(): Flow<List<StockItem>> =
        flowOf(items.values.filter { it.currentQty <= it.reorderPoint })

    override suspend fun getItem(id: String): StockItem? = items[id]

    override suspend fun saveItem(item: StockItem) {
        items[item.id] = item
    }

    override suspend fun getAllItemsOnce(): List<StockItem> = items.values.toList()

    override fun observeAllBatches(): Flow<List<StockBatch>> = flowOf(batches.values.toList())

    override suspend fun saveBatch(batch: StockBatch) {
        batches[batch.id] = batch
    }

    override fun observeIngredientsForMenuItem(menuItemId: String): Flow<List<MenuItemIngredient>> =
        flowOf(ingredients.filter { it.menuItemId == menuItemId })

    override suspend fun getIngredientsForMenuItem(menuItemId: String): List<MenuItemIngredient> =
        ingredients.filter { it.menuItemId == menuItemId }

    override suspend fun saveIngredient(ingredient: MenuItemIngredient) {
        ingredients.removeAll { it.menuItemId == ingredient.menuItemId && it.stockItemId == ingredient.stockItemId }
        ingredients.add(ingredient)
    }

    override suspend fun deleteIngredient(menuItemId: String, stockItemId: String) {
        ingredients.removeAll { it.menuItemId == menuItemId && it.stockItemId == stockItemId }
    }

    override fun observeOpnames(): Flow<List<StockOpname>> = flowOf(opnames.values.toList())

    override fun observeInProgressOpname(): Flow<StockOpname?> =
        flowOf(opnames.values.firstOrNull { it.status == OpnameStatus.IN_PROGRESS })

    override suspend fun getLinesForOpname(opnameId: String): List<StockOpnameLine> =
        lines.values.filter { it.opnameId == opnameId }

    override fun observeLinesForOpname(opnameId: String): Flow<List<StockOpnameLine>> =
        flowOf(lines.values.filter { it.opnameId == opnameId })

    override suspend fun saveOpname(opname: StockOpname) {
        opnames[opname.id] = opname
    }

    override suspend fun saveLine(line: StockOpnameLine) {
        lines[line.id] = line
    }

    override suspend fun saveLines(lines: List<StockOpnameLine>) {
        lines.forEach { this.lines[it.id] = it }
    }

    override suspend fun setCurrentQty(stockItemId: String, qty: Double) {
        items[stockItemId]?.let { items[stockItemId] = it.copy(currentQty = qty) }
    }

    override suspend fun deductQty(stockItemId: String, amount: Double) {
        items[stockItemId]?.let { items[stockItemId] = it.copy(currentQty = it.currentQty - amount) }
    }
}
