package com.wfx.warungpos.feature.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wfx.warungpos.core.common.AppPreferences
import com.wfx.warungpos.core.util.applyAppLocale
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class LanguageSettingsViewModel @Inject constructor(
    private val appPreferences: AppPreferences,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    val language: StateFlow<String> = appPreferences.language
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), appPreferences.getLanguage())

    fun setLanguage(code: String) {
        appPreferences.setLanguage(code)
        // DEFECT-015: this is what actually makes the switch visible immediately, instead of
        // only ever being read back from storage on some future cold start. See LocaleHelper.kt.
        applyAppLocale(context, code)
    }
}
