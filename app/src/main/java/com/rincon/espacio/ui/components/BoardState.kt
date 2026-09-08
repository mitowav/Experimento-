package com.rincon.espacio.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import kotlin.math.min

/**
 * El tablero.
 *
 * Un espacio fijo y generoso —no infinito a propósito: infinito significa
 * perderse— sobre el que la pantalla es una ventana que se mueve y se acerca.
 *
 * El modelo es una sola fórmula: `pantalla = tablero * escala + desplazamiento`.
 *
 * La pieza que faltaba era el **área útil**. La ventana ocupa toda la pantalla,
 * pero arriba hay un encabezado y abajo una barra, así que encuadrar respecto a
 * la pantalla entera dejaba el contenido debajo de ellos. Todo el encuadre se
 * calcula ahora contra [safeRect], y el tablero no puede alejarse hasta dejar
 * esa zona vacía.
 */
object Board {
    val Width = 1000.dp
    val Height = 1500.dp
    const val MinScale = 0.4f
    const val MaxScale = 2.2f
    const val DoubleTapScale = 1.5f
}

class BoardState internal constructor(scale: Float, offsetX: Float, offsetY: Float) {

    var scale by mutableFloatStateOf(scale)
        internal set

    var offset by mutableStateOf(Offset(offsetX, offsetY))
        internal set

    /** Tamaño de la ventana visible, en px. */
    var viewport by mutableStateOf(Size.Zero)
        internal set

    /** Tamaño del tablero, en px. */
    var board by mutableStateOf(Size.Zero)
        internal set

    private var safeTop by mutableFloatStateOf(0f)
    private var safeBottom by mutableFloatStateOf(0f)

    /** Lo que de verdad se ve: sin el encabezado ni la barra inferior. */
    val safeRect: Rect
        get() {
            if (viewport == Size.Zero) return Rect(0f, 0f, 1f, 1f)
            val top = safeTop
            val bottom = (viewport.height - safeBottom).coerceAtLeast(top + 1f)
            return Rect(0f, top, viewport.width, bottom)
        }

    internal fun configure(viewport: Size, board: Size, top: Float, bottom: Float) {
        val changed = this.viewport != viewport || this.board != board ||
            safeTop != top || safeBottom != bottom
        this.viewport = viewport
        this.board = board
        safeTop = top
        safeBottom = bottom
        if (changed) offset = clamp()
    }

    fun boardToScreen(point: Offset): Offset = point * scale + offset

    fun screenToBoard(point: Offset): Offset = (point - offset) / scale

    /**
     * Mantiene el tablero cubriendo el área útil.
     *
     * Si el tablero es más grande que ella, no se puede arrastrar hasta que
     * asome un borde; si es más pequeño, queda centrado en ella. En ningún caso
     * se puede acabar mirando a un vacío fuera del tablero.
     */
    internal fun clamp(candidate: Offset = offset, candidateScale: Float = scale): Offset {
        if (viewport == Size.Zero || board == Size.Zero) return candidate
        val safe = safeRect
        val scaledWidth = board.width * candidateScale
        val scaledHeight = board.height * candidateScale

        val x = if (scaledWidth <= safe.width) {
            safe.left + (safe.width - scaledWidth) / 2f
        } else {
            candidate.x.coerceIn(safe.right - scaledWidth, safe.left)
        }
        val y = if (scaledHeight <= safe.height) {
            safe.top + (safe.height - scaledHeight) / 2f
        } else {
            candidate.y.coerceIn(safe.bottom - scaledHeight, safe.top)
        }
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

    internal fun zoomTo(target: Float, focus: Offset) {
        val newScale = target.coerceIn(Board.MinScale, Board.MaxScale)
        val factor = newScale / scale
        val candidate = focus - (focus - offset) * factor
        scale = newScale
        offset = clamp(candidate, newScale)
    }

    /** Encaja un rectángulo del tablero dentro del área útil. */
    internal fun fitTo(rect: Rect, padding: Float, maxScale: Float = 1f) {
        if (viewport == Size.Zero || rect.width <= 0f || rect.height <= 0f) return
        val safe = safeRect
        val available = Size(
            (safe.width - padding * 2f).coerceAtLeast(1f),
            (safe.height - padding * 2f).coerceAtLeast(1f),
        )
        val target = min(available.width / rect.width, available.height / rect.height)
            .coerceIn(Board.MinScale, maxScale)
        scale = target
        offset = clamp(
            Offset(
                safe.center.x - rect.center.x * target,
                safe.center.y - rect.center.y * target,
            ),
            target,
        )
    }

    /** ¿Se ve algo de este rectángulo del tablero en el área útil? */
    internal fun isVisible(rect: Rect): Boolean {
        val safe = safeRect
        val topLeft = boardToScreen(rect.topLeft)
        val bottomRight = boardToScreen(rect.bottomRight)
        return Rect(topLeft, bottomRight).overlaps(safe)
    }

    /** Centro del área útil, en coordenadas de tablero y en dp. */
    fun visibleCenterDp(density: Density): Offset {
        if (viewport == Size.Zero) return Offset(60f, 60f)
        val center = screenToBoard(safeRect.center)
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
