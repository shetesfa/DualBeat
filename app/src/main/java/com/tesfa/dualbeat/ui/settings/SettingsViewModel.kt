package com.tesfa.dualbeat.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tesfa.dualbeat.DualBeatApp
import com.tesfa.dualbeat.core.theme.AccentChoice
import com.tesfa.dualbeat.core.theme.ThemeMode
import com.tesfa.dualbeat.data.preferences.UserPreferences
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as DualBeatApp
    private val prefsRepo = app.preferencesRepository

    val userPreferences: StateFlow<UserPreferences> = prefsRepo.userPreferencesFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = UserPreferences()
    )

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { prefsRepo.setThemeMode(mode) }
    }

    fun setAccentChoice(accent: AccentChoice) {
        viewModelScope.launch { prefsRepo.setAccentChoice(accent) }
    }

    fun setResumePlayback(enabled: Boolean) {
        viewModelScope.launch { prefsRepo.setResumePlayback(enabled) }
    }

    fun setAutoPlay(enabled: Boolean) {
        viewModelScope.launch { prefsRepo.setAutoPlay(enabled) }
    }

    fun setBackgroundAudio(enabled: Boolean) {
        viewModelScope.launch { prefsRepo.setBackgroundAudio(enabled) }
    }

    fun setGesturesEnabled(enabled: Boolean) {
        viewModelScope.launch { prefsRepo.setGesturesEnabled(enabled) }
    }
}
