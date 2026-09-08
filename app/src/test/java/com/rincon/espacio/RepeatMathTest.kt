package com.rincon.espacio

import com.rincon.espacio.domain.model.RepeatRule
import com.rincon.espacio.notifications.RepeatMath
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class RepeatMathTest {

    private val zone: ZoneId = ZoneId.of("Europe/Madrid")

    private fun millis(text: String): Long =
        LocalDateTime.parse(text).atZone(zone).toInstant().toEpochMilli()

    @Test
    fun `un aviso unico no se repite`() {
        val next = RepeatMath.next(millis("2026-09-08T09:00"), RepeatRule.Once, millis("2026-09-08T10:00"), zone)
        assertNull(next)
    }

    @Test
    fun `diario salta al dia siguiente conservando la hora`() {
        val next = RepeatMath.next(
            triggerAtMillis = millis("2026-09-08T09:00"),
            repeat = RepeatRule.Daily,
            afterMillis = millis("2026-09-08T09:00"),
            zone = zone,
        )
        assertEquals(millis("2026-09-09T09:00"), next)
    }

    @Test
    fun `diario se pone al dia tras varios dias apagado`() {
        val next = RepeatMath.next(
            triggerAtMillis = millis("2026-09-01T07:30"),
            repeat = RepeatRule.Daily,
            afterMillis = millis("2026-09-08T12:00"),
            zone = zone,
        )
        assertEquals(millis("2026-09-09T07:30"), next)
    }

    @Test
    fun `de lunes a viernes salta el fin de semana`() {
        // 2026-09-11 es viernes.
        val next = RepeatMath.next(
            triggerAtMillis = millis("2026-09-11T08:00"),
            repeat = RepeatRule.Weekdays,
            afterMillis = millis("2026-09-11T08:00"),
            zone = zone,
        )
        assertEquals(millis("2026-09-14T08:00"), next)
    }

    @Test
    fun `mensual respeta meses de distinta longitud`() {
        val next = RepeatMath.next(
            triggerAtMillis = millis("2026-01-31T20:00"),
            repeat = RepeatRule.Monthly,
            afterMillis = millis("2026-01-31T20:00"),
            zone = zone,
        )
        assertEquals(millis("2026-02-28T20:00"), next)
    }

    @Test
    fun `semanal siempre cae en el futuro`() {
        val after = millis("2026-09-08T12:00")
        val next = RepeatMath.next(millis("2026-06-01T18:00"), RepeatRule.Weekly, after, zone)!!
        assertTrue(next > after)
    }
}
