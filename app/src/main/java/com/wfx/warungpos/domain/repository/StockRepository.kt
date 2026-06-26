package com.wfx.warungpos.domain.repository

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
    suspend fun saveBatch(batch: StockBatch)
    fun observeOpnames(): Flow<List<StockOpname>>
    suspend fun saveOpname(opname: StockOpname)
    suspend fun saveLine(line: StockOpnameLine)
}
