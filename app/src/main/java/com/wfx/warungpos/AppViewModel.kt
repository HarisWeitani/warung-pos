package com.wfx.warungpos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.database.FirebaseDatabase
import com.wfx.warungpos.core.common.AppPreferences
import com.wfx.warungpos.core.common.NetworkMonitor
import com.wfx.warungpos.core.common.SessionManager
import com.wfx.warungpos.core.common.UserRole
import com.wfx.warungpos.data.remote.firebase.FirebaseAuthDataSource
import com.wfx.warungpos.data.remote.sync.SyncCoordinator
import com.wfx.warungpos.data.seeding.FirstRunManager
import com.wfx.warungpos.domain.usecase.shift.EnsureDayOpenUseCase
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
    private val networkMonitor: NetworkMonitor,
    private val firstRunManager: FirstRunManager,
    private val syncCoordinator: SyncCoordinator,
    private val authDataSource: FirebaseAuthDataSource,
    private val ensureDayOpenUseCase: EnsureDayOpenUseCase,
    val appPreferences: AppPreferences,
) : ViewModel() {

    val userRole: StateFlow<UserRole> = sessionManager.userRole
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UserRole.OWNER)

    val language: StateFlow<String> = appPreferences.language

    val isUnlocked: StateFlow<Boolean> = sessionManager.isUnlocked
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    private val _versionGateState = MutableStateFlow<VersionGateState>(VersionGateState.Loading)
    val versionGateState: StateFlow<VersionGateState> = _versionGateState.asStateFlow()

    init {
        viewModelScope.launch {
            firstRunManager.ensureSeeded()
            ensureDayOpenUseCase()
        }

        viewModelScope.launch {
            // Anonymous sign-in keeps RTDB sync working without a user-facing login.
            authDataSource.ensureSignedIn()
            _versionGateState.value =
                if (networkMonitor.isOnline.value) checkVersionGate() else VersionGateState.Allowed
            syncCoordinator.start()
        }
    }

    private suspend fun checkVersionGate(): VersionGateState = try {
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
