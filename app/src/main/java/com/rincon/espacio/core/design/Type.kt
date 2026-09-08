package com.rincon.espacio.core.design

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
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

private fun style(
    size: Int,
    lineHeight: Int,
    weight: FontWeight,
    letterSpacing: Double = 0.0,
) = TextStyle(
    fontFamily = NunitoFamily,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    letterSpacing = letterSpacing.sp,
    lineHeightStyle = trim,
)

/** Escala tipográfica con jerarquía marcada y tamaños cómodos en móvil. */
@Immutable
data class RinconTypography(
    val display: TextStyle = style(38, 44, FontWeight.ExtraBold, (-0.6)),
    val title: TextStyle = style(28, 34, FontWeight.Bold, (-0.3)),
    val section: TextStyle = style(21, 27, FontWeight.Bold, (-0.1)),
    val cardTitle: TextStyle = style(18, 24, FontWeight.SemiBold),
    val body: TextStyle = style(16, 23, FontWeight.Medium),
    val bodyStrong: TextStyle = style(16, 23, FontWeight.SemiBold),
    val label: TextStyle = style(14, 19, FontWeight.SemiBold, 0.1),
    val caption: TextStyle = style(13, 18, FontWeight.Medium, 0.2),
    val note: TextStyle = style(17, 24, FontWeight.SemiBold),
    val numeral: TextStyle = style(34, 38, FontWeight.ExtraBold, (-0.8)),
)
