package com.rincon.espacio

import com.rincon.espacio.core.util.Dates
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class DatesTest {

    private val tuesday = LocalDate.of(2026, 9, 8)

    @Test
    fun `epochDay ida y vuelta`() {
        assertEquals(tuesday, Dates.fromEpochDay(Dates.epochDay(tuesday)))
    }

    @Test
    fun `segundo del dia ida y vuelta`() {
        val time = LocalTime.of(18, 45)
        assertEquals(time, Dates.fromSecondOfDay(Dates.secondOfDay(time)))
    }

    @Test
    fun `la semana empieza en lunes`() {
        assertEquals(LocalDate.of(2026, 9, 7), Dates.weekStart(tuesday))
        // Un domingo pertenece a la semana que empezo el lunes anterior.
        assertEquals(LocalDate.of(2026, 9, 7), Dates.weekStart(LocalDate.of(2026, 9, 13)))
    }

    @Test
    fun `fechas relativas legibles`() {
        assertEquals("Hoy", Dates.relative(tuesday, tuesday))
        assertEquals("Mañana", Dates.relative(tuesday.plusDays(1), tuesday))
        assertEquals("Ayer", Dates.relative(tuesday.minusDays(1), tuesday))
    }

    @Test
    fun `saludo segun la hora`() {
        assertEquals("Buenos días", Dates.greeting(LocalTime.of(9, 0)))
        assertEquals("Buenas tardes", Dates.greeting(LocalTime.of(17, 0)))
        assertEquals("Buenas noches", Dates.greeting(LocalTime.of(23, 30)))
    }

    @Test
    fun `formato de hora con dos digitos`() {
        assertEquals("08:05", Dates.time(LocalTime.of(8, 5)))
    }

    @Test
    fun `millisOf respeta fecha y hora`() {
        val millis = Dates.millisOf(tuesday, LocalTime.of(7, 30))
        val back = java.time.Instant.ofEpochMilli(millis)
            .atZone(java.time.ZoneId.systemDefault())
        assertEquals(tuesday, back.toLocalDate())
        assertEquals(LocalTime.of(7, 30), back.toLocalTime())
    }
}
