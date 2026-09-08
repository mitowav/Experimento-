package com.rincon.espacio.ui.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rincon.espacio.core.design.ThemeMode
import com.rincon.espacio.core.design.ThemePalette
import com.rincon.espacio.core.feedback.SoundIntensity
import com.rincon.espacio.data.prefs.AppSettings
import com.rincon.espacio.data.prefs.PreferencesRepository
import com.rincon.espacio.data.prefs.UiDensity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(private val preferences: PreferencesRepository) : ViewModel() {

    val settings: StateFlow<AppSettings> = preferences.settings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AppSettings(),
    )

    fun setThemeMode(mode: ThemeMode) = viewModelScope.launch { preferences.setThemeMode(mode) }
    fun setPalette(palette: ThemePalette) = viewModelScope.launch { preferences.setPalette(palette) }
    fun setReduceMotion(value: Boolean) = viewModelScope.launch { preferences.setReduceMotion(value) }
    fun setSoundEnabled(value: Boolean) = viewModelScope.launch { preferences.setSoundEnabled(value) }
    fun setSoundIntensity(value: SoundIntensity) = viewModelScope.launch { preferences.setSoundIntensity(value) }
    fun setHaptics(value: Boolean) = viewModelScope.launch { preferences.setHaptics(value) }
    fun setDecorations(value: Boolean) = viewModelScope.launch { preferences.setDecorations(value) }
    fun setDensity(value: UiDensity) = viewModelScope.launch { preferences.setDensity(value) }
    fun setDisplayName(value: String) = viewModelScope.launch { preferences.setDisplayName(value) }
    fun finishOnboarding() = viewModelScope.launch { preferences.setOnboardingDone(true) }
}
