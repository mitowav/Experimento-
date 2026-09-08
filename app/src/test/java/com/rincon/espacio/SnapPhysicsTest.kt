package com.rincon.espacio

import androidx.compose.ui.geometry.Offset
import com.rincon.espacio.ui.components.settlePosition
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * El imán del escritorio: debe ayudar sin secuestrar. Estos tests fijan el
 * contrato de "se ajusta si está cerca, y no toca nada si no lo está".
 */
class SnapPhysicsTest {

    private val minX = 0f
    private val maxX = 900f
    // Múltiplo de la rejilla: así el tope inferior coincide con una guía y
    // el test comprueba el límite, no el imán.
    private val maxY = 1440f
    private val grid = 48f
    private val gap = 12f
    private val tolerance = 11f

    private fun settle(x: Float, y: Float, others: List<Pair<Offset, Float>> = emptyList()) =
        settlePosition(
            candidate = Offset(x, y),
            noteWidth = 164f,
            noteHeight = 120f,
            minX = minX,
            maxX = maxX,
            maxY = maxY,
            grid = grid,
            gap = gap,
            tolerance = tolerance,
            others = others,
        )

    @Test
    fun `la nota no se sale del tablero`() {
        assertEquals(minX, settle(-500f, 100f).x, 0.01f)
        assertEquals(maxX, settle(9000f, 100f).x, 0.01f)
        assertEquals(0f, settle(100f, -400f).y, 0.01f)
        assertEquals(maxY, settle(100f, 99999f).y, 0.01f)
    }

    @Test
    fun `se alinea con el canto de otra nota cuando esta cerca`() {
        val other = Offset(120f, 300f) to 140f
        val result = settle(126f, 305f, listOf(other))
        assertEquals(120f, result.x, 0.01f)
        assertEquals(300f, result.y, 0.01f)
    }

    @Test
    fun `se apila justo debajo de otra nota`() {
        val other = Offset(120f, 300f) to 140f
        // 300 + 140 + 12 = 452
        val result = settle(120f, 456f, listOf(other))
        assertEquals(452f, result.y, 0.01f)
    }

    @Test
    fun `fuera de la tolerancia la nota se queda donde la dejas`() {
        val other = Offset(120f, 300f) to 140f
        val result = settle(170f, 400f, listOf(other))
        assertEquals(170f, result.x, 0.01f)
        assertEquals(400f, result.y, 0.01f)
    }

    @Test
    fun `se ajusta a la rejilla solo si esta cerca de una linea`() {
        // 100 esta a 4 de 96 (multiplo de 48): dentro de tolerancia, se ajusta.
        assertEquals(96f, settle(100f, 100f).x, 0.01f)
        // 170 esta a 22 de la linea mas cercana: se queda libre.
        assertEquals(170f, settle(170f, 100f).x, 0.01f)
    }
}
