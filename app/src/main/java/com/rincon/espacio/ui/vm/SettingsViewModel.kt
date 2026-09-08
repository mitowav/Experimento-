package com.rincon.espacio.ui.vm

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rincon.espacio.core.design.ThemeMode
import com.rincon.espacio.core.design.ThemePalette
import com.rincon.espacio.core.feedback.SoundIntensity
import com.rincon.espacio.data.prefs.AppSettings
import com.rincon.espacio.data.prefs.PreferencesRepository
import com.rincon.espacio.data.prefs.UiDensity
import com.rincon.espacio.data.repo.BackupRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import com.rincon.espacio.notifications.ReminderTone
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val preferences: PreferencesRepository,
    private val backup: BackupRepository,
) : ViewModel() {

    /** Último mensaje de copia de seguridad, para contarlo en pantalla. */
    private val _backupMessage = MutableStateFlow<String?>(null)
    val backupMessage = _backupMessage.asStateFlow()

    fun clearBackupMessage() { _backupMessage.value = null }

    fun exportTo(resolver: ContentResolver, uri: Uri) = viewModelScope.launch {
        val result = runCatching {
            withContext(Dispatchers.IO) {
                val json = backup.export()
                resolver.openOutputStream(uri)?.use { it.write(json.toByteArray()) }
                    ?: error("No se pudo escribir el archivo")
            }
        }
        _backupMessage.value = result.fold(
            onSuccess = { "Copia guardada." },
            onFailure = { "No se pudo guardar la copia." },
        )
    }

    fun importFrom(resolver: ContentResolver, uri: Uri) = viewModelScope.launch {
        val result = runCatching {
            withContext(Dispatchers.IO) {
                val json = resolver.openInputStream(uri)?.use { it.readBytes().decodeToString() }
                    ?: error("No se pudo leer el archivo")
                backup.import(json)
            }
        }
        _backupMessage.value = result.fold(
            onSuccess = { "Restaurados $it elementos." },
            onFailure = { "Ese archivo no parece una copia de Rincón." },
        )
    }

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
    fun setReminderTone(value: ReminderTone) = viewModelScope.launch { preferences.setReminderTone(value) }
    fun finishOnboarding() = viewModelScope.launch { preferences.setOnboardingDone(true) }
}
