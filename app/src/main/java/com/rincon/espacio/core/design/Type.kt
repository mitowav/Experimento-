package com.rincon.espacio.core.design

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.sp
import com.rincon.espacio.R

/**
 * Nunito (variable) es una grotesca redondeada: cálida, muy legible y con
 * personalidad sin resultar infantil. Se empaqueta en la app para no depender
 * de servicios de fuentes en tiempo de ejecución.
 */
val NunitoFamily = FontFamily(
    Font(R.font.nunito_variable, FontWeight.Normal),
    Font(R.font.nunito_variable, FontWeight.Medium),
    Font(R.font.nunito_variable, FontWeight.SemiBold),
    Font(R.font.nunito_variable, FontWeight.Bold),
    Font(R.font.nunito_variable, FontWeight.ExtraBold),
)

private val trim = LineHeightStyle(
    alignment = LineHeightStyle.Alignment.Center,
    trim = LineHeightStyle.Trim.None,
)

/**
 * Sombra de texto muy corta y difusa.
 *
 * No es un efecto "de sombra": a esta distancia y opacidad el ojo no la lee
 * como sombra sino como grosor, igual que una letra impresa en papel grueso.
 * Sólo se aplica a los títulos grandes, donde hay superficie suficiente para
 * que aporte cuerpo sin ensuciar la lectura.
 */
private val letterpress = Shadow(
    color = Color(0x33000000),
    offset = Offset(0f, 1.5f),
    blurRadius = 4f,
)

private fun style(
    size: Int,
    lineHeight: Int,
    weight: FontWeight,
    letterSpacing: Double = 0.0,
    depth: Boolean = false,
) = TextStyle(
    fontFamily = NunitoFamily,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    letterSpacing = letterSpacing.sp,
    lineHeightStyle = trim,
    shadow = if (depth) letterpress else null,
)

/** Escala tipográfica con jerarquía marcada y tamaños cómodos en móvil. */
@Immutable
data class RinconTypography(
    val display: TextStyle = style(36, 43, FontWeight.ExtraBold, (-0.6), depth = true),
    val title: TextStyle = style(27, 33, FontWeight.Bold, (-0.3), depth = true),
    val section: TextStyle = style(20, 26, FontWeight.Bold, (-0.1), depth = true),
    val cardTitle: TextStyle = style(18, 24, FontWeight.SemiBold),
    val body: TextStyle = style(16, 23, FontWeight.Medium),
    val bodyStrong: TextStyle = style(16, 23, FontWeight.SemiBold),
    val label: TextStyle = style(14, 19, FontWeight.SemiBold, 0.1),
    val caption: TextStyle = style(13, 18, FontWeight.Medium, 0.2),
    val note: TextStyle = style(17, 24, FontWeight.SemiBold),
    val numeral: TextStyle = style(32, 36, FontWeight.ExtraBold, (-0.8), depth = true),
    /** Etiquetas de barras y controles: no deben crecer hasta romper el diseño. */
    val navLabel: TextStyle = style(11, 14, FontWeight.SemiBold, 0.1),
)
