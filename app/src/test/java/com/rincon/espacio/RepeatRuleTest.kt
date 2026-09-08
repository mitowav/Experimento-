package com.rincon.espacio

import com.rincon.espacio.domain.model.RepeatRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

/**
 * Cuándo vuelve una tarea que se repite.
 *
 * La regla que importa: se cuenta desde hoy, no desde la fecha original. Una
 * tarea diaria olvidada durante una semana debe volver mañana, no arrastrar
 * seis días de retraso encima.
 */
class RepeatRuleTest {

    private val today = LocalDate.of(2026, 9, 8) // martes

    @Test
    fun `sin repeticion no hay proxima fecha`() {
        assertNull(RepeatRule.Once.nextDate(today, today))
    }

    @Test
    fun `diaria salta al dia siguiente`() {
        assertEquals(LocalDate.of(2026, 9, 9), RepeatRule.Daily.nextDate(today, today))
    }

    @Test
    fun `una tarea diaria atrasada vuelve manana y no arrastra el retraso`() {
        val olvidadaHaceUnaSemana = LocalDate.of(2026, 9, 1)
        assertEquals(
            LocalDate.of(2026, 9, 9),
            RepeatRule.Daily.nextDate(olvidadaHaceUnaSemana, today),
        )
    }

    @Test
    fun `de lunes a viernes se salta el fin de semana`() {
        val viernes = LocalDate.of(2026, 9, 11)
        assertEquals(
            LocalDate.of(2026, 9, 14),
            RepeatRule.Weekdays.nextDate(viernes, viernes),
        )
    }

    @Test
    fun `semanal cae el mismo dia de la semana siguiente`() {
        val next = RepeatRule.Weekly.nextDate(today, today)!!
        assertEquals(LocalDate.of(2026, 9, 15), next)
        assertEquals(today.dayOfWeek, next.dayOfWeek)
    }

    @Test
    fun `mensual respeta meses de distinta longitud`() {
        val treintaYUno = LocalDate.of(2026, 1, 31)
        assertEquals(
            LocalDate.of(2026, 2, 28),
            RepeatRule.Monthly.nextDate(treintaYUno, treintaYUno),
        )
    }

    @Test
    fun `la proxima fecha siempre esta en el futuro`() {
        RepeatRule.entries.filter { it != RepeatRule.Once }.forEach { rule ->
            val next = rule.nextDate(LocalDate.of(2020, 3, 4), today)!!
            org.junit.Assert.assertTrue(
                "$rule deberia devolver una fecha futura",
                next.isAfter(today),
            )
        }
    }
}
