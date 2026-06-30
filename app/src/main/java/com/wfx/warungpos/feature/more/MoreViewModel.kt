package com.wfx.warungpos.feature.more

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wfx.warungpos.core.common.SessionManager
import com.wfx.warungpos.core.common.UserRole
import com.wfx.warungpos.data.remote.firebase.FirebaseAuthDataSource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class MoreUiState(
    val email: String = "",
    val userRole: UserRole = UserRole.NONE,
)

@HiltViewModel
class MoreViewModel @Inject constructor(
    private val authDataSource: FirebaseAuthDataSource,
    sessionManager: SessionManager,
) : ViewModel() {

    val uiState: StateFlow<MoreUiState> = combine(
        sessionManager.currentUser,
        sessionManager.userRole,
    ) { user, role ->
        MoreUiState(email = user?.email ?: "", userRole = role)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MoreUiState())

    fun signOut() {
        authDataSource.signOut()
    }
}
