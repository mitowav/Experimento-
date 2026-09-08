package com.rincon.espacio

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import com.rincon.espacio.ui.components.Board
import com.rincon.espacio.ui.components.BoardState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * La ventana sobre el tablero.
 *
 * Si esta aritmética falla, el zoom "salta", arrastrar deja de caer donde
 * apunta el dedo y —lo peor— se puede acabar mirando a un rincón vacío con las
 * notas fuera de la pantalla. Por eso el área útil tiene sus propios tests.
 */
class BoardStateTest {

    private val safeTop = 200f
    private val safeBottom = 300f

    private fun board(): BoardState {
        val state = BoardState(1f, 0f, 0f)
        state.configure(
            viewport = Size(1000f, 2000f),
            board = Size(3000f, 5000f),
            top = safeTop,
            bottom = safeBottom,
        )
        return state
    }

    @Test
    fun `el area util descuenta encabezado y barra`() {
        val state = board()
        assertEquals(safeTop, state.safeRect.top, 0.01f)
        assertEquals(2000f - safeBottom, state.safeRect.bottom, 0.01f)
        assertEquals(1500f, state.safeRect.height, 0.01f)
    }

    @Test
    fun `pantalla y tablero son conversiones inversas`() {
        val state = board()
        state.applyTransform(Offset(500f, 900f), Offset(-40f, 25f), 1.4f)
        val point = Offset(742f, 1310f)
        val back = state.boardToScreen(state.screenToBoard(point))
        assertEquals(point.x, back.x, 0.01f)
        assertEquals(point.y, back.y, 0.01f)
    }

    @Test
    fun `al hacer zoom el punto bajo los dedos no se mueve`() {
        val state = board()
        val focus = Offset(300f, 700f)
        val before = state.screenToBoard(focus)
        state.applyTransform(focus, Offset.Zero, 1.8f)
        val after = state.screenToBoard(focus)
        assertEquals(before.x, after.x, 0.5f)
        assertEquals(before.y, after.y, 0.5f)
    }

    @Test
    fun `la escala se mantiene dentro de los limites`() {
        val state = board()
        repeat(20) { state.applyTransform(Offset(500f, 1000f), Offset.Zero, 2f) }
        assertEquals(Board.MaxScale, state.scale, 0.001f)
        repeat(40) { state.applyTransform(Offset(500f, 1000f), Offset.Zero, 0.5f) }
        assertEquals(Board.MinScale, state.scale, 0.001f)
    }

    @Test
    fun `el tablero nunca deja hueco dentro del area util`() {
        val state = board()
        val safe = state.safeRect

        state.applyTransform(Offset(500f, 1000f), Offset(9000f, 9000f), 1f)
        assertTrue("no debe asomar el borde izquierdo", state.offset.x <= safe.left + 0.01f)
        assertTrue("no debe asomar el borde superior", state.offset.y <= safe.top + 0.01f)

        state.applyTransform(Offset(500f, 1000f), Offset(-99000f, -99000f), 1f)
        val right = state.offset.x + state.board.width * state.scale
        val bottom = state.offset.y + state.board.height * state.scale
        assertTrue("no debe asomar el borde derecho", right >= safe.right - 0.01f)
        assertTrue("no debe asomar el borde inferior", bottom >= safe.bottom - 0.01f)
    }

    @Test
    fun `si el tablero cabe entero queda centrado en el area util`() {
        val state = board()
        repeat(40) { state.applyTransform(Offset(500f, 1000f), Offset.Zero, 0.5f) }
        val safe = state.safeRect
        val scaledHeight = state.board.height * state.scale
        if (scaledHeight < safe.height) {
            assertEquals(safe.top + (safe.height - scaledHeight) / 2f, state.offset.y, 0.5f)
        }
        val scaledWidth = state.board.width * state.scale
        if (scaledWidth < safe.width) {
            assertEquals(safe.left + (safe.width - scaledWidth) / 2f, state.offset.x, 0.5f)
        }
    }

    @Test
    fun `encuadrar unas notas las deja visibles y centradas`() {
        val state = board()
        // Un grupo de notas pequeño y lejos del origen: el caso que dejaba al
        // escritorio mirando a una esquina vacía.
        val notes = Rect(2200f, 3800f, 2600f, 4200f)
        state.fitTo(notes, padding = 20f)

        assertTrue("las notas deben quedar a la vista", state.isVisible(notes))

        val safe = state.safeRect
        val center = state.boardToScreen(notes.center)
        assertEquals(safe.center.x, center.x, 1f)
        assertEquals(safe.center.y, center.y, 1f)
    }

    @Test
    fun `una nota fuera de la ventana se detecta como no visible`() {
        val state = board()
        state.fitTo(Rect(0f, 0f, 400f, 400f), padding = 20f)
        assertTrue(state.isVisible(Rect(0f, 0f, 400f, 400f)))
        assertTrue(!state.isVisible(Rect(2800f, 4800f, 2900f, 4900f)))
    }

    @Test
    fun `el centro visible se expresa en dp del tablero`() {
        val state = BoardState(1f, 0f, 0f)
        state.configure(Size(1000f, 2000f), Size(3000f, 5000f), top = 0f, bottom = 0f)
        val density = androidx.compose.ui.unit.Density(2f)
        val center = state.visibleCenterDp(density)
        // Ventana de 1000x2000 px a densidad 2, sin desplazamiento: 250x500 dp.
        assertEquals(250f, center.x, 0.5f)
        assertEquals(500f, center.y, 0.5f)
    }
}
