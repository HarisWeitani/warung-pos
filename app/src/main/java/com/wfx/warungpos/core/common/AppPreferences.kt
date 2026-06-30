package com.wfx.warungpos.core.common

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppPreferences @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val prefs by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "app_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    private val _language by lazy {
        MutableStateFlow(prefs.getString(KEY_LANGUAGE, DEFAULT_LANGUAGE) ?: DEFAULT_LANGUAGE)
    }
    val language: StateFlow<String> by lazy { _language.asStateFlow() }

    fun getLanguage(): String = _language.value

    fun setLanguage(code: String) {
        prefs.edit().putString(KEY_LANGUAGE, code).apply()
        _language.value = code
    }

    companion object {
        private const val KEY_LANGUAGE = "language_code"
        private const val DEFAULT_LANGUAGE = "id"
    }
}
