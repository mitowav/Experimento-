package com.rincon.espacio.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.rincon.espacio.core.design.ThemeMode
import com.rincon.espacio.core.design.ThemePalette
import com.rincon.espacio.core.feedback.SoundIntensity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Densidad de la interfaz: cuánto aire dejamos entre elementos. */
enum class UiDensity(val label: String, val scale: Float) {
    Comfortable("Cómoda", 1f),
    Compact("Compacta", 0.82f);

    companion object {
        fun fromKey(key: String?): UiDensity = entries.firstOrNull { it.name == key } ?: Comfortable
    }
}

/**
 * Dónde estabas mirando el escritorio la última vez.
 *
 * No es un ajuste que nadie elija: es memoria. Volver a la app y encontrarla
 * exactamente como la dejaste (mismo zoom, misma zona del tablero, misma
 * pestaña) es la diferencia entre "una app" y "tu sitio".
 */
data class DeskView(
    val scale: Float = 1f,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
    val lastTab: String = "",
)

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.System,
    val palette: ThemePalette = ThemePalette.Cream,
    val reduceMotion: Boolean = false,
    val soundEnabled: Boolean = true,
    val soundIntensity: SoundIntensity = SoundIntensity.Low,
    val hapticsEnabled: Boolean = true,
    val decorations: Boolean = true,
    val density: UiDensity = UiDensity.Comfortable,
    val onboardingDone: Boolean = false,
    val displayName: String = "",
)

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "rincon_settings")

class PreferencesRepository(context: Context) {

    private val store = context.applicationContext.dataStore

    private object Keys {
        val themeMode = stringPreferencesKey("theme_mode")
        val palette = stringPreferencesKey("palette")
        val reduceMotion = booleanPreferencesKey("reduce_motion")
        val soundEnabled = booleanPreferencesKey("sound_enabled")
        val soundIntensity = stringPreferencesKey("sound_intensity")
        val haptics = booleanPreferencesKey("haptics_enabled")
        val decorations = booleanPreferencesKey("decorations")
        val density = stringPreferencesKey("density")
        val onboarding = booleanPreferencesKey("onboarding_done")
        val displayName = stringPreferencesKey("display_name")
        val deskScale = floatPreferencesKey("desk_scale")
        val deskOffsetX = floatPreferencesKey("desk_offset_x")
        val deskOffsetY = floatPreferencesKey("desk_offset_y")
        val lastTab = stringPreferencesKey("last_tab")
    }

    val deskView: Flow<DeskView> = store.data.map { p ->
        DeskView(
            scale = p[Keys.deskScale] ?: 1f,
            offsetX = p[Keys.deskOffsetX] ?: 0f,
            offsetY = p[Keys.deskOffsetY] ?: 0f,
            lastTab = p[Keys.lastTab].orEmpty(),
        )
    }

    suspend fun saveDeskView(scale: Float, offsetX: Float, offsetY: Float) = store.edit {
        it[Keys.deskScale] = scale
        it[Keys.deskOffsetX] = offsetX
        it[Keys.deskOffsetY] = offsetY
    }

    suspend fun setLastTab(route: String) = store.edit { it[Keys.lastTab] = route }

    val settings: Flow<AppSettings> = store.data.map { p ->
        AppSettings(
            themeMode = ThemeMode.fromKey(p[Keys.themeMode]),
            palette = ThemePalette.fromKey(p[Keys.palette]),
            reduceMotion = p[Keys.reduceMotion] ?: false,
            soundEnabled = p[Keys.soundEnabled] ?: true,
            soundIntensity = SoundIntensity.fromKey(p[Keys.soundIntensity]),
            hapticsEnabled = p[Keys.haptics] ?: true,
            decorations = p[Keys.decorations] ?: true,
            density = UiDensity.fromKey(p[Keys.density]),
            onboardingDone = p[Keys.onboarding] ?: false,
            displayName = p[Keys.displayName].orEmpty(),
        )
    }

    suspend fun setThemeMode(mode: ThemeMode) = store.edit { it[Keys.themeMode] = mode.name }
    suspend fun setPalette(palette: ThemePalette) = store.edit { it[Keys.palette] = palette.name }
    suspend fun setReduceMotion(value: Boolean) = store.edit { it[Keys.reduceMotion] = value }
    suspend fun setSoundEnabled(value: Boolean) = store.edit { it[Keys.soundEnabled] = value }
    suspend fun setSoundIntensity(value: SoundIntensity) = store.edit { it[Keys.soundIntensity] = value.name }
    suspend fun setHaptics(value: Boolean) = store.edit { it[Keys.haptics] = value }
    suspend fun setDecorations(value: Boolean) = store.edit { it[Keys.decorations] = value }
    suspend fun setDensity(value: UiDensity) = store.edit { it[Keys.density] = value.name }
    suspend fun setOnboardingDone(value: Boolean) = store.edit { it[Keys.onboarding] = value }
    suspend fun setDisplayName(value: String) = store.edit { it[Keys.displayName] = value.trim() }
}
