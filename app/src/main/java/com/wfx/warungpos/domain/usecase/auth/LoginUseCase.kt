package com.wfx.warungpos.domain.usecase.auth

import com.wfx.warungpos.core.common.SessionManager
import com.wfx.warungpos.data.remote.firebase.FirebaseAuthDataSource
import javax.inject.Inject

class LoginUseCase @Inject constructor(
    private val authDataSource: FirebaseAuthDataSource,
    private val sessionManager: SessionManager,
) {
    suspend operator fun invoke(email: String, password: String): Result<Unit> =
        authDataSource.signIn(email.trim(), password)
            .map { sessionManager.refreshRole() }
}
