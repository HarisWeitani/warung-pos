package com.wfx.warungpos.core.common

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.wfx.warungpos.core.util.UuidGenerator
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Local session for the single-stall app. There is no remote account: identity is a locally
 * chosen username protected by a numeric PIN, both stored in EncryptedSharedPreferences. The
 * app is single-user with full (OWNER) access. Firebase RTDB sync uses a separate anonymous
 * sign-in handled by [com.wfx.warungpos.data.remote.firebase.FirebaseAuthDataSource].
 */
@Singleton
class SessionManager @Inject constructor(
    @ApplicationContext private val context: Context,
) : SessionProvider {

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

    override val deviceId: String by lazy {
        prefs.getString(KEY_DEVICE_ID, null) ?: UuidGenerator.generate().also { newId ->
            prefs.edit().putString(KEY_DEVICE_ID, newId).apply()
        }
    }

    private val _username = MutableStateFlow(prefs.getString(KEY_USERNAME, "").orEmpty())
    val username: StateFlow<String> = _username.asStateFlow()

    // Locked on every cold start; a valid PIN (or first-run registration) unlocks the UI.
    private val _isUnlocked = MutableStateFlow(false)
    val isUnlocked: StateFlow<Boolean> = _isUnlocked.asStateFlow()

    // Single-user app — always OWNER so every screen is accessible.
    private val _userRole = MutableStateFlow(UserRole.OWNER)
    val userRole: StateFlow<UserRole> = _userRole.asStateFlow()

    /** True once a username + PIN have been set up on this device. */
    val isRegistered: Boolean
        get() = prefs.contains(KEY_PIN_HASH) && _username.value.isNotBlank()

    override val currentUserId: String?
        get() = _username.value.ifBlank { null }

    override val currentUserRole: UserRole
        get() = _userRole.value

    /** First-run setup: store the chosen username + PIN and unlock. */
    fun register(username: String, pin: String) {
        val trimmed = username.trim()
        prefs.edit()
            .putString(KEY_USERNAME, trimmed)
            .putString(KEY_PIN_HASH, hash(pin))
            .apply()
        _username.value = trimmed
        _isUnlocked.value = true
    }

    /** Verify the entered PIN against the stored hash; unlocks on success. */
    fun unlock(pin: String): Boolean {
        val ok = prefs.getString(KEY_PIN_HASH, null) == hash(pin)
        if (ok) _isUnlocked.value = true
        return ok
    }

    /** Re-lock the UI (returns to the PIN screen) without clearing the stored username/PIN. */
    fun lock() {
        _isUnlocked.value = false
    }

    private fun hash(pin: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(pin.toByteArray())
            .joinToString("") { "%02x".format(it) }

    companion object {
        private const val KEY_DEVICE_ID = "device_id"
        private const val KEY_USERNAME = "username"
        private const val KEY_PIN_HASH = "pin_hash"
    }
}
