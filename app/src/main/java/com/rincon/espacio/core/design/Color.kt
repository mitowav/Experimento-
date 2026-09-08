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

/** Variante "océano": azules profundos con arena cálida. */
private val OceanLight = CreamLight.copy(
    background = Color(0xFFF2F5F6),
    backgroundTop = Color(0xFFE8EFF2),
    surface = Color(0xFFFBFDFD),
    surfaceSunken = Color(0xFFE2EAEE),
    outline = Color(0xFFC9D8DE),
    outlineSoft = Color(0xFFDFE8EC),
    textPrimary = Color(0xFF2A343A),
    textSecondary = Color(0xFF5C6B73),
    accent = Color(0xFF3F7F92),
    accentSoft = Color(0xFFD3E6EC),
    shadow = Color(0xFF3C5560),
)

private val OceanDark = CreamDark.copy(
    background = Color(0xFF12181B),
    backgroundTop = Color(0xFF171F23),
    surface = Color(0xFF1D262A),
    surfaceRaised = Color(0xFF242F34),
    surfaceSunken = Color(0xFF0E1315),
    outline = Color(0xFF33434A),
    outlineSoft = Color(0xFF263338),
    accent = Color(0xFF7FBACD),
    accentInk = Color(0xFF0E1E24),
    accentSoft = Color(0xFF27383F),
)

/** Variante "ciruela": morados profundos y un rosa cálido. */
private val PlumLight = CreamLight.copy(
    background = Color(0xFFF7F3F8),
    backgroundTop = Color(0xFFF1EAF4),
    surface = Color(0xFFFDFBFE),
    surfaceSunken = Color(0xFFEAE1EE),
    outline = Color(0xFFD8CAE0),
    outlineSoft = Color(0xFFE8DEED),
    textPrimary = Color(0xFF332B3B),
    textSecondary = Color(0xFF635A6C),
    accent = Color(0xFF7B5A9E),
    accentSoft = Color(0xFFE6DAF1),
    shadow = Color(0xFF4E3F5C),
)

private val PlumDark = CreamDark.copy(
    background = Color(0xFF17131C),
    backgroundTop = Color(0xFF1D1823),
    surface = Color(0xFF221C29),
    surfaceRaised = Color(0xFF2A2333),
    surfaceSunken = Color(0xFF110E15),
    outline = Color(0xFF3E3449),
    outlineSoft = Color(0xFF2E2737),
    accent = Color(0xFFB799DA),
    accentInk = Color(0xFF1B1223),
    accentSoft = Color(0xFF382D46),
)

/** Variante "arena": tostados claros, muy luminosa. */
private val SandLight = CreamLight.copy(
    background = Color(0xFFF8F4EC),
    backgroundTop = Color(0xFFF2EADC),
    surface = Color(0xFFFFFDF9),
    surfaceSunken = Color(0xFFEDE3D2),
    outline = Color(0xFFDBCDB6),
    outlineSoft = Color(0xFFE9DFCD),
    textPrimary = Color(0xFF3B3527),
    textSecondary = Color(0xFF6C6353),
    accent = Color(0xFFA8813C),
    accentSoft = Color(0xFFF0E2C4),
    shadow = Color(0xFF6B5A38),
)

private val SandDark = CreamDark.copy(
    background = Color(0xFF191712),
    backgroundTop = Color(0xFF201D17),
    surface = Color(0xFF262218),
    surfaceRaised = Color(0xFF2E291E),
    surfaceSunken = Color(0xFF121009),
    outline = Color(0xFF453D2C),
    outlineSoft = Color(0xFF332E22),
    accent = Color(0xFFDCB86C),
    accentInk = Color(0xFF241C0C),
    accentSoft = Color(0xFF3F3623),
)

/** Temas de color disponibles en Ajustes. */
enum class ThemePalette(val label: String) {
    Cream("Crema"),
    Sand("Arena"),
    Forest("Bosque"),
    Ocean("Océano"),
    Dusk("Atardecer"),
    Plum("Ciruela");

    fun colors(dark: Boolean): RinconColors = when (this) {
        Cream -> if (dark) CreamDark else CreamLight
        Sand -> if (dark) SandDark else SandLight
        Forest -> if (dark) ForestDark else ForestLight
        Ocean -> if (dark) OceanDark else OceanLight
        Dusk -> if (dark) DuskDark else DuskLight
        Plum -> if (dark) PlumDark else PlumLight
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
        Color(0xFF6B5726), Color(0xFF836C31), Color(0xFFFBF1D2)),
    Sky("Cielo", Color(0xFFC5DDEE), Color(0xFF9FC4DC), Color(0xFF1F3B4C),
        Color(0xFF335063), Color(0xFF426477), Color(0xFFE1F0FA)),
    Sage("Salvia", Color(0xFFCBDCBB), Color(0xFFA9C294), Color(0xFF2C3E22),
        Color(0xFF3C4F2E), Color(0xFF4C633B), Color(0xFFE4F0DA)),
    Rose("Rosa", Color(0xFFF6CFD7), Color(0xFFE0AEB9), Color(0xFF4C2530),
        Color(0xFF663A45), Color(0xFF7A4854), Color(0xFFFBE6EB)),
    Lavender("Lavanda", Color(0xFFDCD2EF), Color(0xFFBCAFD6), Color(0xFF352C4C),
        Color(0xFF493E64), Color(0xFF594D77), Color(0xFFEDE6F9)),
    Peach("Melocotón", Color(0xFFF9D7BC), Color(0xFFE5B995), Color(0xFF4E3220),
        Color(0xFF6B4630), Color(0xFF80553B), Color(0xFFFCEBDD)),
    Cream("Crema", Color(0xFFF3E7D3), Color(0xFFDCCBAF), Color(0xFF43382A),
        Color(0xFF4A4136), Color(0xFF5C5142), Color(0xFFF4EDE0)),
    Terracotta("Terracota", Color(0xFFEBB093), Color(0xFFD08F6E), Color(0xFF4A2517),
        Color(0xFF6E402B), Color(0xFF854E35), Color(0xFFFAE4D8)),
    Mint("Menta", Color(0xFFC2E2DA), Color(0xFF9AC7BC), Color(0xFF1F3F38),
        Color(0xFF2E4A44), Color(0xFF3B5D55), Color(0xFFD9EFE9)),
    Coral("Coral", Color(0xFFF8C4B8), Color(0xFFE29E8E), Color(0xFF52281F),
        Color(0xFF6B3B31), Color(0xFF824A3E), Color(0xFFFBDDD5)),
    Slate("Pizarra", Color(0xFFCFD6DE), Color(0xFFAAB4C0), Color(0xFF262F39),
        Color(0xFF39414B), Color(0xFF4A535F), Color(0xFFE3E9EF)),
    Sand("Arena", Color(0xFFEBDCC2), Color(0xFFD2BE9C), Color(0xFF463A25),
        Color(0xFF544733), Color(0xFF685941), Color(0xFFF3E9D9));

    fun paper(dark: Boolean): Color = if (dark) darkPaper else lightPaper
    fun edge(dark: Boolean): Color = if (dark) darkEdge else lightEdge
    fun ink(dark: Boolean): Color = if (dark) darkInk else lightInk

    companion object {
        fun fromKey(key: String?): PaperColor = entries.firstOrNull { it.name == key } ?: Butter
    }
}


/**
 * Estilo del papel.
 *
 * No es decoración gratuita: cada estilo cambia cómo se lee la nota. El rayado
 * invita a escribir frases, la cuadrícula a listas, el post-it a recordatorios
 * cortos. Es la misma idea que elegir libreta.
 */
enum class PaperStyle(val label: String) {
    Plain("Liso"),
    Lined("Rayado"),
    Grid("Cuadrícula"),
    Sticky("Post-it"),
    Recycled("Reciclado");

    companion object {
        fun fromKey(key: String?): PaperStyle = entries.firstOrNull { it.name == key } ?: Plain
    }
}
