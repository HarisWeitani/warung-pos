package com.wfx.warungpos.feature.sync

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wfx.warungpos.data.remote.sync.SyncCoordinator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

enum class SyncBarState { HIDDEN, SYNCING, OFFLINE }

@HiltViewModel
class SyncViewModel @Inject constructor(
    syncCoordinator: SyncCoordinator,
) : ViewModel() {

    val state: StateFlow<SyncBarState> = combine(
        syncCoordinator.isOnline,
        syncCoordinator.isSyncing,
    ) { online, syncing ->
        when {
            !online -> SyncBarState.OFFLINE
            syncing -> SyncBarState.SYNCING
            else -> SyncBarState.HIDDEN
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SyncBarState.HIDDEN)
}
