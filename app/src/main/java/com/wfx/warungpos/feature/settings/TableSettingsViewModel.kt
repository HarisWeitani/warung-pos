package com.wfx.warungpos.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wfx.warungpos.core.common.SessionManager
import com.wfx.warungpos.core.common.SyncStatus
import com.wfx.warungpos.core.util.DateUtil
import com.wfx.warungpos.core.util.UuidGenerator
import com.wfx.warungpos.domain.model.Table
import com.wfx.warungpos.domain.repository.TableRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TableSettingsViewModel @Inject constructor(
    private val tableRepository: TableRepository,
    private val sessionManager: SessionManager,
) : ViewModel() {

    val tables: StateFlow<List<Table>> = tableRepository.observeAllTables()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun addTable(label: String) {
        if (label.isBlank()) return
        viewModelScope.launch {
            val now = DateUtil.nowEpochMs()
            tableRepository.saveTable(
                Table(
                    id = UuidGenerator.generate(),
                    label = label,
                    isActive = true,
                    updatedAt = now,
                    syncStatus = SyncStatus.PENDING,
                    deviceId = sessionManager.deviceId,
                )
            )
        }
    }

    fun toggleActive(table: Table) {
        viewModelScope.launch {
            tableRepository.saveTable(table.copy(isActive = !table.isActive, updatedAt = DateUtil.nowEpochMs()))
        }
    }
}
