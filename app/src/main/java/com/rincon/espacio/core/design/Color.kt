package com.rincon.espacio.core.design

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * Paleta de Rincón.
 *
 * La identidad es cálida: nada de blanco puro ni negro puro. El modo claro
 * parte de un crema apagado y el oscuro de un marrón muy profundo, de forma
 * que la sensación "cozy" se mantiene en ambos.
 */
@Immutable
data class RinconColors(
    val background: Color,
    val backgroundTop: Color,
    val surface: Color,
    val surfaceRaised: Color,
    val surfaceSunken: Color,
    val outline: Color,
    val outlineSoft: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val accent: Color,
    val accentInk: Color,
    val accentSoft: Color,
    val success: Color,
    val successSoft: Color,
    val warning: Color,
    val danger: Color,
    val shadow: Color,
    val isDark: Boolean,
)

private val CreamLight = RinconColors(
    background = Color(0xFFFBF6EE),
    backgroundTop = Color(0xFFF6EDE0),
    surface = Color(0xFFFFFDF8),
    surfaceRaised = Color(0xFFFFFFFF),
    surfaceSunken = Color(0xFFF2E8DA),
    outline = Color(0xFFE3D4BF),
    outlineSoft = Color(0xFFEFE5D6),
    textPrimary = Color(0xFF3A322B),
    textSecondary = Color(0xFF6E6156),
    textMuted = Color(0xFF9A8B7C),
    accent = Color(0xFFC96F45),
    accentInk = Color(0xFFFFFFFF),
    accentSoft = Color(0xFFF7DECF),
    success = Color(0xFF5E7F51),
    successSoft = Color(0xFFDCE8D2),
    warning = Color(0xFFC98A2E),
    danger = Color(0xFFB55545),
    shadow = Color(0xFF7A5B3C),
    isDark = false,
)

private val CreamDark = RinconColors(
    background = Color(0xFF1B1714),
    backgroundTop = Color(0xFF221C18),
    surface = Color(0xFF272119),
    surfaceRaised = Color(0xFF2F2820),
    surfaceSunken = Color(0xFF151210),
    outline = Color(0xFF443A30),
    outlineSoft = Color(0xFF352D26),
    textPrimary = Color(0xFFF2E8DA),
    textSecondary = Color(0xFFC3B4A3),
    textMuted = Color(0xFF9A8A79),
    accent = Color(0xFFE0906A),
    accentInk = Color(0xFF2A1B12),
    accentSoft = Color(0xFF453026),
    success = Color(0xFF9CBB8B),
    successSoft = Color(0xFF32402B),
    warning = Color(0xFFE0B36A),
    danger = Color(0xFFDD8874),
    shadow = Color(0xFF000000),
    isDark = true,
)

/** Variante "bosque": verdes salvia y madera. */
private val ForestLight = CreamLight.copy(
    background = Color(0xFFF6F3EA),
    backgroundTop = Color(0xFFEDF0E3),
    surface = Color(0xFFFDFCF6),
    surfaceSunken = Color(0xFFE8EBDC),
    outline = Color(0xFFD5DAC3),
    outlineSoft = Color(0xFFE6E9D8),
    textPrimary = Color(0xFF2F362C),
    textSecondary = Color(0xFF616B5B),
    accent = Color(0xFF6B8A5A),
    accentSoft = Color(0xFFDCE8D0),
    shadow = Color(0xFF4C5B41),
)

private val ForestDark = CreamDark.copy(
    background = Color(0xFF151914),
    backgroundTop = Color(0xFF1B211A),
    surface = Color(0xFF20261F),
    surfaceRaised = Color(0xFF272E25),
    surfaceSunken = Color(0xFF101410),
    outline = Color(0xFF3A452F),
    outlineSoft = Color(0xFF2C3427),
    accent = Color(0xFF9CC084),
    accentInk = Color(0xFF16200F),
    accentSoft = Color(0xFF2F3B29),
)

/** Variante "atardecer": rosas y lavandas. */
private val DuskLight = CreamLight.copy(
    background = Color(0xFFFBF3F1),
    backgroundTop = Color(0xFFF6ECF2),
    surface = Color(0xFFFFFBFA),
    surfaceSunken = Color(0xFFF3E5E6),
    outline = Color(0xFFE8D2D5),
    outlineSoft = Color(0xFFF2E2E4),
    textPrimary = Color(0xFF3B2F33),
    textSecondary = Color(0xFF6F5F65),
    accent = Color(0xFFB4658A),
    accentSoft = Color(0xFFF6DCE7),
    shadow = Color(0xFF6E4A57),
)

private val DuskDark = CreamDark.copy(
    background = Color(0xFF1A1518),
    backgroundTop = Color(0xFF201A1F),
    surface = Color(0xFF251E23),
    surfaceRaised = Color(0xFF2D242A),
    surfaceSunken = Color(0xFF130F12),
    outline = Color(0xFF453741),
    outlineSoft = Color(0xFF342A31),
    accent = Color(0xFFDD93B4),
    accentInk = Color(0xFF2A1620),
    accentSoft = Color(0xFF432E39),
)

/** Temas de color disponibles en Ajustes. */
enum class ThemePalette(val label: String) {
    Cream("Crema"),
    Forest("Bosque"),
    Dusk("Atardecer");

    fun colors(dark: Boolean): RinconColors = when (this) {
        Cream -> if (dark) CreamDark else CreamLight
        Forest -> if (dark) ForestDark else ForestLight
        Dusk -> if (dark) DuskDark else DuskLight
    }

    companion object {
        fun fromKey(key: String?): ThemePalette =
            entries.firstOrNull { it.name == key } ?: Cream
    }
}

/**
 * Colores de papel para notas, tareas y categorías.
 * Cada uno guarda su tinta para asegurar contraste suficiente en ambos modos.
 */
enum class PaperColor(
    val label: String,
    private val lightPaper: Color,
    private val lightEdge: Color,
    private val lightInk: Color,
    private val darkPaper: Color,
    private val darkEdge: Color,
    private val darkInk: Color,
) {
    Butter("Mantequilla", Color(0xFFF9E6A8), Color(0xFFE7CD82), Color(0xFF4A3C15),
        Color(0xFF5C4B1E), Color(0xFF6E5A26), Color(0xFFF7EAC4)),
    Sky("Cielo", Color(0xFFC5DDEE), Color(0xFF9FC4DC), Color(0xFF1F3B4C),
        Color(0xFF2B4353), Color(0xFF365263), Color(0xFFD8EAF6)),
    Sage("Salvia", Color(0xFFCBDCBB), Color(0xFFA9C294), Color(0xFF2C3E22),
        Color(0xFF334227), Color(0xFF3F5131), Color(0xFFDDEBD0)),
    Rose("Rosa", Color(0xFFF6CFD7), Color(0xFFE0AEB9), Color(0xFF4C2530),
        Color(0xFF56303A), Color(0xFF663B46), Color(0xFFF8DEE4)),
    Lavender("Lavanda", Color(0xFFDCD2EF), Color(0xFFBCAFD6), Color(0xFF352C4C),
        Color(0xFF3D3454), Color(0xFF4A4064), Color(0xFFE6DEF5)),
    Peach("Melocotón", Color(0xFFF9D7BC), Color(0xFFE5B995), Color(0xFF4E3220),
        Color(0xFF5A3B27), Color(0xFF6B4831), Color(0xFFF9E3D2)),
    Cream("Crema", Color(0xFFF3E7D3), Color(0xFFDCCBAF), Color(0xFF43382A),
        Color(0xFF3E362C), Color(0xFF4C4235), Color(0xFFF0E6D6)),
    Terracotta("Terracota", Color(0xFFEBB093), Color(0xFFD08F6E), Color(0xFF4A2517),
        Color(0xFF5D3524), Color(0xFF6E412D), Color(0xFFF7DACB));

    fun paper(dark: Boolean): Color = if (dark) darkPaper else lightPaper
    fun edge(dark: Boolean): Color = if (dark) darkEdge else lightEdge
    fun ink(dark: Boolean): Color = if (dark) darkInk else lightInk

    companion object {
        fun fromKey(key: String?): PaperColor = entries.firstOrNull { it.name == key } ?: Butter
    }
}
