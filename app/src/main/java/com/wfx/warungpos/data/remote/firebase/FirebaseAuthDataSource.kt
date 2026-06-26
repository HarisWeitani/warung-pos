package com.wfx.warungpos.data.remote.firebase

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.wfx.warungpos.core.common.UserRole
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseAuthDataSource @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
) {
    suspend fun signIn(email: String, password: String): Result<FirebaseUser> = runCatching {
        firebaseAuth.signInWithEmailAndPassword(email, password).await().user!!
    }

    fun signOut() = firebaseAuth.signOut()

    fun observeAuthState(): Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { auth -> trySend(auth.currentUser) }
        firebaseAuth.addAuthStateListener(listener)
        awaitClose { firebaseAuth.removeAuthStateListener(listener) }
    }

    suspend fun getUserRole(): UserRole {
        val user = firebaseAuth.currentUser ?: return UserRole.NONE
        val token = runCatching { user.getIdToken(true).await() }.getOrNull() ?: return UserRole.NONE
        return when (token.claims["role"] as? String) {
            "owner" -> UserRole.OWNER
            "staff" -> UserRole.STAFF
            else -> UserRole.NONE
        }
    }
}
