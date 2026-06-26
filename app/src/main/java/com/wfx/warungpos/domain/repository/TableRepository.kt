package com.wfx.warungpos.domain.repository

import com.wfx.warungpos.domain.model.Table
import kotlinx.coroutines.flow.Flow

interface TableRepository {
    fun observeActiveTables(): Flow<List<Table>>
    fun observeAllTables(): Flow<List<Table>>
    suspend fun getTable(id: String): Table?
    suspend fun saveTable(table: Table)
    suspend fun deleteTable(id: String)
}
