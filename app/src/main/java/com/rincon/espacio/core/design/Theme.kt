package com.rincon.espacio.core.design

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle

/** Modo de color elegido por la persona usuaria. */
enum class ThemeMode(val label: String) {
    System("Sistema"), Light("Claro"), Dark("Oscuro");

    companion object {
        fun fromKey(key: String?): ThemeMode = entries.firstOrNull { it.name == key } ?: System
    }
}

val LocalRinconColors: ProvidableCompositionLocal<RinconColors> =
    staticCompositionLocalOf { ThemePalette.Cream.colors(false) }
val LocalRinconTypography = staticCompositionLocalOf { RinconTypography() }
val LocalRinconShapes = staticCompositionLocalOf { RinconShapes() }
val LocalRinconMotion = staticCompositionLocalOf { RinconMotion(reduced = false) }

/** Punto de acceso corto: `Rincon.colors`, `Rincon.type`, `Rincon.motion`. */
object Rincon {
    val colors: RinconColors
        @Composable @ReadOnlyComposable get() = LocalRinconColors.current
    val type: RinconTypography
        @Composable @ReadOnlyComposable get() = LocalRinconTypography.current
    val shapes: RinconShapes
        @Composable @ReadOnlyComposable get() = LocalRinconShapes.current
    val motion: RinconMotion
        @Composable @ReadOnlyComposable get() = LocalRinconMotion.current
}

@Composable
fun RinconTheme(
    mode: ThemeMode = ThemeMode.System,
    palette: ThemePalette = ThemePalette.Cream,
    reduceMotion: Boolean = false,
    content: @Composable () -> Unit,
) {
    val dark = when (mode) {
        ThemeMode.System -> isSystemInDarkTheme()
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
    }
    val target = remember(palette, dark) { palette.colors(dark) }
    // Pasar de claro a oscuro de golpe es un fogonazo en la cara. Cada color
    // del tema viaja a su nuevo valor en medio segundo, así que el cambio se
    // vive como si alguien bajara la luz de la habitación.
    val colors = animatedColors(target, reduceMotion)
    val typography = remember { RinconTypography() }
    val shapes = remember { RinconShapes() }
    val motion = remember(reduceMotion) { RinconMotion(reduceMotion) }

    // Material 3 se usa sólo como base técnica (ripple, selección de texto,
    // accesibilidad). El aspecto visual lo define íntegramente RinconColors.
    val materialScheme = remember(colors) {
        if (colors.isDark) {
            darkColorScheme(
                primary = colors.accent,
                onPrimary = colors.accentInk,
                background = colors.background,
                onBackground = colors.textPrimary,
                surface = colors.surface,
                onSurface = colors.textPrimary,
                surfaceVariant = colors.surfaceSunken,
                onSurfaceVariant = colors.textSecondary,
                outline = colors.outline,
                error = colors.danger,
            )
        } else {
            lightColorScheme(
                primary = colors.accent,
                onPrimary = colors.accentInk,
                background = colors.background,
                onBackground = colors.textPrimary,
                surface = colors.surface,
                onSurface = colors.textPrimary,
                surfaceVariant = colors.surfaceSunken,
                onSurfaceVariant = colors.textSecondary,
                outline = colors.outline,
                error = colors.danger,
            )
        }
    }

    CompositionLocalProvider(
        LocalRinconColors provides colors,
        LocalRinconTypography provides typography,
        LocalRinconShapes provides shapes,
        LocalRinconMotion provides motion,
    ) {
        MaterialTheme(
            colorScheme = materialScheme,
            typography = materialTypographyFrom(typography),
            content = content,
        )
    }
}

@Composable
private fun animatedColors(target: RinconColors, reduceMotion: Boolean): RinconColors {
    val duration = if (reduceMotion) 120 else 520
    val spec = remember(duration) { tween<Color>(duration, easing = RinconMotion.StandardEasing) }

    @Composable
    fun fade(value: Color, label: String): Color =
        animateColorAsState(targetValue = value, animationSpec = spec, label = label).value

    return RinconColors(
        background = fade(target.background, "background"),
        backgroundTop = fade(target.backgroundTop, "backgroundTop"),
        surface = fade(target.surface, "surface"),
        surfaceRaised = fade(target.surfaceRaised, "surfaceRaised"),
        surfaceSunken = fade(target.surfaceSunken, "surfaceSunken"),
        outline = fade(target.outline, "outline"),
        outlineSoft = fade(target.outlineSoft, "outlineSoft"),
        textPrimary = fade(target.textPrimary, "textPrimary"),
        textSecondary = fade(target.textSecondary, "textSecondary"),
        textMuted = fade(target.textMuted, "textMuted"),
        accent = fade(target.accent, "accent"),
        accentInk = fade(target.accentInk, "accentInk"),
        accentSoft = fade(target.accentSoft, "accentSoft"),
        success = fade(target.success, "success"),
        successSoft = fade(target.successSoft, "successSoft"),
        warning = fade(target.warning, "warning"),
        danger = fade(target.danger, "danger"),
        shadow = fade(target.shadow, "shadow"),
        // El modo sí cambia de golpe: decide qué tinta usa el papel, y una
        // tinta a medio camino no se leería bien durante la transición.
        isDark = target.isDark,
    )
}

private fun materialTypographyFrom(t: RinconTypography): Typography {
    fun m(style: TextStyle) = style
    return Typography(
        displayLarge = m(t.display),
        headlineLarge = m(t.title),
        headlineMedium = m(t.section),
        titleLarge = m(t.cardTitle),
        bodyLarge = m(t.body),
        bodyMedium = m(t.body),
        labelLarge = m(t.label),
        labelMedium = m(t.caption),
        labelSmall = m(t.caption),
    )
}
