package com.wfx.warungpos.domain.usecase.auth

import com.google.firebase.auth.FirebaseUser
import com.wfx.warungpos.core.common.SessionManager
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

class GetCurrentUserUseCase @Inject constructor(private val sessionManager: SessionManager) {
    operator fun invoke(): StateFlow<FirebaseUser?> = sessionManager.currentUser
}
