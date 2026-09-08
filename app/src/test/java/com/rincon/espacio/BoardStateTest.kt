package com.rincon.espacio

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import com.rincon.espacio.ui.components.Board
import com.rincon.espacio.ui.components.BoardState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * La ventana sobre el tablero. Si esta aritmética falla, el zoom "salta" y
 * arrastrar deja de caer donde apunta el dedo, así que conviene fijarla.
 */
class BoardStateTest {

    private fun board(): BoardState {
        val state = BoardState(1f, 0f, 0f)
        state.viewport = Size(1000f, 2000f)
        state.boardPx = Size(3000f, 5000f)
        return state
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
    fun `no se puede pasear fuera del tablero`() {
        val state = board()
        state.applyTransform(Offset(500f, 1000f), Offset(9000f, 9000f), 1f)
        assertTrue("el borde izquierdo no debe entrar en pantalla", state.offset.x <= 0.01f)
        assertTrue("el borde superior no debe entrar en pantalla", state.offset.y <= 0.01f)

        state.applyTransform(Offset(500f, 1000f), Offset(-99000f, -99000f), 1f)
        assertTrue(state.offset.x >= state.viewport.width - state.boardPx.width * state.scale - 0.01f)
        assertTrue(state.offset.y >= state.viewport.height - state.boardPx.height * state.scale - 0.01f)
    }

    @Test
    fun `si el tablero cabe entero queda centrado`() {
        val state = board()
        repeat(40) { state.applyTransform(Offset(500f, 1000f), Offset.Zero, 0.5f) }
        val scaledWidth = state.boardPx.width * state.scale
        if (scaledWidth < state.viewport.width) {
            assertEquals((state.viewport.width - scaledWidth) / 2f, state.offset.x, 0.5f)
        }
    }

    @Test
    fun `el centro visible se expresa en dp del tablero`() {
        val state = board()
        val density = androidx.compose.ui.unit.Density(2f)
        val center = state.visibleCenterDp(density)
        // Con escala 1 y sin desplazamiento, el centro de una ventana de
        // 1000x2000 px a densidad 2 son 250x500 dp.
        assertEquals(250f, center.x, 0.5f)
        assertEquals(500f, center.y, 0.5f)
    }
}
