package com.rincon.espacio.core.design

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize

/**
 * Un único lugar donde vive el "carácter" del movimiento de la app.
 *
 * Regla: nada de números mágicos repartidos por los componentes. Si algo se
 * mueve en Rincón, usa uno de estos muelles. Con "reducir movimiento" activo
 * los muelles se vuelven más rígidos y las transiciones más cortas, pero
 * ninguna animación desaparece del todo (el usuario sigue entendiendo qué pasó).
 */
@Immutable
class RinconMotion(val reduced: Boolean) {

    /** Respuesta inmediata: pulsaciones, escalas de botón. */
    fun <T> snappy(): SpringSpec<T> = spring(
        dampingRatio = if (reduced) 1f else 0.62f,
        stiffness = if (reduced) 2200f else 1400f,
    )

    /** Movimiento estándar de tarjetas y contenedores. */
    fun <T> gentle(): SpringSpec<T> = spring(
        dampingRatio = if (reduced) 1f else 0.82f,
        stiffness = if (reduced) 1400f else Spring.StiffnessMediumLow,
    )

    /** Asentamiento con peso: notas al soltarse. */
    fun <T> settle(): SpringSpec<T> = spring(
        dampingRatio = if (reduced) 1f else 0.74f,
        stiffness = if (reduced) 1200f else 320f,
    )

    /** Rebote perceptible, sólo para celebrar (objetivo cumplido). */
    fun <T> playful(): SpringSpec<T> = spring(
        dampingRatio = if (reduced) 1f else 0.45f,
        stiffness = if (reduced) 1600f else 520f,
    )

    fun <T> fade(): FiniteAnimationSpec<T> = tween(if (reduced) 90 else 220, easing = StandardEasing)
    fun <T> quickFade(): FiniteAnimationSpec<T> = tween(if (reduced) 60 else 140, easing = StandardEasing)

    fun offsetSpring(): FiniteAnimationSpec<IntOffset> = spring(
        dampingRatio = if (reduced) 1f else 0.85f,
        stiffness = if (reduced) 1600f else 400f,
        visibilityThreshold = IntOffset(1, 1),
    )

    fun sizeSpring(): FiniteAnimationSpec<IntSize> = spring(
        dampingRatio = if (reduced) 1f else 0.9f,
        stiffness = if (reduced) 1600f else 500f,
        visibilityThreshold = IntSize(1, 1),
    )

    fun dpSpring(): SpringSpec<Dp> = spring(
        dampingRatio = if (reduced) 1f else 0.8f,
        stiffness = if (reduced) 1600f else 600f,
        visibilityThreshold = Dp.VisibilityThreshold,
    )

    /** Entrada de un bloque de interfaz: rápida, con salida suave. */
    fun <T> appear(index: Int = 0): FiniteAnimationSpec<T> = tween(
        durationMillis = if (reduced) 110 else 300,
        delayMillis = if (reduced) 0 else (index * 45).coerceAtMost(320),
        easing = EnterEasing,
    )

    /** Cuánto se permite exagerar un gesto (inclinación, escala al arrastrar). */
    val physicality: Float get() = if (reduced) 0.25f else 1f

    /** Desplazamiento inicial de los bloques al entrar. */
    val appearOffsetDp: Float get() = if (reduced) 4f else 18f

    companion object {
        val StandardEasing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
        val EnterEasing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)
    }
}
