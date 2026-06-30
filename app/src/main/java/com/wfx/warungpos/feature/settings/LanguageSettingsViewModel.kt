package com.wfx.warungpos.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wfx.warungpos.core.common.AppPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class LanguageSettingsViewModel @Inject constructor(
    private val appPreferences: AppPreferences,
) : ViewModel() {

    val language: StateFlow<String> = appPreferences.language
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), appPreferences.getLanguage())

    fun setLanguage(code: String) {
        appPreferences.setLanguage(code)
    }
}
