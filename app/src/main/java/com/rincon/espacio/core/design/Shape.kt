package com.rincon.espacio.core.design

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.dp

/** Esquinas generosas: la app debe sentirse blanda al tacto. */
@Immutable
data class RinconShapes(
    val chip: RoundedCornerShape = RoundedCornerShape(14.dp),
    val note: RoundedCornerShape = RoundedCornerShape(20.dp),
    val card: RoundedCornerShape = RoundedCornerShape(24.dp),
    val panel: RoundedCornerShape = RoundedCornerShape(30.dp),
    val sheet: RoundedCornerShape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp, bottomStart = 0.dp, bottomEnd = 0.dp),
    val pill: RoundedCornerShape = RoundedCornerShape(50),
)

/** Espaciado en una escala de 4dp para mantener ritmo vertical constante. */
object Space {
    val xxs = 2.dp
    val xs = 4.dp
    val s = 8.dp
    val m = 12.dp
    val l = 16.dp
    val xl = 20.dp
    val xxl = 28.dp
    val xxxl = 40.dp

    /** Altura mínima táctil recomendada. */
    val touch = 48.dp
    /** Margen lateral estándar de pantalla. */
    val screen = 20.dp
}
