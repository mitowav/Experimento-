package com.rincon.espacio.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp

/**
 * El tablero.
 *
 * Es un espacio fijo y generoso —no infinito a propósito: infinito significa
 * perderse, y aquí queremos que siempre se pueda volver a encontrar una nota—
 * sobre el que la pantalla es una ventana que se mueve y se acerca.
 *
 * El modelo es deliberadamente simple: `pantalla = tablero * escala + offset`.
 * Todo lo demás (arrastrar notas, imán, papelera, crear en el sitio correcto)
 * se deriva de esa única fórmula.
 */
object Board {
    val Width = 1200.dp
    val Height = 1900.dp
    const val MinScale = 0.35f
    const val MaxScale = 2.2f
    const val DoubleTapScale = 1.6f
}

class BoardState internal constructor(scale: Float, offsetX: Float, offsetY: Float) {

    var scale by mutableFloatStateOf(scale)
        internal set

    var offset by mutableStateOf(Offset(offsetX, offsetY))
        internal set

    /** Tamaño de la ventana visible, en px. Lo rellena el propio canvas. */
    var viewport by mutableStateOf(Size.Zero)
        internal set

    var boardPx by mutableStateOf(Size.Zero)
        internal set

    fun boardToScreen(point: Offset): Offset = point * scale + offset

    fun screenToBoard(point: Offset): Offset = (point - offset) / scale

    /** Coloca el offset dentro de límites razonables para no perder el tablero. */
    internal fun clamp(candidate: Offset = offset, candidateScale: Float = scale): Offset {
        if (viewport == Size.Zero || boardPx == Size.Zero) return candidate
        val scaledW = boardPx.width * candidateScale
        val scaledH = boardPx.height * candidateScale
        val x = if (scaledW <= viewport.width) (viewport.width - scaledW) / 2f
        else candidate.x.coerceIn(viewport.width - scaledW, 0f)
        val y = if (scaledH <= viewport.height) (viewport.height - scaledH) / 2f
        else candidate.y.coerceIn(viewport.height - scaledH, 0f)
        return Offset(x, y)
    }

    internal fun applyTransform(centroid: Offset, pan: Offset, zoom: Float) {
        val newScale = (scale * zoom).coerceIn(Board.MinScale, Board.MaxScale)
        // Mantener fijo el punto que hay bajo los dedos mientras se acerca.
        val factor = newScale / scale
        val candidate = centroid - (centroid - offset) * factor + pan
        scale = newScale
        offset = clamp(candidate, newScale)
    }

    /** Zoom centrado en un punto de la pantalla, para botones y doble toque. */
    internal fun zoomTo(target: Float, focus: Offset) {
        val newScale = target.coerceIn(Board.MinScale, Board.MaxScale)
        val factor = newScale / scale
        val candidate = focus - (focus - offset) * factor
        scale = newScale
        offset = clamp(candidate, newScale)
    }

    /** Centro de lo que se está viendo, en coordenadas de tablero y en dp. */
    fun visibleCenterDp(density: Density): Offset {
        if (viewport == Size.Zero) return Offset(80f, 80f)
        val center = screenToBoard(Offset(viewport.width / 2f, viewport.height / 2f))
        return with(density) { Offset(center.x.toDp().value, center.y.toDp().value) }
    }
}

@Composable
fun rememberBoardState(): BoardState = rememberSaveable(
    saver = listSaver(
        save = { listOf(it.scale, it.offset.x, it.offset.y) },
        restore = { BoardState(it[0], it[1], it[2]) },
    )
) { BoardState(1f, 0f, 0f) }
