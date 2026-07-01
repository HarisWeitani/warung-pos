package com.wfx.warungpos.data.remote.firebase

import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The app has no user-facing login — identity is a local username + PIN (see SessionManager).
 * Firebase RTDB security rules still require `auth != null`, so we sign in anonymously behind
 * the scenes to keep cross-device sync working. Anonymous sign-in requires the "Anonymous"
 * provider to be enabled in the Firebase Auth console; if it isn't (or the device is offline),
 * this fails silently and the app continues to work locally — writes stay PENDING until sync
 * can push them.
 */
@Singleton
class FirebaseAuthDataSource @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
) {
    suspend fun ensureSignedIn(): Result<Unit> = runCatching {
        if (firebaseAuth.currentUser == null) {
            firebaseAuth.signInAnonymously().await()
        }
        Unit
    }
}
