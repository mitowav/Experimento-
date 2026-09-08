package com.rincon.espacio

import com.rincon.espacio.domain.model.Habit
import com.rincon.espacio.domain.model.HabitCadence
import com.rincon.espacio.domain.model.HabitWithChecks
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class HabitStreakTest {

    private val today: LocalDate = LocalDate.of(2026, 9, 8) // martes

    private fun daily(vararg dates: LocalDate) = HabitWithChecks(
        habit = Habit(id = 1, title = "Leer", cadence = HabitCadence.Daily),
        checkedDates = dates.toSet(),
    )

    @Test
    fun `sin marcas la racha es cero`() {
        assertEquals(0, daily().streak(today))
    }

    @Test
    fun `cuenta dias consecutivos incluyendo hoy`() {
        val item = daily(today, today.minusDays(1), today.minusDays(2))
        assertEquals(3, item.streak(today))
    }

    @Test
    fun `si hoy aun no esta marcado la racha de ayer se mantiene`() {
        val item = daily(today.minusDays(1), today.minusDays(2))
        assertEquals(2, item.streak(today))
    }

    @Test
    fun `un hueco corta la racha`() {
        val item = daily(today, today.minusDays(1), today.minusDays(3))
        assertEquals(2, item.streak(today))
    }

    @Test
    fun `en habitos de lunes a viernes el fin de semana no rompe la racha`() {
        // 2026-09-08 martes; el fin de semana previo es 5 y 6 de septiembre.
        val item = HabitWithChecks(
            habit = Habit(id = 1, title = "Estudiar", cadence = HabitCadence.Weekdays),
            checkedDates = setOf(
                LocalDate.of(2026, 9, 8),
                LocalDate.of(2026, 9, 7),
                LocalDate.of(2026, 9, 4),
                LocalDate.of(2026, 9, 3),
            ),
        )
        assertEquals(4, item.streak(today))
    }

    @Test
    fun `el objetivo semanal usa las veces configuradas`() {
        val item = HabitWithChecks(
            habit = Habit(id = 1, title = "Correr", cadence = HabitCadence.TimesPerWeek, timesPerWeek = 3),
            checkedDates = setOf(LocalDate.of(2026, 9, 7), LocalDate.of(2026, 9, 8)),
        )
        val (done, goal) = item.weekProgress(LocalDate.of(2026, 9, 7))
        assertEquals(2, done)
        assertEquals(3, goal)
    }
}
