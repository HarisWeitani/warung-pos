package com.wfx.warungpos.domain.repository

import com.wfx.warungpos.domain.model.MenuItemIngredient
import com.wfx.warungpos.domain.model.StockBatch
import com.wfx.warungpos.domain.model.StockItem
import com.wfx.warungpos.domain.model.StockOpname
import com.wfx.warungpos.domain.model.StockOpnameLine
import kotlinx.coroutines.flow.Flow

interface StockRepository {
    fun observeAllItems(): Flow<List<StockItem>>
    fun observeLowStock(): Flow<List<StockItem>>
    suspend fun getItem(id: String): StockItem?
    suspend fun saveItem(item: StockItem)
    suspend fun getAllItemsOnce(): List<StockItem>

    fun observeAllBatches(): Flow<List<StockBatch>>
    suspend fun saveBatch(batch: StockBatch)

    fun observeIngredientsForMenuItem(menuItemId: String): Flow<List<MenuItemIngredient>>
    suspend fun getIngredientsForMenuItem(menuItemId: String): List<MenuItemIngredient>
    suspend fun saveIngredient(ingredient: MenuItemIngredient)
    suspend fun deleteIngredient(menuItemId: String, stockItemId: String)

    fun observeOpnames(): Flow<List<StockOpname>>
    fun observeInProgressOpname(): Flow<StockOpname?>
    suspend fun getLinesForOpname(opnameId: String): List<StockOpnameLine>
    fun observeLinesForOpname(opnameId: String): Flow<List<StockOpnameLine>>
    suspend fun saveOpname(opname: StockOpname)
    suspend fun saveLine(line: StockOpnameLine)
    suspend fun saveLines(lines: List<StockOpnameLine>)

    /** Saves the opname and its snapshot lines in one transaction: the opname row must exist
     * before the lines (FK), and both must be visible to observers atomically so the UI never
     * reads the new opname before its lines are there. */
    suspend fun startOpname(opname: StockOpname, lines: List<StockOpnameLine>)

    /** Directly sets currentQty without going through a batch or opname (used by opname submit). */
    suspend fun setCurrentQty(stockItemId: String, qty: Double)

    /** Decrements currentQty by [amount] (used for ingredient deduction on payment). */
    suspend fun deductQty(stockItemId: String, amount: Double)
}
