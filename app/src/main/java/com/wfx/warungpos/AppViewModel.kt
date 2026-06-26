package com.wfx.warungpos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.database.FirebaseDatabase
import com.wfx.warungpos.core.common.NetworkMonitor
import com.wfx.warungpos.core.common.SessionManager
import com.wfx.warungpos.core.common.UserRole
import com.wfx.warungpos.data.seeding.FirstRunManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import javax.inject.Inject

sealed interface VersionGateState {
    data object Loading : VersionGateState
    data object UpdateRequired : VersionGateState
    data object Allowed : VersionGateState
}

@HiltViewModel
class AppViewModel @Inject constructor(
    sessionManager: SessionManager,
    networkMonitor: NetworkMonitor,
    private val firstRunManager: FirstRunManager,
) : ViewModel() {

    val userRole: StateFlow<UserRole> = sessionManager.userRole
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UserRole.NONE)

    private val _versionGateState = MutableStateFlow<VersionGateState>(VersionGateState.Loading)
    val versionGateState: StateFlow<VersionGateState> = _versionGateState.asStateFlow()

    init {
        viewModelScope.launch { firstRunManager.ensureSeeded() }

        if (!networkMonitor.isOnline.value) {
            _versionGateState.value = VersionGateState.Allowed
        } else {
            viewModelScope.launch { checkVersionGate() }
        }
    }

    private suspend fun checkVersionGate() {
        _versionGateState.value = try {
            val snapshot = withTimeout(5_000) {
                FirebaseDatabase.getInstance()
                    .getReference("appConfig/minVersionCode")
                    .get()
                    .await()
            }
            val minVersion = snapshot.getValue(Long::class.java) ?: 1L
            if (BuildConfig.VERSION_CODE < minVersion) VersionGateState.UpdateRequired
            else VersionGateState.Allowed
        } catch (_: Exception) {
            VersionGateState.Allowed
        }
    }
}
