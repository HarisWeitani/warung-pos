package com.wfx.warungpos.core.common

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.wfx.warungpos.core.util.UuidGenerator
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val firebaseAuth: FirebaseAuth,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val prefs by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "session_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    val deviceId: String by lazy {
        prefs.getString(KEY_DEVICE_ID, null) ?: UuidGenerator.generate().also { newId ->
            prefs.edit().putString(KEY_DEVICE_ID, newId).apply()
        }
    }

    private val _currentUser = MutableStateFlow<FirebaseUser?>(firebaseAuth.currentUser)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()

    private val _userRole = MutableStateFlow(UserRole.NONE)
    val userRole: StateFlow<UserRole> = _userRole.asStateFlow()

    init {
        // AuthStateListener runs on the main thread; role refresh is dispatched to Default
        firebaseAuth.addAuthStateListener { auth ->
            _currentUser.value = auth.currentUser
            if (auth.currentUser == null) {
                _userRole.value = UserRole.NONE
            } else {
                scope.launch { refreshRole() }
            }
        }
    }

    suspend fun refreshRole() {
        val user = firebaseAuth.currentUser ?: run {
            _userRole.value = UserRole.NONE
            return
        }
        runCatching { user.getIdToken(true).await() }
            .onSuccess { result ->
                _userRole.value = when (result.claims["role"] as? String) {
                    "owner" -> UserRole.OWNER
                    "staff" -> UserRole.STAFF
                    else -> UserRole.NONE
                }
            }
            // Token refresh failed; keep existing role — will retry on next call
    }

    companion object {
        private const val KEY_DEVICE_ID = "device_id"
    }
}
